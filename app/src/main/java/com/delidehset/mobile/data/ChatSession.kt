package com.delidehset.mobile.data

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.webkit.WebView
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.io.File

/**
 * The room, kept alive for as long as the app is in front.
 *
 * The screen used to own the [ChatBridge]: opening Sohbet built a WebView, loaded the site's
 * client, asked for a socket token and joined the room, and leaving the screen threw all of
 * that away. Coming back paid the whole price again, and the room arrived empty while it
 * caught up — which is exactly what a chat is not allowed to feel like.
 *
 * So the transport now belongs to the app rather than to the screen. The screen shows what
 * this holds and goes away without disturbing it; the socket stays up while the app is in
 * front, and [onBackground] is what takes it down when the app goes away. The last room is
 * also kept on disk (see [ChatCache]), so even a cold start has the conversation on screen
 * before the socket has said a word.
 */
class ChatSession(
    context: Context,
    private val http: DeliHttp,
    private val cookies: DeliCookieJar,
) {
    private val appContext = context.applicationContext

    private val _state = MutableStateFlow(ChatCache.read(appContext))
    val state: StateFlow<ChatState> = _state.asStateFlow()

    private val _status = MutableStateFlow(if (_state.value.messages.isEmpty()) "Bağlanıyor…" else "")
    val status: StateFlow<String> = _status.asStateFlow()

    private var viewerName: String? = null
    private var bridge: ChatBridge? = null
    private var transport: WebView? = null

    /** Closes the socket after a long time away; cancelled the moment the screen is back. */
    private val idle = Handler(Looper.getMainLooper())
    private val closeWhenIdle = Runnable { if (!onScreen) close() }

    /** True between [onShown] and [onHidden]: someone is looking at the room. */
    var onScreen: Boolean = false
        private set

    /**
     * The transport, built once.
     *
     * Called from the chat screen, on the main thread. A second call — signing in, opening
     * the screen again — returns the same WebView instead of a second socket.
     */
    fun start(viewerName: String?): WebView {
        transport?.let { existing ->
            if (this.viewerName == viewerName) return existing
        }
        // A different account, or a transport that was closed: the old socket belongs to
        // the old session, and a destroyed WebView cannot be shown again.
        close()

        val created = ChatBridge(
            // The application context on purpose: this outlives every activity, and an
            // activity-held WebView would leak the window it was made in.
            context = appContext,
            http = http,
            cookies = cookies,
            viewerName = viewerName,
            onState = { fresh ->
                _state.value = fresh
                ChatCache.write(appContext, fresh)
            },
            onStatus = { _status.value = it },
        )
        val view = created.createWebView()
        bridge = created
        transport = view
        this.viewerName = viewerName
        return view
    }

    /**
     * Hands the room's WebView to a host view, without ever reusing a parent.
     *
     * The screen used to build the WebView once and keep it in `remember`, which broke in
     * two ways that both looked like "the room is dead": a view cannot be added to a
     * second parent, and a WebView that the session had already closed was shown again
     * as a blank rectangle. The host is the screen's own frame; whatever the session has
     * is moved into it, rebuilt if necessary, and shown.
     */
    fun attach(host: android.view.ViewGroup, viewerName: String?): WebView {
        val web = start(viewerName)
        if (web.parent !== host) {
            (web.parent as? android.view.ViewGroup)?.removeView(web)
            host.removeAllViews()
            host.addView(
                web,
                android.view.ViewGroup.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                ),
            )
            web.onResume()
            web.resumeTimers()
        }
        return web
    }

    /** Says who is here, twice: the frame is usually still loading the first time. */
    fun hello(name: String?) {
        val web = transport ?: return
        val room = bridge ?: return
        room.hello(web, name)
    }

    fun send(text: String) {
        bridge?.send(transport, text)
    }

    fun voice(join: Boolean? = null, mic: Boolean? = null, deafen: Boolean? = null) {
        bridge?.voice(transport, join = join, mic = mic, deafen = deafen)
    }

    /** The screen is on: timers run, and the socket is allowed to catch up. */
    fun onShown() {
        onScreen = true
        idle.removeCallbacks(closeWhenIdle)
        transport?.onResume()
        transport?.resumeTimers()
    }

    /**
     * The screen is off, the app is not.
     *
     * The socket deliberately stays: this is what makes coming back instant. The call is
     * *not* kept — a hot microphone behind a closed screen is the one thing worse than a
     * slow room, and the screen leaves it before calling this.
     */
    fun onHidden() {
        onScreen = false
        scheduleIdleClose()
    }

    /**
     * The app left the front. The socket is kept for a while — returning to a room you were
     * just in should not pay for the page and the token again — but not for ever: a room
     * nobody has looked at in ten minutes is one the room does not need this phone in.
     */
    fun onBackground() {
        if (!onScreen) scheduleIdleClose()
    }

    private fun scheduleIdleClose() {
        idle.removeCallbacks(closeWhenIdle)
        idle.postDelayed(closeWhenIdle, IDLE_CLOSE_MS)
    }

    /** Drops the transport; the cached conversation stays for the next open. */
    fun close() {
        idle.removeCallbacks(closeWhenIdle)
        val web = transport
        val room = bridge
        transport = null
        bridge = null
        viewerName = null
        if (web != null && room != null) {
            // Leaving the call first, so the room tells the others instead of leaving a
            // ghost in the participant list.
            runCatching { room.voice(web, join = false) }
            room.release(web)
        }
    }

    private companion object {
        /** How long a room stays connected with nobody looking at it. */
        const val IDLE_CLOSE_MS = 10 * 60 * 1000L
    }
}

