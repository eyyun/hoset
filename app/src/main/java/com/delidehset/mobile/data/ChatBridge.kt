package com.delidehset.mobile.data

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.runBlocking
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayInputStream

/** One chat line, as the site renders it. */
data class ChatMessage(
    val id: String,
    val author: String,
    val avatarUrl: String?,
    val timeText: String,
    val bodyHtml: String,
    val reactions: List<String> = emptyList(),
    /** The colour the room paints this author in, when it paints one at all. */
    val authorColor: String? = null,
)

/** One person in the room's voice channel, as the room's own client shows them. */
data class VoiceUser(
    val name: String,
    val speaking: Boolean = false,
)

/**
 * The voice channel as the room reports it. The call itself (capture, codec, playback)
 * runs in the room page inside the hidden WebView; this is only what the native panel
 * needs to draw, plus what it needs to know to keep its buttons honest.
 */
data class VoiceState(
    val joined: Boolean = false,
    val count: Int = 0,
    val users: List<VoiceUser> = emptyList(),
    val muted: Boolean = false,
    val deafened: Boolean = false,
) {
    companion object {
        val Empty = VoiceState()
    }
}

/** Everything the native chat screen shows at one moment. */
data class ChatState(
    val messages: List<ChatMessage> = emptyList(),
    val online: List<String> = emptyList(),
    val typing: String = "",
    val voice: VoiceState = VoiceState.Empty,
) {
    companion object {
        val Empty = ChatState()
    }
}

/**
 * Transport for the native chat screen.
 *
 * The room is a binary websocket protocol whose token lives behind
 * `/api/realtime/get_token`; the code that speaks it is the site's own client on `/chat/1`,
 * which renders the room inside a **sandboxed** iframe (`sandbox="allow-scripts …"`, no
 * `allow-same-origin`). That sandbox is why the room's markup cannot be read from the
 * parent page — a WebView sees it as an opaque origin.
 *
 * So this bridge does two things instead:
 *
 *  1. it hosts `/chat/1` in a WebView that is laid out one pixel big and never shown, so
 *     the site's own client opens the socket, receives messages and sends ours; and
 *  2. it rewrites the *frame's* HTML on the way in (see [shouldInterceptRequest]) to add a
 *     small script that lives inside that sandbox, where the room's DOM is its own — that
 *     script reports the room to Kotlin and, on demand, types into the room's input.
 *
 * The user only ever sees Compose: list, avatars, reactions, composer. The WebView carries
 * bytes and nothing else.
 */