/**
 * The room as it was last seen, on disk.
 *
 * Only what stays true across opens is kept: the messages and who wrote them. Online lists,
 * typing indicators and the call are moments, not history — replaying a participant count
 * from an hour ago, or a call nobody is in, would be worse than showing nothing.
 */
internal object ChatCache {

    /** Enough to fill a phone screen several times over, small enough to read at launch. */
    private const val KEEP_MESSAGES = 80

    private fun file(context: Context) = File(context.filesDir, "chat_last_room.json")

    fun read(context: Context): ChatState {
        val raw = runCatching { file(context).readText() }.getOrNull() ?: return ChatState.Empty
        return decode(raw)
    }

    fun write(context: Context, state: ChatState) {
        if (state.messages.isEmpty()) return
        runCatching { file(context).writeText(encode(state)) }
    }

    fun encode(state: ChatState): String = buildJsonObject {
        put("messages", buildJsonArray {
            for (m in state.messages.takeLast(KEEP_MESSAGES)) {
                add(buildJsonObject {
                    put("id", m.id)
                    put("author", m.author)
                    put("avatar", m.avatarUrl ?: "")
                    put("time", m.timeText)
                    put("body", m.bodyHtml)
                    put("color", m.authorColor ?: "")
                    put("reactions", buildJsonArray { m.reactions.forEach { add(it) } })
                })
            }
        })
    }.toString()

    fun decode(raw: String): ChatState {
        val root = runCatching { Json.parseToJsonElement(raw).jsonObject }.getOrNull() ?: return ChatState.Empty
        val messages = root["messages"]?.jsonArray.orEmpty().mapNotNull { element ->
            val o = runCatching { element.jsonObject }.getOrNull() ?: return@mapNotNull null
            val body = o.str("body") ?: return@mapNotNull null
            ChatMessage(
                id = o.str("id").orEmpty(),
                author = o.str("author").orEmpty(),
                avatarUrl = o.str("avatar")?.takeIf { it.isNotBlank() },
                timeText = o.str("time").orEmpty(),
                bodyHtml = body,
                reactions = o["reactions"]?.let { r ->
                    runCatching { r.jsonArray.mapNotNull { it.jsonPrimitive.content } }.getOrNull()
                }.orEmpty(),
                authorColor = o.str("color")?.takeIf { it.isNotBlank() },
            )
        }
        return if (messages.isEmpty()) ChatState.Empty else ChatState(messages = messages)
    }

    private fun kotlinx.serialization.json.JsonObject.str(key: String): String? =
        (this[key] as? kotlinx.serialization.json.JsonPrimitive)?.content
}