class ChatBridge(
    private val context: Context,
    private val http: DeliHttp,
    private val cookies: DeliCookieJar,
    /**
     * Who the app is signed in as. The room's call reports its participants by name, so
     * this is what lets the panel answer "am I in this call" instead of guessing from the
     * markup.
     */
    private val viewerName: String? = null,
    private val onState: (ChatState) -> Unit,
    private val onStatus: (String) -> Unit,
) {

    private companion object {
        /** The full room page: it nests the frame and owns the websocket client. */
        const val ROOM_PATH = "/chat/1"

        /** The sandboxed room itself; this is the response we rewrite. */
        const val FRAME_PATH = "/chat/frame"

        const val FRAME_ID = "chat-frame"
    }

    @SuppressLint("SetJavaScriptEnabled")
    fun createWebView(): WebView = WebView(context).apply {
        setBackgroundColor(android.graphics.Color.TRANSPARENT)
        // This view is plumbing: it carries the socket and the call, and it is never
        // meant to be seen. A WebView paints its document white before the page has
        // styled anything, which showed up as a white flash when the room was opened —
        // drawing it at zero alpha removes that entirely. It stays attached and laid
        // out, so scripts, timers and audio all keep running.
        alpha = 0f
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.databaseEnabled = true
        // The room is the same page and the same 1 MB script every time it is opened; asking
        // the network for them again on each visit is what made the screen arrive slowly and
        // stutter while it caught up. The cache is preferred and the network is the fallback,
        // so the first visit of a session still gets a fresh page.
        settings.cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
        // The voice call plays remote audio without any tap on this page, so the room's
        // own playback must not be held behind the autoplay gesture requirement.
        settings.mediaPlaybackRequiresUserGesture = false
        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

        // Same session as the native client, so the room opens already signed in.
        DeliHttp.BASE_URL.toHttpUrlOrNull()?.let { url ->
            val header = cookies.exportRawHeader(url)
            if (header.isNotBlank()) {
                header.split("; ").forEach { pair ->
                    CookieManager.getInstance().setCookie(DeliHttp.BASE_URL, "$pair; path=/")
                }
                CookieManager.getInstance().flush()
            }
        }

        addJavascriptInterface(PageBridge(), "DeliChat")

        // The room asks to record as soon as you join the call. The app already holds the
        // platform permission (the native panel asks for it before joining), so the
        // request from this page is passed straight through.
        webChromeClient = object : WebChromeClient() {
            override fun onPermissionRequest(request: PermissionRequest) {
                val wanted = request.resources.filter { it == PermissionRequest.RESOURCE_AUDIO_CAPTURE }
                if (wanted.isNotEmpty() && hasRecordPermission()) {
                    request.grant(wanted.toTypedArray())
                } else {
                    request.deny()
                }
            }
        }

        webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(
                view: WebView,
                request: WebResourceRequest,
            ): WebResourceResponse? {
                val url = request.url
                if (!request.isForMainFrame && url.encodedPath == FRAME_PATH) {
                    return runCatching { frameWithWatcher(url.encodedQuery) }.getOrNull()
                }
                return null
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                onStatus("Bağlanıyor…")
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                onStatus("Bağlı")
            }
        }

        loadUrl(DeliHttp.BASE_URL + ROOM_PATH)
    }

    /**
     * Fetches the room frame and hands it back with the watcher appended, so the script
     * runs inside the sandbox with the room's own document in scope.
     */
    private fun frameWithWatcher(query: String?): WebResourceResponse {
        val path = if (query.isNullOrBlank()) FRAME_PATH else "$FRAME_PATH?$query"
        val body = runBlocking { http.get(path).body }
        val patched = body.replace("</body>", "$WATCHER</body>")
            .let { if (it == body) body + WATCHER else it }
        return WebResourceResponse(
            "text/html",
            "utf-8",
            200,
            "OK",
            mapOf("Cache-Control" to "no-store"),
            ByteArrayInputStream(patched.toByteArray(Charsets.UTF_8)),
        )
    }

    /**
     * Native → room. The frame is a sandboxed opaque origin, so it cannot be reached
     * directly; a `postMessage` from the parent is the one channel that does work.
     */
    fun send(webView: WebView?, text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        postToFrame(webView, "{ __deli: 'send', text: ${JSONObject.quote(trimmed)} }")
    }

    /**
     * Native → room, voice channel. [join]/[mic]/[deafen] carry the state the panel wants;
     * null means "leave that switch alone". The frame performs the actual toggles, because
     * the call it owns is the one whose audio the room page is streaming.
     */
    fun voice(
        webView: WebView?,
        join: Boolean? = null,
        mic: Boolean? = null,
        deafen: Boolean? = null,
    ) {
        val payload = buildString {
            append("{ __deli: 'voice'")
            join?.let { append(", join: $it") }
            mic?.let { append(", mic: $it") }
            deafen?.let { append(", deafen: $it") }
            viewerName?.takeIf { it.isNotBlank() }?.let { append(", me: ${JSONObject.quote(it)}") }
            append(" }")
        }
        android.util.Log.d("DeliChat", "voice command -> $payload")
        postToFrame(webView, payload)
    }

    /** Introduces this account to the room, which is what names it in the call list. */
    fun hello(webView: WebView?, name: String?) {
        val mine = name?.trim().orEmpty()
        if (mine.isEmpty()) return
        postToFrame(webView, "{ __deli: 'hello', me: ${JSONObject.quote(mine)} }")
    }

    /**
     * One look at the call's own markup, reported back through the log. Used to keep the
     * snapshot below in step with what the room actually renders.
     */
    fun inspectVoice(webView: WebView?) {
        postToFrame(webView, "{ __deli: 'voice-inspect' }")
    }

    private fun postToFrame(webView: WebView?, payload: String) {
        val js = """
            (function () {
              var frame = document.getElementById('$FRAME_ID');
              if (!frame || !frame.contentWindow) return;
              frame.contentWindow.postMessage($payload, '*');
            })();
        """.trimIndent()
        webView?.evaluateJavascript(js, null)
    }

    private fun hasRecordPermission(): Boolean =
        context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED

    fun release(webView: WebView?) {
        webView?.let {
            it.stopLoading()
            (it.parent as? android.view.ViewGroup)?.removeView(it)
            it.destroy()
        }
    }

    /** The room calls these two methods: one snapshot, one diagnostic line. */
    private inner class PageBridge {
        @JavascriptInterface
        fun post(json: String) {
            val parsed = runCatching { parse(json) }.getOrNull() ?: return
            onState(parsed)
        }

        @JavascriptInterface
        fun log(line: String) {
            android.util.Log.d("DeliChat", line)
        }
    }

    private fun parse(json: String): ChatState {
        val root = JSONObject(json)
        val messages = mutableListOf<ChatMessage>()
        val list: JSONArray = root.optJSONArray("messages") ?: JSONArray()
        for (i in 0 until list.length()) {
            val m = list.optJSONObject(i) ?: continue
            val id = m.optString("id")
            if (id.isBlank()) continue
            val reactions = m.optJSONArray("reactions")?.let { arr ->
                (0 until arr.length()).mapNotNull { arr.optString(it).takeIf { s -> s.isNotBlank() } }
            }.orEmpty()
            messages += ChatMessage(
                id = id,
                author = m.optString("author"),
                avatarUrl = m.optString("avatar").takeIf { it.isNotBlank() },
                timeText = m.optString("time"),
                bodyHtml = m.optString("html"),
                reactions = reactions,
                authorColor = m.optString("color").takeIf { it.isNotBlank() },
            )
        }
        val online = root.optJSONArray("online")?.let { arr ->
            (0 until arr.length()).mapNotNull { arr.optString(it).takeIf { s -> s.isNotBlank() } }
        }.orEmpty()
        val voiceJson = root.optJSONObject("voice")
        val voiceUsers = voiceJson?.optJSONArray("users")?.let { arr ->
            (0 until arr.length()).mapNotNull { i ->
                val u = arr.optJSONObject(i) ?: return@mapNotNull null
                val name = u.optString("name")
                if (name.isBlank()) null else VoiceUser(name, u.optBoolean("speaking"))
            }
        }.orEmpty()
        val voice = VoiceState(
            joined = voiceJson?.optBoolean("joined") ?: false,
            count = voiceJson?.optInt("count") ?: 0,
            users = voiceUsers,
            muted = voiceJson?.optBoolean("muted") ?: false,
            deafened = voiceJson?.optBoolean("deafened") ?: false,
        )

        return ChatState(
            messages = messages,
            online = online,
            typing = root.optString("typing"),
            voice = voice,
        )
    }

    /** Appended to the frame's own HTML, so it runs inside that sandbox. */
    private val WATCHER: String
        get() = """
        <script>
        (function () {
          if (window.__deliRoom) return;
          window.__deliRoom = true;

          function clean(s) { return (s || '').replace(/\s+/g, ' ').trim(); }

          // Who is who, as the room itself reports it. The `users` message carries every
          // account's picture and colour, and it is the only reliable source for either:
          // the message markup is rendered before the pictures are known, so a line can
          // sit there with no image at all.
          var uAvatar = {}, uColor = {};

          // The call, as the room reports it over its own socket. `voice-count` carries the
          // participant list and `voice-mute-state` the microphone; together they are the
          // only source that is right the moment something changes. The markup behind the
          // room's call menu lags behind the join, and it does not say at all that the room
          // enters a call muted.
          var vNet = {
            countKnown: false, joined: false, count: 0, users: [],
            muteKnown: false, muted: false,
            deafenKnown: false, deafened: false
          };
          var vMe = '';

          function sameName(a, b) {
            return String(a || '').replace(/^@/, '').toLowerCase() ===
                   String(b || '').replace(/^@/, '').toLowerCase();
          }

          function rememberVoiceState(data) {
            if (!data || typeof data.type !== 'string') return;
            if (data.type === 'voice-count') {
              var list = [];
              var parts = data.participants || [];
              for (var i = 0; i < parts.length; i++) {
                var p = parts[i];
                if (!p || !p.username) continue;
                list.push({ name: String(p.username), speaking: false });
              }
              vNet.users = list;
              vNet.count = typeof data.count === 'number' ? data.count : list.length;
              vNet.countKnown = true;
              vNet.joined = vMe !== '' && list.some(function (u) { return sameName(u.name, vMe); });
            }
            if (data.type === 'voice-mute-state') {
              vNet.muted = !!data.muted;
              vNet.muteKnown = true;
            }
            if (data.type === 'voice-deafen-state') {
              vNet.deafened = !!data.deafened;
              vNet.deafenKnown = true;
            }
          }

          function rememberUsers(data) {
            if (!data || data.type !== 'users' || !data.users) return;
            for (var i = 0; i < data.users.length; i++) {
              var u = data.users[i];
              if (!u || !u.username) continue;
              var key = String(u.username).toLowerCase();
              if (u.profilePic) uAvatar[key] = u.profilePic;
              if (typeof u.color === 'number' && u.color > 0) {
                var hex = (u.color & 0xffffff).toString(16);
                while (hex.length < 6) hex = '0' + hex;
                uColor[key] = '#' + hex;
              }
            }
          }

          function snapshot() {
            var room = document.getElementById('chat-window');
            if (!room) return null;

            var messages = [];
            var groups = room.querySelectorAll('.chat-group');
            for (var g = 0; g < groups.length; g++) {
              var group = groups[g];
              var link = group.querySelector('.userlink');
              var author = link ? clean(link.textContent) : '';
              // A colour the room set for this account, if it set one: the attribute
              // first, then the inline style, then whatever the stylesheet resolved to.
              var authorColor = '';
              if (link) {
                authorColor = link.getAttribute('data-color') || '';
                if (!authorColor && link.style && link.style.color) authorColor = link.style.color;
                if (!authorColor && group.getAttribute('data-color')) authorColor = group.getAttribute('data-color');
              }
              // The picture, however this build of the room happens to mark it up, with
              // the room's own user list as the fallback.
              var pic = group.querySelector('img.avatar-pic') || group.querySelector('img');
              var avatar = pic
                ? (pic.getAttribute('src') || pic.getAttribute('data-src') || '')
                : '';
              var authorKey = (author || '').replace(/^@/, '').toLowerCase();
              if (!avatar) avatar = uAvatar[authorKey] || '';
              if (!authorColor) authorColor = uColor[authorKey] || '';
              var lines = group.querySelectorAll('[data-mid]');
              for (var l = 0; l < lines.length; l++) {
                var line = lines[l];
                var body = line.querySelector('.chat-message');
                var time = line.querySelector('.time');
                var chips = line.querySelectorAll('.react-chip');
                var reactions = [];
                for (var c = 0; c < chips.length; c++) reactions.push(clean(chips[c].textContent));
                messages.push({
                  id: line.getAttribute('data-mid') || '',
                  author: author,
                  avatar: avatar,
                  time: time ? clean(time.textContent) : '',
                  html: body ? body.innerHTML : '',
                  reactions: reactions,
                  color: authorColor
                });
              }
            }

            var online = [];
            var links = document.querySelectorAll('#online a');
            for (var o = 0; o < links.length; o++) online.push(clean(links[o].textContent));
            var typing = document.getElementById('typing-indicator');

            return {
              messages: messages,
              online: online,
              typing: typing ? clean(typing.textContent) : '',
              voice: voiceState()
            };
          }

          // ---- voice channel -------------------------------------------------------
          //
          // The room's own client owns the call: it captures the mic, runs the codec and
          // plays the other side. All this bridge needs is enough of its state to draw the
          // native panel, and a way to press its buttons.
          //
          // Those buttons are pressed by *clicking the room's own controls*, which is what
          // a user would do: function names differ between builds of the site, and a
          // control that was renamed cannot silently turn "mute" into "leave the call".
          // The window functions below are only the fallback for a room that renders no
          // controls at all.
          var VOICE_FN = {
            join:  ['toggleVoice', 'toggleVoiceJoin', 'toggleCall'],
            mic:   ['toggleMicMute', 'toggleMute', 'toggleMicrophone', 'toggleMic'],
            deaf:  ['toggleDeafen', 'toggleDeaf'],
            music: ['toggleMusic']
          };

          function hasFn(name) {
            try { return typeof window[name] === 'function'; } catch (e) { return false; }
          }

          var RE_JOIN = /voice|call|phone|join/i;
          var RE_MIC = /mic|mute|microphone/i;
          var RE_DEAF = /deaf|headphone|volume|speaker|sound|earphone/i;

          /** Every attribute that could say what a control does, in one string. */
          function label(el) {
            var out = (el.id || '') + ' ' + (typeof el.className === 'string' ? el.className : '');
            var attr = ['onclick', 'title', 'aria-label', 'data-action', 'data-tip', 'alt', 'name'];
            for (var i = 0; i < attr.length; i++) {
              var v = el.getAttribute ? el.getAttribute(attr[i]) : null;
              if (v) out += ' ' + v;
            }
            var icons = el.querySelectorAll ? el.querySelectorAll('i,svg') : [];
            for (var j = 0; j < icons.length; j++) {
              var ic = icons[j];
              var icls = typeof ic.className === 'string' ? ic.className : '';
              var di = ic.getAttribute ? (ic.getAttribute('data-icon') || '') : '';
              out += ' ' + icls + ' ' + di;
            }
            return out;
          }

          function visible(el) {
            try { return !!(el.offsetParent || el.getClientRects().length); } catch (e) { return false; }
          }

          /** The room's own control for something, chosen by what it says it does. */
          function findControl(regex, avoid) {
            var nodes = document.querySelectorAll('[onclick],button,a,[data-action]');
            var best = null, bestScore = -1;
            for (var i = 0; i < nodes.length; i++) {
              var el = nodes[i];
              var text = label(el);
              if (!regex.test(text)) continue;
              if (avoid && avoid.test(text)) continue;
              if (!visible(el)) continue;
              var score = 0;
              if (el.getAttribute('onclick')) score += 3;
              if (el.tagName === 'BUTTON' || el.tagName === 'A') score += 2;
              if (el.getAttribute('data-action')) score += 2;
              if (/toggle|voice|call|btn/i.test(text)) score += 1;
              if (score > bestScore) { bestScore = score; best = el; }
            }
            return best;
          }

          /**
           * Works one of the call's switches.
           *
           * The room's own function comes first: the call controls are also behind a menu
           * that has to be opened, so clicking whatever looks like the join button is not
           * the same thing as joining. Clicking is the fallback for a build that renames
           * the functions.
           */
          function press(kind, regex, avoid) {
            var names = VOICE_FN[kind] || [];
            for (var i = 0; i < names.length; i++) {
              if (!hasFn(names[i])) continue;
              try { window[names[i]](); return 'fn:' + names[i]; } catch (e) { /* keep trying */ }
            }
            var el = findControl(regex, avoid);
            if (el) {
              try {
                el.click();
                return 'click:' + (el.id || (typeof el.className === 'string' ? el.className : '') || el.tagName);
              } catch (e) { /* nothing left to try */ }
            }
            return '';
          }

          //
          // Reading the room's call state has to be forgiving: only the three switches
          // below are ever conclusive, and they are read straight off the markup the call
          // paints (an active class, the microphone icon, the deafen icon). When none of
          // them says anything, the last state we set through our own buttons stands — so
          // the panel and the page can never disagree about whether we are in the call,
          // which is what would otherwise turn a second "join" into a quiet "leave".
          var vLocal = { joined: false, muted: false, deafened: false };

          function cls(el) {
            return el && typeof el.className === 'string' ? el.className : '';
          }

          function onClass(el) {
            if (!el) return null;
            // Bootstrap's `text-muted` is a *colour*, not a state: reading it as "microphone
            // muted" is what kept reporting an active call as "not in a call".
            var c = cls(el).replace(/text-muted/g, '').replace(/muted-text/g, '');
            if (/\b(active|on|joined|open)\b/.test(c)) return true;
            if (/\b(off|closed)\b/.test(c)) return false;
            return null;
          }

          function hasIcon(selector) {
            try { return !!document.querySelector(selector); } catch (e) { return false; }
          }

          // In the call? The room marks its own toggle, and the participant list only
          // exists while the call does.
          function joinSignal() {
            var toggle = document.querySelector('#voice-join, .voice-join, [onclick*="toggleVoice"]');
            var flag = onClass(toggle);
            if (flag !== null) return flag;
            var list = document.querySelector('.call-users, .call-participants, #call-users, #voice-users');
            if (list) return true;
            if (document.querySelector('.call-user')) return true;
            return null;
          }

          function muteSignal() {
            if (hasIcon('i.fa-microphone-slash, .fa-microphone-slash')) return true;
            if (hasIcon('i.fa-microphone, .fa-microphone')) return false;
            var btn = document.querySelector('#voice-mute, .voice-mute');
            return onClass(btn);
          }

          function deafenSignal() {
            if (hasIcon('i.fa-deaf, i.fa-deafness, .fa-deaf')) return true;
            if (hasIcon('i.fa-headphones, i.fa-volume-high, .fa-headphones')) return false;
            var btn = document.querySelector('#voice-deafen, .voice-deafen');
            return onClass(btn);
          }

          // ---- echo control ---------------------------------------------------------
          //
          // You hear your own voice in this room for one of two reasons. The room's own
          // client can be told which account is the local one — "Echo guard" in its call
          // menu — and then it does not play your microphone back at you; with that guard
          // off, the first thing anyone hears after unmuting is themselves. And the
          // microphone itself has to be opened with the platform's acoustic echo
          // canceller on, or the loudspeaker's sound walks straight back into it.
          //
          // Both are handled here, and neither one needs the room's cooperation: the
          // guard is armed on the engine the room builds (found by what it can do, since
          // builds rename it), and the microphone request is rewritten on its way out.
          var vEngine = null;
          var vOwnUid = 0;

          /**
           * The room publishes things on `window` with `Object.defineProperty`, so a plain
           * `for (var k in window)` does not see them: the engine was invisible to the
           * first version of this guard and the guard never went on. Own property names
           * include the hidden ones; the name itself does not matter, what it can do does.
           */
          function findEngine() {
            if (vEngine) return vEngine;
            var names = [];
            try { names = Object.getOwnPropertyNames(window); } catch (e) { names = []; }
            for (var i = 0; i < names.length; i++) {
              try {
                var v = window[names[i]];
                if (v && typeof v.setOwnUserId === 'function') {
                  vEngine = v;
                  DeliChat.log('echo-guard: engine found as window.' + names[i]);
                  return v;
                }
              } catch (e) {}
            }
            return null;
          }

          /**
           * Keeps the room's echo guard on. The room disables it by asking for -1, so
           * that is the one request refused here: the id it was given before is handed
           * back instead, and every request is written to the log so the native panel can
           * be checked against what actually happened.
           */
          function guardEcho() {
            var engine = findEngine();
            if (!engine || engine.__deliEcho) return !!engine;
            try {
              var orig = engine.setOwnUserId.bind(engine);
              engine.setOwnUserId = function (id) {
                if (typeof id === 'number' && id > 0) vOwnUid = id;
                var want = (id > 0 || vOwnUid <= 0) ? id : vOwnUid;
                DeliChat.log('echo-guard: setOwnUserId(' + id + ') -> ' + want);
                return orig(want);
              };
              engine.__deliEcho = true;
              DeliChat.log('echo-guard: engine armed');
              return true;
            } catch (x) {
              DeliChat.log('echo-guard: could not arm (' + x + ')');
              return false;
            }
          }

          /**
           * The room asks for `{echoCancellation: true}` already; this makes sure of it,
           * and adds the switches the older Chromium spellings use. Constraints a browser
           * does not know are ignored, so the extra names cost nothing.
           */
          function hardenMic() {
            try {
              var md = navigator.mediaDevices;
              if (!md || typeof md.getUserMedia !== 'function' || md.__deliMic) return false;
              var orig = md.getUserMedia.bind(md);
              md.getUserMedia = function (c) {
                try {
                  var out = c || {};
                  if (out.audio !== false) {
                    var base = (out.audio && typeof out.audio === 'object') ? out.audio : {};
                    var merged = {};
                    for (var k in base) merged[k] = base[k];
                    merged.echoCancellation = true;
                    merged.noiseSuppression = true;
                    merged.googEchoCancellation = true;
                    merged.googEchoCancellation2 = true;
                    merged.googNoiseSuppression = true;
                    merged.googHighpassFilter = true;
                    out.audio = merged;
                    DeliChat.log('mic constraints -> ' + JSON.stringify(merged));
                  }
                } catch (x) {}
                return orig(out);
              };
              md.__deliMic = true;
              DeliChat.log('mic hardened');
              return true;
            } catch (e) {
              return false;
            }
          }

          // ---- the call's own wire protocol ----------------------------------------
          //
          // The room's voice does not run over WebRTC: it is a small custom protocol on its
          // socket (a two byte type, a length, then the payload), and an audio frame carries
          // the account id it came from in the first four bytes of that payload. Everything
          // audible is therefore identifiable — which is what makes it possible to answer
          // "why do I hear myself": the server sends the room's audio to everyone in it,
          // the sender included, and the client is the only thing that can leave its own
          // frames out again. That is what the room's own "Echo guard" is, and this is the
          // same filter one layer lower, so it holds even when that switch is off.
          var vFrames = { own: 0, seenOwn: 0, dropped: 0 };
          var F_VAUDIO = 0x22;
          var F_MAUDIO = 0x24;

          function audioUid(data) {
            try {
              if (!(data instanceof ArrayBuffer) || data.byteLength < 10) return 0;
              var view = new DataView(data);
              var type = view.getUint16(0, false);
              if (type !== F_VAUDIO && type !== F_MAUDIO) return 0;
              return view.getUint32(6, false);
            } catch (e) { return 0; }
          }

          /** The id this account's own microphone is stamped with, read from its output. */
          function learnOwnUid(data) {
            var uid = audioUid(data);
            if (uid <= 0) return;
            if (uid === vFrames.own) vFrames.seenOwn++;
            else { vFrames.own = uid; vFrames.seenOwn = 1; DeliChat.log('voice: own audio id ' + uid); }
            // A frame this page sent itself cannot belong to anybody else, so one send is
            // enough: the echo filter used to wait for a second frame, and the first
            // thing anyone heard after unmuting was themselves.
            if (vFrames.seenOwn === 1 && vEngine) {
              try { vEngine.setOwnUserId(uid); } catch (e) {}
            }
          }

          /** True for the frames of this account's own microphone on their way back. */
          function ownAudioFrame(data) {
            if (vFrames.own <= 0 || vFrames.seenOwn < 1) return false;
            if (audioUid(data) !== vFrames.own) return false;
            vFrames.dropped++;
            if (vFrames.dropped === 1 || vFrames.dropped % 100 === 0) {
              DeliChat.log('voice: dropped ' + vFrames.dropped + ' own audio frame(s)');
            }
            return true;
          }

          /**
           * Installed on the socket prototype, before the room builds its socket. Only
           * `onmessage` is wrapped: replacing listeners added with `addEventListener`
           * would leave the room unable to remove them again.
           */
          function filterSockets() {
            try {
              var proto = window.WebSocket && window.WebSocket.prototype;
              if (!proto || proto.__deliFiltered) return false;
              proto.__deliFiltered = true;
              var send = proto.send;
              proto.send = function (data) {
                try { learnOwnUid(data); } catch (e) {}
                return send.apply(this, arguments);
              };
              var desc = Object.getOwnPropertyDescriptor(proto, 'onmessage');
              if (desc && desc.set) {
                Object.defineProperty(proto, 'onmessage', {
                  configurable: true,
                  enumerable: desc.enumerable,
                  get: desc.get,
                  set: function (fn) {
                    desc.set.call(this, typeof fn === 'function' ? function (ev) {
                      if (ownAudioFrame(ev && ev.data)) return;
                      return fn.call(this, ev);
                    } : fn);
                  }
                });
              }

              // `onmessage` is only one of the two ways a page listens. A client built
              // with addEventListener bypassed the filter completely — which is the other
              // way your own voice came back. Listeners are mapped to their wrapped
              // version so removeEventListener still removes the right one.
              var add = proto.addEventListener;
              var remove = proto.removeEventListener;
              var wrappedFor = new Map();
              if (typeof add === 'function' && typeof remove === 'function') {
                proto.addEventListener = function (type, fn, opts) {
                  if (type === 'message' && typeof fn === 'function') {
                    var wrapped = function (ev) {
                      if (ownAudioFrame(ev && ev.data)) return;
                      return fn.call(this, ev);
                    };
                    wrappedFor.set(fn, wrapped);
                    return add.call(this, type, wrapped, opts);
                  }
                  return add.apply(this, arguments);
                };
                proto.removeEventListener = function (type, fn, opts) {
                  if (type === 'message' && typeof fn === 'function' && wrappedFor.has(fn)) {
                    var wrapped = wrappedFor.get(fn);
                    wrappedFor.delete(fn);
                    return remove.call(this, type, wrapped, opts);
                  }
                  return remove.apply(this, arguments);
                };
              }
              DeliChat.log('voice: sockets filtered');
              return true;
            } catch (e) {
              DeliChat.log('voice: could not filter sockets (' + e + ')');
              return false;
            }
          }

          function voiceUsers() {
            var users = [];
            var nodes = document.querySelectorAll('.call-user');
            for (var i = 0; i < nodes.length; i++) {
              var n = nodes[i];
              var nameEl = n.querySelector('.call-name') || n.querySelector('.username') || n.querySelector('span');
              var nm = clean(nameEl ? nameEl.textContent : n.textContent);
              nm = nm.replace(/^@/, '');
              if (!nm) continue;
              users.push({ name: nm, speaking: /\bspeaking\b/.test(cls(n)) });
            }
            return users;
          }

          function voiceState() {
            var users = vNet.countKnown ? vNet.users : voiceUsers();
            var count = vNet.countKnown ? vNet.count : users.length;

            var joined = vNet.countKnown ? vNet.joined : joinSignal();
            if (joined === null || typeof joined === 'undefined') joined = vLocal.joined;

            var muted = vNet.muteKnown ? vNet.muted : muteSignal();
            if (muted === null) muted = vLocal.muted;
            var deaf = vNet.deafenKnown ? vNet.deafened : deafenSignal();
            if (deaf === null) deaf = vLocal.deafened;

            // Speaking rings come from the markup, which is the one thing it does say.
            var speaking = {};
            var domUsers = voiceUsers();
            for (var s = 0; s < domUsers.length; s++) {
              if (domUsers[s].speaking) speaking[String(domUsers[s].name).toLowerCase()] = true;
            }
            for (var u = 0; u < users.length; u++) {
              if (speaking[String(users[u].name).toLowerCase()]) users[u].speaking = true;
            }

            // Remember what we just concluded: it is what the next command compares to.
            vLocal.joined = joined;
            vLocal.muted = muted;
            vLocal.deafened = deaf;

            return {
              joined: joined,
              count: joined ? Math.max(count, users.length, 1) : count,
              users: users,
              muted: muted,
              deafened: deaf
            };
          }

          // A plain inventory of the call's markup, so the native panel can be kept in
          // step with what this room actually renders instead of guessing.
          function voiceInventory() {
            var out = { ids: [], classes: [], fns: [], keys: [], dom: [] };
            // The call's own nodes, with their class and whether they are actually shown:
            // that is what decides "am I in the call" — several of these exist in the DOM
            // even when nobody is in a call at all.
            var watched = ['voice-chat-btn', 'call-menu', 'call-participants', 'call-actions',
                           'voice-bar', 'call-mic-btn', 'call-deafen-btn', 'voice-count-badge'];
            for (var wd = 0; wd < watched.length; wd++) {
              var node = document.getElementById(watched[wd]);
              if (!node) { out.dom.push(watched[wd] + '=missing'); continue; }
              out.dom.push(
                watched[wd] +
                ' cls="' + (typeof node.className === 'string' ? node.className : '') + '"' +
                ' ' + (visible(node) ? 'shown' : 'hidden') +
                ' text="' + clean(node.textContent).slice(0, 60) + '"'
              );
            }
            var all = document.querySelectorAll('*');
            for (var i = 0; i < all.length && out.ids.length < 60; i++) {
              var id = all[i].id || '';
              if (id && /voice|call|mic|mute|music|deaf|speak/i.test(id)) {
                var cls = typeof all[i].className === 'string' ? all[i].className : '';
                out.ids.push(id + ' [' + cls + '] "' + clean(all[i].textContent).slice(0, 40) + '"');
              }
            }
            for (var j = 0; j < all.length && out.classes.length < 60; j++) {
              var c = typeof all[j].className === 'string' ? all[j].className : '';
              if (c && /call|voice|speak|mic/i.test(c)) out.classes.push(all[j].tagName.toLowerCase() + '.' + c);
            }
            Object.keys(VOICE_FN).forEach(function (k) {
              VOICE_FN[k].forEach(function (name) {
                out.fns.push(name + '=' + (hasFn(name) ? 'yes' : 'no'));
              });
            });
            try {
              for (var s = 0; s < localStorage.length; s++) {
                var key = localStorage.key(s) || '';
                if (/voice|call|mic|mute/i.test(key)) out.keys.push(key + '=' + localStorage.getItem(key));
              }
            } catch (e) {}
            // Where the echo handling stands, so "I still hear myself" can be answered
            // with a log line instead of a guess.
            out.echo = {
              engine: !!vEngine,
              ownUid: vOwnUid,
              micPatched: !!(navigator.mediaDevices && navigator.mediaDevices.__deliMic),
              sockets: !!(window.WebSocket && window.WebSocket.prototype.__deliFiltered),
              audioId: vFrames.own,
              dropped: vFrames.dropped
            };
            return out;
          }

          /**
           * Native -> room. The panel sends the state it wants, not a "toggle": the room
           * is the one that knows where the call actually stands, and asking for a state
           * it already has does nothing.
           */
          function voiceControlReport() {
            return ['call-mic-btn', 'call-deafen-btn'].map(function (id) {
              var el = document.getElementById(id);
              if (!el) return id + ':missing';
              var icons = el.querySelectorAll('i');
              var icls = icons.length ? (typeof icons[0].className === 'string' ? icons[0].className : '') : '';
              return id + ':' + (visible(el) ? 'shown' : 'hidden') + ':' + icls.trim();
            }).join(' | ');
          }

          function voiceCommand(data) {
            if (typeof data.me === 'string' && data.me !== '') vMe = data.me;
            var state = voiceState();
            var did = [];
            if (data.join === true && !state.joined) {
              did.push(press('join', RE_JOIN));
              vLocal.joined = true;
              vLocal.muted = true;   // the room joins muted, and so does the panel
            }
            if (data.join === false && state.joined) {
              did.push(press('join', RE_JOIN));
              vLocal.joined = false;
              vLocal.deafened = false;
            }
            if (typeof data.mic === 'boolean' && data.mic !== state.muted) {
              did.push(press('mic', RE_MIC, RE_DEAF));
              vLocal.muted = !state.muted;
            }
            if (typeof data.deafen === 'boolean' && data.deafen !== state.deafened) {
              did.push(press('deaf', RE_DEAF, RE_MIC));
              vLocal.deafened = !state.deafened;
            }
            DeliChat.log('voice: ' + JSON.stringify(data) + ' -> ' + did.join(',') +
              ' state=' + JSON.stringify(voiceState()) +
              ' can(' + VOICE_FN.mic.join('/') + ')=' + voiceControlReport());
            // One look at the call's markup right after a command, so the panel can be
            // wired to what the room actually renders instead of to a guess.
            setTimeout(function () {
              try { DeliChat.log('voice-after: ' + JSON.stringify(voiceInventory())); } catch (e) {}
            }, 900);
            push();
          }

          var last = '';
          function push() {
            try {
              var snap = snapshot();
              if (!snap) return;
              var encoded = JSON.stringify(snap);
              if (encoded === last) return;
              last = encoded;
              DeliChat.post(encoded);
            } catch (e) { /* mid-render: the next tick catches it */ }
          }

          function typeAndSend(text) {
            var box = document.getElementById('input-text-chat');
            DeliChat.log('send: box=' + (box ? 'yes' : 'no'));
            if (!box) return;
            box.focus();
            box.value = text;
            box.dispatchEvent(new Event('input', { bubbles: true }));
            var enter = {
              key: 'Enter', code: 'Enter', keyCode: 13, which: 13,
              bubbles: true, cancelable: true
            };
            var a = box.dispatchEvent(new KeyboardEvent('keydown', enter));
            var b = box.dispatchEvent(new KeyboardEvent('keypress', enter));
            DeliChat.log('send: value="' + box.value + '" keydownDefault=' + a + ' keypressDefault=' + b);
          }

          // The parent posts our commands in; everything else is the room's own traffic.
          window.addEventListener('message', function (event) {
            var data = event.data;
            try { DeliChat.log('msg: ' + (typeof data) + ' ' + JSON.stringify(data).slice(0, 180)); } catch (x) {}
            if (typeof data === 'string' && data.indexOf('__deli') !== -1) {
              try { data = JSON.parse(data); } catch (e) { return; }
            }
            rememberUsers(data);
            rememberVoiceState(data);
            if (data && data.__deli === 'hello' && typeof data.me === 'string') {
              vMe = data.me;
              // The call reports its participants by name, so knowing which name is ours
              // is what makes "am I in this call" answerable at all.
              push();
            }
            if (data && data.__deli === 'send' && typeof data.text === 'string') {
              typeAndSend(data.text);
            }
            if (data && data.__deli === 'voice') {
              voiceCommand(data);
            }
            if (data && data.__deli === 'voice-inspect') {
              try { DeliChat.log('voice-inventory: ' + JSON.stringify(voiceInventory())); }
              catch (e) { DeliChat.log('voice-inventory failed: ' + e); }
            }
          });

          function boot() {
            if (!document.getElementById('chat-window')) { setTimeout(boot, 300); return; }
            var room = document.getElementById('chat-window');
            new MutationObserver(push).observe(room, { childList: true, subtree: true, characterData: true });
            var typing = document.getElementById('typing-indicator');
            if (typing) new MutationObserver(push).observe(typing, { childList: true, subtree: true, characterData: true });
            // One look at what this build of the room calls its call controls, so the
            // panel can be checked against the real thing instead of guessed at.
            setTimeout(function () {
              try { DeliChat.log('inventory: ' + JSON.stringify(voiceInventory())); }
              catch (e) { DeliChat.log('inventory failed: ' + e); }
            }, 2500);
            push();
          }
          // Both of these go in now, before anyone presses "join": the microphone patch
          // has to be in place before the room opens its own capture, and the engine only
          // exists once the room has built it, so it is looked for until it appears.
          hardenMic();
          filterSockets();
          setInterval(function () { hardenMic(); guardEcho(); }, 1500);

          boot();
          setInterval(push, 1200);
        })();
        </script>
        """
}
