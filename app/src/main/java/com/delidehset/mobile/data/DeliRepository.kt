package com.delidehset.mobile.data

import com.delidehset.mobile.ui.tr
import com.delidehset.mobile.ui.trf
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/** Outcome of a mutating call, with whatever message the site produced. */
data class ActionResult(val ok: Boolean, val message: String?, val raw: DeliResponse? = null)

/** A server-rendered list page: its table, or the note explaining why it is empty. */
data class TablePage(
    val headers: List<String> = emptyList(),
    val rows: List<SimpleRow> = emptyList(),
    val note: String? = null,
)

/** Everything a post page gives us. */
data class PostPage(
    val post: PostDetail,
    val comments: List<CommentNode>,
    val commentVotes: Map<String, Int>,
    val commentForm: DiscoveredForm?,
    val html: String,
)

class DeliRepository(
    private val http: DeliHttp,
    private val cookies: DeliCookieJar,
    private val session: ViewerCache,
    private val cache: BodyCache = BodyCache.None,
) {

    private companion object {
        /** Page bodies rarely change second to second; this makes menus feel instant. */
        const val PAGE_TTL_MS = 5 * 60 * 1000L
        const val FEED_TTL_MS = 60 * 1000L

        /** How many post pages to warm after a feed load. */
        const val PREFETCH_COUNT = 2

        /**
         * How many feed pages a community is counted over before its number is given up on.
         * A page holds a few dozen posts, so this is far more than any community here has.
         */
        const val HOLE_COUNT_PAGES = 8

        /**
         * Sidebar destinations warmed in the background, in the site's own order. Kept
         * short on purpose: the server is slow and every extra request at start-up
         * competes with the screen the user is actually looking at.
         */
        val WARM_PAGES = listOf("/holes", "/users", "/comments")

        /** Gap between warm-up requests, so they never crowd out a real one. */
        const val WARM_GAP_MS = 400L
    }

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    /* ------------------------------------------------------------------ */
    /*  Session                                                           */
    /* ------------------------------------------------------------------ */

    /**
     * Loads the home page once so we pick up the session cookies and the antiforgery
     * token, and learn whether the stored session is still valid.
     */
    suspend fun bootstrap(): Viewer {
        val res = DeliErrors.withRetry { http.get("/") }
        val doc = HtmlParser.document(res.body)
        http.updateAntiforgery(HtmlParser.antiforgeryToken(doc))
        val viewer = HtmlParser.parseViewer(doc)
        session.save(viewer)
        return viewer
    }

    suspend fun currentViewer(): Viewer = bootstrap()

    /**
     * Sign-in. The form posts `username`, `password` and `redirect`; a successful
     * login answers with a redirect, a failed one re-renders the page with an error.
     */
    suspend fun login(username: String, password: String): LoginResult {
        val page = http.get("/login")
        val doc = HtmlParser.document(page.body)
        http.updateAntiforgery(HtmlParser.antiforgeryToken(doc))

        val res = http.postForm(
            "/login",
            listOf(
                "username" to username,
                "password" to password,
                "redirect" to "/",
            ),
            referer = "/login",
        )

        return when {
            res.isRedirect && res.location?.contains("/login") != true -> {
                val viewer = bootstrap()
                if (viewer.isSignedIn) LoginResult.Success
                else LoginResult.Failure("Giriş yapılamadı. Kullanıcı adı veya şifre hatalı olabilir.")
            }

            res.isSuccess -> {
                // Re-rendered form: pull the validation message out of it.
                val failDoc = HtmlParser.document(res.body)
                http.updateAntiforgery(HtmlParser.antiforgeryToken(failDoc))
                val message = failDoc.select(".validation-summary-errors li, .alert-danger, .text-danger")
                    .firstOrNull { it.text().isNotBlank() }?.text()?.trim()
                    ?: failDoc.selectFirst("#login .invalid-feedback")?.text()?.trim()
                LoginResult.Failure(message ?: "Kullanıcı adı veya şifre hatalı.")
            }

            res.code == 403 -> LoginResult.Failure(
                "İstek reddedildi (403). Güvenlik doğrulaması yenilenemedi, tekrar dene."
            )

            else -> LoginResult.Failure(trf("Giriş yapılamadı (HTTP {0}).", res.code))
        }
    }

    suspend fun logout() {
        runCatching { http.get("/logout") }
        cookies.clear()
        session.clear()
    }

    /* ------------------------------------------------------------------ */
    /*  Feed                                                              */
    /* ------------------------------------------------------------------ */

    suspend fun feed(
        sort: FeedSort,
        hole: String? = null,
        cursor: String? = null,
        effortOnly: Boolean = false,
        force: Boolean = false,
    ): FeedResponse {
        val url = HttpUrl.Builder()
            .scheme("https")
            .host("delidehset.com")
            .addPathSegment("api")
            .addPathSegment("feed")
            .addPathSegment("posts")
            .addQueryParameter("sort", sort.key)
            .apply { hole?.takeIf { it.isNotBlank() }?.let { addQueryParameter("hole", it) } }
            .apply { cursor?.takeIf { it.isNotBlank() }?.let { addQueryParameter("cursor", it) } }
            .apply { if (effortOnly) addQueryParameter("effortposts_only", "true") }
            .build()
            .toString()

        // Only the first page is cached; subsequent pages are keyed by cursor anyway.
        val key = "feed:$url"
        if (!force) {
            cache.read(key)?.takeIf { it.isFresh(FEED_TTL_MS) }?.let { return decodeFeed(it.body) }
        }

        return try {
            val body = DeliErrors.withRetry {
                val res = http.get(url, accept = "application/json")
                if (!res.isSuccess) throw DeliException(DeliErrors.explainHttp(res.code, "Akış"))
                res.body
            }
            cache.write(key, body)
            decodeFeed(body)
        } catch (t: Throwable) {
            // Offline / flaky network: show what we had rather than an empty screen.
            cache.read(key)?.let { decodeFeed(it.body) } ?: throw t
        }
    }

    private fun decodeFeed(body: String): FeedResponse =
        json.decodeFromString(FeedResponse.serializer(), body)

    /**
     * Warms the cache for the first few posts of a feed so tapping one opens instantly.
     * Failures are ignored on purpose — this is a convenience, not a requirement.
     */
    suspend fun prefetchPosts(permalinks: List<String>) {
        for (link in permalinks.take(PREFETCH_COUNT)) {
            runCatching { post(link) }
        }
    }

    /**
     * Fetches the sidebar pages once, in the background, right after start-up. They are
     * plain server-rendered menus that the site takes its time with, and pre-warming them
     * is what makes opening one feel instant instead of showing a spinner.
     */
    suspend fun warmPages() {
        for (path in WARM_PAGES) {
            if (!coroutineContext.isActive) return
            runCatching { page(path) }
            delay(WARM_GAP_MS)
        }
    }

    /* ------------------------------------------------------------------ */
    /*  Posts and comments                                                */
    /* ------------------------------------------------------------------ */

    suspend fun post(permalink: String, force: Boolean = false): PostPage {
        val key = "post:$permalink"
        if (!force) {
            cache.read(key)?.takeIf { it.isFresh(PAGE_TTL_MS) }?.let { return parsePostPage(it.body, permalink) }
        }

        val html = try {
            DeliErrors.withRetry {
                val res = http.get(permalink)
                if (res.isRedirect && res.location?.contains("/login") == true) throw NotSignedInException()
                if (!res.isSuccess) throw DeliException(DeliErrors.explainHttp(res.code, "Gönderi"))
                res.body
            }
        } catch (t: Throwable) {
            cache.read(key)?.body ?: throw t
        }

        cache.write(key, html)
        return parsePostPage(html, permalink)
    }

    private fun parsePostPage(html: String, permalink: String): PostPage {
        val doc = HtmlParser.document(html)
        http.updateAntiforgery(HtmlParser.antiforgeryToken(doc))

        val post = HtmlParser.parsePost(html, permalink)
            ?: throw DeliException("Gönderi sayfası beklenen biçimde gelmedi; site güncellenmiş olabilir.")

        return PostPage(
            post = post,
            comments = HtmlParser.parseComments(html),
            commentVotes = HtmlParser.commentVotes(html),
            commentForm = HtmlParser.parseForm(html, "form[data-comment-form]")
                ?: HtmlParser.parseForm(html, "form[action^=/comment]"),
            html = html,
        )
    }

    suspend fun vote(kind: String, id: String, value: Int): ActionResult {
        ensureAntiforgery()
        val res = http.postEmpty("/vote/$kind/$id/$value", referer = "/")
        return interpret(res)
    }

    /**
     * Posts a comment using the form the server rendered for this session, so the
     * request matches whatever field names the current build expects.
     *
     * The shipped build names the thread field `parent_fullname` and encodes the target
     * as `p_<post uuid>` for a top-level comment, `c_<comment uuid>` for a reply — the
     * same values the site's own JavaScript puts in the hidden input it generates. When
     * a build does not expose that field, the older `post_id`/`parent_id` names are used
     * instead, so this keeps working if the platform changes again.
     */
    suspend fun comment(
        form: DiscoveredForm?,
        postId: String,
        body: String,
        parentId: String? = null,
        hole: String? = null,
    ): ActionResult {
        ensureAntiforgery()
        val action = form?.action?.takeIf { it.isNotBlank() } ?: "/comment"
        val res = http.postForm(action, commentFields(form, postId, body, parentId, hole), referer = "/")
        return interpret(res, "Yorum")
    }

    suspend fun deletePost(id: String): ActionResult {
        ensureAntiforgery()
        return interpret(http.postEmpty("/delete/post/$id", referer = "/"))
    }

    suspend fun undeletePost(id: String): ActionResult {
        ensureAntiforgery()
        return interpret(http.postEmpty("/undelete_post/$id", referer = "/"))
    }

    suspend fun reportPost(id: String, reason: String, platform: Boolean = false): ActionResult {
        ensureAntiforgery()
        val path = if (platform) "/report/post/$id" else "/report/hole/post/$id"
        return interpret(http.postForm(path, listOf("reason" to reason), referer = "/"))
    }

    suspend fun reportComment(id: String, reason: String, platform: Boolean = false): ActionResult {
        ensureAntiforgery()
        val path = if (platform) "/report/comment/$id" else "/report/hole/comment/$id"
        return interpret(http.postForm(path, listOf("reason" to reason), referer = "/"))
    }

    /* ------------------------------------------------------------------ */
    /*  Submitting a post                                                 */
    /* ------------------------------------------------------------------ */

    /** The submit form is public, which lets us read its exact fields and hole list. */
    suspend fun submitForm(): DiscoveredForm? {
        val res = http.get("/submit")
        val doc = HtmlParser.document(res.body)
        http.updateAntiforgery(HtmlParser.antiforgeryToken(doc))
        return HtmlParser.parseForm(res.body, "form#submitform")
            ?: HtmlParser.parseForm(res.body, "form[action^=/submit]")
    }

    /* ------------------------------------------------------------------ */
    /*  Account settings                                                  */
    /* ------------------------------------------------------------------ */

    /**
     * The signed-in settings pages, read straight from the server.
     *
     * delidehset.com has two tabs — Personal (`/settings`) and Security
     * (`/settings/security`) — and posts every change as a form-encoded request with the
     * antiforgery token in the `RequestVerificationToken` header, which is exactly what
     * [DeliHttp] does. The field names below are the ones the live pages actually use.
     */
    suspend fun accountSettings(force: Boolean = false): AccountSettings {
        val personal = HtmlParser.parsePersonalSettings(page("/settings", force))
        val security = runCatching {
            HtmlParser.parseSecuritySettings(page("/settings/security", force))
        }.getOrElse { SecuritySettings() }
        return AccountSettings(personal, security)
    }

    /** Bio and pronouns, the two free-text fields of the Personal tab. */
    suspend fun savePersonalSettings(bio: String, pronouns: String): ActionResult =
        settingsPost("/settings/personal", listOf("bio" to bio, "pronouns" to pronouns))

    suspend fun saveLanguage(language: String): ActionResult =
        settingsPost("/settings/language", listOf("language" to language))

    /**
     * Just the account's language, without the security page.
     *
     * The app follows the site's language, so it reads this one value — asking for both
     * settings pages to get it would be a second request for a field nobody else wants.
     */
    suspend fun accountLanguage(): String =
        HtmlParser.parsePersonalSettings(page("/settings")).language

    suspend fun saveTheme(theme: String, themeColor: String?): ActionResult {
        val fields = mutableListOf("theme" to theme)
        themeColor?.takeIf { it.isNotBlank() }?.let { fields += "themecolor" to it }
        return settingsPost("/settings/theme", fields)
    }

    suspend fun changePassword(oldPassword: String, newPassword: String, confirm: String): ActionResult =
        settingsPost(
            "/settings/password",
            listOf(
                "old_password" to oldPassword,
                "new_password" to newPassword,
                "new_password_confirm" to confirm,
            ),
        )

    suspend fun changeEmail(password: String, email: String): ActionResult =
        settingsPost("/settings/email", listOf("password" to password, "email" to email))

    /**
     * `/contact` is a single `message` textarea that the site's own JavaScript posts to
     * the page itself, so the app can offer it natively instead of hosting the page.
     */
    /* ---------------------------------------------------------------- */
    /*  What a profile offers: follow, mute, block                      */
    /* ---------------------------------------------------------------- */

    /**
     * The three verbs a site profile carries, at the routes the site itself posts to.
     *
     * All of them are POST-only — a GET answers 405 — and all of them take the account in
     * the path except blocking, which names it in the form instead. Nothing about the
     * request is guessed from a button label: the paths came from the site's own strings
     * (`/follow/`, `/unfollow/`, `/block_user`, `/mute_notifs/`) and were confirmed to
     * answer 405 on GET and 302-to-login on POST, which is what an authenticated endpoint
     * looks like from outside.
     */
    suspend fun followUser(name: String, follow: Boolean): ActionResult {
        ensureAntiforgery()
        val user = encodeSegment(name)
        val path = if (follow) "/follow/$user" else "/unfollow/$user"
        val res = http.postForm(path, emptyList(), referer = "/@$user")
        cache.clear()
        return interpret(res, tr("Takip"))
    }

    /**
     * Following a community, the way the hole page's own button does it.
     *
     * `/h/{hole}/follow` and `/h/{hole}/unfollow` are POST-only (a GET answers 405) and
     * take no body — the hole is in the path, which is exactly what the site's own
     * "… followed successfully!" answer belongs to.
     */
    suspend fun followHole(hole: String, follow: Boolean): ActionResult {
        ensureAntiforgery()
        val name = encodeSegment(hole)
        val path = if (follow) "/h/$name/follow" else "/h/$name/unfollow"
        val res = http.postForm(path, emptyList(), referer = "/h/$name")
        cache.clear()
        return interpret(res, tr("Topluluk"))
    }

    /** Notifications from one account, on or off. The site keys it by the account's id. */
    suspend fun muteUserNotifications(id: String, mute: Boolean): ActionResult {
        ensureAntiforgery()
        val path = if (mute) "/mute_notifs/${encodeSegment(id)}" else "/unmute_notifs/${encodeSegment(id)}"
        val res = http.postForm(path, emptyList(), referer = "/notifications")
        cache.clear()
        return interpret(res, tr("Bildirimler"))
    }

    /** Blocking has no id in the path, so the account travels in the form. */
    suspend fun blockUser(name: String, block: Boolean): ActionResult {
        ensureAntiforgery()
        val path = if (block) "/block_user" else "/unblock_user"
        val res = http.postForm(path, listOf("username" to name), referer = "/@${encodeSegment(name)}")
        cache.clear()
        return interpret(res, tr("Engel"))
    }

    /** One path segment, escaped the way a browser escapes it. */
    private fun encodeSegment(value: String): String =
        java.net.URLEncoder.encode(value, "UTF-8").replace("+", "%20")

    /**
     * Whether the viewer follows a community, as its own page renders it.
     *
     * A guest page carries no follow control at all, so `null` means "the page did not
     * say" rather than "not following".
     */
    suspend fun holeState(hole: String): HoleState =
        HtmlParser.parseHoleState(page("/h/" + encodeSegment(hole)))

    /**
     * Whether a community shows itself to this account at all.
     *
     * A private community answers 403 with its lock screen, which is an answer and not a
     * failure — so this reads it directly instead of going through [page], whose job is to
     * throw on anything that is not a page.
     */
    suspend fun holeAccess(hole: String): HoleAccess {
        val path = "/h/" + encodeSegment(hole)
        val res = runCatching { http.get(path) }.getOrNull() ?: return HoleAccess()
        val lock = HtmlParser.parseHoleLock(res.body)
        // A public community's page has no lock screen and parseHoleLock says null.
        return lock ?: HoleAccess()
    }

    /**
     * Asks to join a private community, with the reason its moderators will read.
     *
     * The site's own join dialog posts the same route with a `reason` field and answers
     * `I_JOIN_REQUESTED`; that message is what the app says back, instead of a generic
     * "done" that leaves the person unsure whether the request was sent.
     */
    suspend fun requestJoin(hole: String, reason: String): ActionResult {
        ensureAntiforgery()
        val name = encodeSegment(hole)
        val res = http.postForm(
            "/h/$name/follow",
            listOf("reason" to reason.take(500)),
            referer = "/h/$name",
        )
        val asked = res.body.contains("I_JOIN_REQUESTED") || res.body.contains("awaiting", true)
        val done = if (asked) {
            ActionResult(true, tr("Katılma isteğin gönderildi. Onay bekleniyor."), res)
        } else {
            interpret(res, tr("Katılma isteği"))
        }
        cache.clear()
        return done
    }

    /** Hiding a whole community from the feed, the page's eye-slash button. */
    suspend fun blockHole(hole: String, block: Boolean): ActionResult {
        ensureAntiforgery()
        val name = encodeSegment(hole)
        val res = http.postForm(
            if (block) "/h/$name/block" else "/h/$name/unblock",
            emptyList(),
            referer = "/h/$name",
        )
        cache.clear()
        return interpret(res, tr("Topluluk"))
    }

    /** A profile's Posts tab, read from its own page so the list stays in the app. */
    suspend fun userPosts(name: String, force: Boolean = false): List<FeedPost> =
        HtmlParser.parseUserPosts(page("/@$name/posts", force = force))

    /** A profile's Comments tab, same idea: the page is the only source the site offers. */
    suspend fun userComments(name: String, force: Boolean = false): List<UserComment> =
        HtmlParser.parseUserComments(page("/@$name/comments", force = force))

    /**
     * `/notifications`, as the site renders it.
     *
     * Not cached for long: the bell is a "how many are waiting" question, and a stale
     * answer there is the same as a wrong one. Thirty seconds is enough to stop a screen
     * re-opening from asking twice.
     */
    suspend fun notifications(force: Boolean = true): NotificationPage =
        HtmlParser.parseNotifications(page("/notifications", force = force, ttlMs = 30_000))

    /**
     * "Mark all notifications as read" — the button on `/notifications` calls
     * `postToastReload(this, '/clear')`, so this is the same POST without the reload.
     */
    suspend fun clearNotifications(): ActionResult {
        ensureAntiforgery()
        val res = http.postForm("/clear", emptyList(), referer = "/notifications")
        cache.clear()
        return interpret(res, tr("Bildirimler"))
    }

    suspend fun contact(message: String): ActionResult {
        ensureAntiforgery()
        val res = http.postForm("/contact", listOf("message" to message), referer = "/contact")
        return interpret(res, "Mesaj")
    }

    /**
     * The two picture forms on `/settings`, as the site itself sends them.
     *
     * Captured from the browser (delidehset.har, 18 Oct 2026): each is a plain multipart
     * POST to **its own** action — `/settings/images/profile` and `/settings/images/banner`
     * — carrying the antiforgery token and one file part named `profile` or `banner`. A
     * profile upload answers `{"meta":{"status":true,…},"data":{"profileurl":
     * "/uploads/…-profile-….jpg"}}` and the page then shows the new file.
     *
     * Posting that same part to `/settings/personal` — which the outer settings form on the
     * page happens to accept — answers `{"message":"Saved!"}`, stores no picture, and saves
     * the bio and pronouns it was *not* given, i.e. blanks them. That is exactly how an
     * upload can report success, change nothing, and quietly eat the bio: which is what
     * this app did until the HAR showed the two requests side by side.
     */
    private suspend fun pictureEndpoint(kind: String): Pair<String, String> = uploadTarget(kind)

    suspend fun uploadProfileImage(fileName: String, contentType: String, bytes: ByteArray): ActionResult {
        val (path, field) = pictureEndpoint("profile")
        return settingsUpload(path, field, fileName, contentType, bytes, referer = "/settings/personal")
    }

    /** The banner is the same request against its own route; only the part name differs. */
    suspend fun uploadBanner(fileName: String, contentType: String, bytes: ByteArray): ActionResult {
        val (path, field) = pictureEndpoint("banner")
        return settingsUpload(path, field, fileName, contentType, bytes, referer = "/settings/personal")
    }

    /** What the live settings page says about its own picture forms; [] when unreadable. */
    private var imageForms: Map<String, Pair<String, String>>? = null

    /**
     * The endpoint and file field name the server currently expects for a picture.
     *
     * Taken from the settings page itself; the names below are only the fallback for a
     * build where that page cannot be read at all.
     */
    private suspend fun uploadTarget(kind: String): Pair<String, String> {
        // `/settings/personal` is the page the browser actually lands on (the HAR's own
        // requests say so); `/settings` only redirects there, so ask for the real one.
        val discovered = imageForms ?: runCatching { HtmlParser.parseImageUploadForms(page("/settings/personal")) }
            .getOrDefault(emptyMap())
            .also {
                imageForms = it
                android.util.Log.d("DeliUpload", "settings picture forms: $it")
            }
        return discovered[kind] ?: if (kind == "banner") {
            "/settings/images/banner" to "banner"
        } else {
            "/settings/images/profile" to "profile"
        }
    }

    private suspend fun settingsPost(path: String, fields: List<Pair<String, String>>): ActionResult {
        ensureAntiforgery()
        val res = http.postForm(path, fields, referer = "/settings")
        // The settings pages are server-rendered, so the saved values come back on the
        // next fetch: drop the cached copies rather than showing what was just replaced.
        cache.clear()
        return interpret(res)
    }

    private suspend fun settingsUpload(
        path: String,
        field: String,
        fileName: String,
        contentType: String,
        bytes: ByteArray,
        referer: String = "/settings",
    ): ActionResult {
        ensureAntiforgery()
        val res = http.postFile(path, field, fileName, contentType, bytes, referer = referer)
        cache.clear()
        // Uploads are the one call whose failure mode is invisible in the UI (the picture
        // simply does not change), so what came back is worth a line in logcat — including
        // the *visible* text of an error page, which is where the site's reason for a 500
        // lives, rather than the head of its markup.
        android.util.Log.d(
            "DeliUpload",
            "POST $path ($fileName, $contentType, ${bytes.size} B) -> ${res.code} " +
                "${res.contentType} loc=${res.location} " +
                "text=${HtmlParser.stripTags(res.body).replace(Regex("\\s+"), " ").take(400)}",
        )
        return interpretPicture(res)
    }

    private fun interpretPicture(res: DeliResponse): ActionResult = interpretPictureResponse(res)

    suspend fun submitPost(
        form: DiscoveredForm?,
        title: String,
        body: String,
        hole: String,
        nsfw: Boolean = false,
        anonymous: Boolean = false,
        tripcodeSecret: String? = null,
    ): ActionResult {
        ensureAntiforgery()
        val fields = mutableListOf<Pair<String, String>>()
        for (f in form?.fields.orEmpty()) {
            if (f.kind == FieldKind.CHECKBOX && f.value.isEmpty()) continue
            when (f.name) {
                form?.bodyField, form?.titleField -> continue
                "anonymous" -> fields += f.name to anonymous.toString()
                "hole" -> fields += f.name to hole
                else -> fields += f.name to f.value
            }
        }
        fields += (form?.titleField ?: "title") to title
        fields += (form?.bodyField ?: "body") to body
        if (form?.fields?.none { it.name == "hole" } != false) fields += "hole" to hole
        if (nsfw && fields.none { it.first == "nsfw" }) fields += "nsfw" to "true"
        if (anonymous) {
            if (fields.none { it.first == "tripcode_secret" } && tripcodeSecret != null) {
                fields += "tripcode_secret" to tripcodeSecret
            }
        }

        val res = http.postMultipart("/submit", fields, referer = "/submit")
        return interpret(res)
    }

    /* ------------------------------------------------------------------ */
    /*  Browsing pages                                                    */
    /* ------------------------------------------------------------------ */

    /**
     * Fetches a server rendered page: a fresh cached copy is returned immediately,
     * and a stale copy is served when the network fails instead of an error screen.
     */
    suspend fun page(path: String, force: Boolean = false, ttlMs: Long = PAGE_TTL_MS): String {
        val key = "page:$path"
        if (!force) {
            cache.read(key)?.takeIf { it.isFresh(ttlMs) }?.let { return it.body }
        }
        val body = try {
            DeliErrors.withRetry { fetchPage(path) }
        } catch (t: Throwable) {
            cache.read(key)?.body ?: throw t
        }
        cache.write(key, body)
        return body
    }

    private suspend fun fetchPage(path: String): String {
        val res = http.get(path)
        if (res.isRedirect && res.location?.contains("/login") == true) throw NotSignedInException()
        if (!res.isSuccess) throw DeliException(DeliErrors.explainHttp(res.code, "Sayfa"))
        val doc = HtmlParser.document(res.body)
        http.updateAntiforgery(HtmlParser.antiforgeryToken(doc))
        return res.body
    }

    /** These pages redirect straight to a concrete page; we want the final URL. */
    suspend fun resolveRedirect(path: String): String? {
        val res = http.get(path)
        return res.location?.let { http.absolute(it) }
    }

    suspend fun holes(force: Boolean = false): List<Hole> =
        HtmlParser.parseHoles(page("/holes", force))

    suspend fun holeInfo(name: String, force: Boolean = false): Hole? =
        HtmlParser.parseHoleHeader(page("/h/$name", force))

    /**
     * How many posts a community really holds.
     *
     * The site's own community table prints `0` in its Posts column for every row — the
     * same `0` it prints for a community that is full of posts — so reading that column
     * told the app nothing, and every community looked empty. The feed API is the honest
     * source: it hands out a `next_cursor` only while another page follows, so a community
     * is counted until the pages run out. The counting stops at a ceiling rather than
     * walking a very large community for ever, and a community that is closed to this
     * viewer (403) simply has no count to give.
     */
    suspend fun holePostCount(name: String): Int? {
        if (name.isBlank()) return null
        var cursor: String? = null
        var total = 0
        repeat(HOLE_COUNT_PAGES) {
            val page = runCatching {
                feed(FeedSort.NEW, hole = name, cursor = cursor)
            }.getOrNull() ?: return null
            total += page.posts.size
            cursor = page.nextCursor?.takeIf { it.isNotBlank() }
            if (cursor == null || page.posts.isEmpty()) return total
        }
        return total
    }

    /** The same, for the whole community list, all at once. */
    suspend fun holePostCounts(names: List<String>): Map<String, Int> = coroutineScope {
        names.distinct()
            .map { name -> async { name to holePostCount(name) } }
            .awaitAll()
            .mapNotNull { (name, count) -> count?.let { name to it } }
            .toMap()
    }

    /**
     * A user's picture, from the endpoint the site's own hovercards call.
     *
     * The feed's JSON carries `author_avatar`, but a comment row and a row in the user
     * tables only carry the name. This is the one call that turns a name into a face;
     * [AvatarStore] remembers the answer, so a thread costs one request per person rather
     * than one per row.
     */
    suspend fun profilePicture(name: String): String? {
        val url = HttpUrl.Builder()
            .scheme("https")
            .host("delidehset.com")
            .addPathSegment("api")
            .addPathSegment("profile")
            .addPathSegment(name)
            .build()
            .toString()
        val res = runCatching { http.get(url, accept = "application/json") }.getOrNull() ?: return null
        if (!res.isSuccess) return null
        val pic = runCatching {
            json.parseToJsonElement(res.body).jsonObject["profile_pic"]?.jsonPrimitive?.contentOrNull
        }.getOrNull()
        return pic?.takeIf { it.isNotBlank() && !it.contains("default-profile-pic") }
    }

    suspend fun users(force: Boolean = false): List<SiteUser> =
        HtmlParser.parseUsers(page("/users", force))

    /**
     * Any page that renders the user table — a profile's `/followers` and `/following`.
     * These used to be handed to the web frame for no reason: the site answers them with
     * the same table `/users` does, which the app already knows how to read.
     */
    suspend fun usersAt(path: String, force: Boolean = false): List<SiteUser> =
        HtmlParser.parseUsers(page(path, force))

    /**
     * A profile, with the counters the site's own header gets wrong.
     *
     * Following somebody leaves the profile header still saying "follows 0 users" — the
     * page is rendered from a count that is not refreshed — while the list behind that
     * link is a page long. The two list headings are the honest numbers, so they are read
     * alongside the profile and take precedence over the header's. They are cached like
     * any other page, and the profile still renders even if they fail.
     */
    suspend fun profile(name: String, force: Boolean = false): UserProfile = coroutineScope {
        val profile = async { HtmlParser.parseProfile(page("/@$name", force), name) }
        val followers = async {
            runCatching { HtmlParser.parseRelationTotal(page("/@$name/followers", force)) }.getOrNull()
        }
        val following = async {
            runCatching { HtmlParser.parseRelationTotal(page("/@$name/following", force)) }.getOrNull()
        }
        val parsed = profile.await()
        val honestFollowers = followers.await() ?: parsed.followers
        val honestFollowing = following.await() ?: parsed.following
        parsed.copy(
            followers = honestFollowers,
            following = honestFollowing,
            // The `stats` rows are read off the same header as the counters, so they carry
            // the site's own wrong numbers — the header says "follows 0 users" while the
            // list behind it is a page long. They are rewritten to the numbers above, or a
            // screen drawing the stats would print the wrong count next to the right one.
            stats = parsed.stats.map { stat ->
                // The two rows the app itself writes, in whatever language is in force, plus
                // the English keys the site's own stats use when they are read instead.
                val key = stat.key.lowercase()
                when {
                    (stat.key == tr("Takipçi") || key.startsWith("follower")) && honestFollowers != null ->
                        stat.copy(value = honestFollowers.toString())
                    (stat.key == tr("Takip") || key.startsWith("following")) && honestFollowing != null ->
                        stat.copy(value = honestFollowing.toString())
                    else -> stat
                }
            },
        )
    }

    /* ------------------------------------------------------------------ */
    /*  A community's own settings                                        */
    /* ------------------------------------------------------------------ */

    /**
     * `/h/{hole}/settings` — the general tab, where the display name, the picture and the
     * content filters live. The site only answers this to the people who run the community;
     * [HoleSettings.canEdit] is the page's own answer to whether this account may write.
     */
    suspend fun holeSettings(hole: String, force: Boolean = false): HoleSettings? {
        val name = encodeSegment(hole)
        val body = runCatching { page("/h/$name/settings?tab=general", force) }.getOrNull() ?: return null
        return HtmlParser.parseHoleSettings(body, hole)
    }

    suspend fun saveHoleSettings(hole: String, displayName: String, blockedIps: String, blockedWords: String): ActionResult {
        val name = encodeSegment(hole)
        ensureAntiforgery()
        val display = http.postForm(
            "/h/$name/display_name",
            listOf("display_name" to displayName),
            referer = "/h/$name/settings?tab=general",
        )
        val filters = http.postForm(
            "/h/$name/settings/content-filters",
            listOf("blocked_ip_ranges" to blockedIps, "blocked_words" to blockedWords),
            referer = "/h/$name/settings?tab=filters",
        )
        cache.clear()
        val displayResult = interpret(display, tr("Topluluk ayarları"))
        val filterResult = interpret(filters, tr("İçerik filtreleri"))
        return if (!displayResult.ok) displayResult else filterResult
    }

    /** The community's picture, posted to the form the settings page renders. */
    suspend fun uploadHoleLogo(
        hole: String,
        fileName: String,
        contentType: String,
        bytes: ByteArray,
    ): ActionResult {
        ensureAntiforgery()
        val name = encodeSegment(hole)
        val res = http.postFile(
            path = "/h/$name/marsey_image",
            fieldName = "marsey",
            fileName = fileName,
            contentType = contentType,
            bytes = bytes,
            referer = "/h/$name/settings?tab=general",
        )
        cache.clear()
        return interpretPictureResponse(res)
    }

    /** The site's rules, which the app shows once before the rest of the app is used. */
    suspend fun rules(force: Boolean = false): SiteRules =
        HtmlParser.parseRules(page("/rules", force, ttlMs = 24 * 60 * 60 * 1000L))

    /**
     * The site's search, one tab at a time: `posts`, `comments` or `users`. The page is
     * read from its results pane only — see [HtmlParser.parseSearch] for why that matters.
     */
    suspend fun search(kind: String, query: String): List<SimpleRow> {
        val url = HttpUrl.Builder()
            .scheme("https").host("delidehset.com")
            .addPathSegment("search")
            .addPathSegment(kind)
            .addQueryParameter("q", query)
            .build()
        val res = http.get(url.toString())
        http.updateAntiforgery(HtmlParser.antiforgeryToken(HtmlParser.document(res.body)))
        return HtmlParser.parseSearch(res.body)
    }

    suspend fun searchPosts(query: String): List<SimpleRow> = search("posts", query)

    suspend fun searchSuggest(query: String): List<String> {
        val url = "${DeliHttp.BASE_URL}/api/search/suggest?q=" + java.net.URLEncoder.encode(query, "UTF-8")
        return runCatching {
            val res = http.get(url, accept = "application/json")
            val arr = json.parseToJsonElement(res.body)
            if (arr is kotlinx.serialization.json.JsonArray) {
                arr.mapNotNull { el ->
                    runCatching {
                        val o = el.jsonObject
                        (o["name"] ?: o["value"] ?: o["text"] ?: o["label"])?.jsonPrimitive?.content
                    }.getOrNull()
                }
            } else emptyList()
        }.getOrDefault(emptyList())
    }

    suspend fun badges(force: Boolean = false): List<BadgeRow> =
        HtmlParser.parseBadges(page("/badges", force))

    suspend fun table(path: String, force: Boolean = false): TablePage {
        val html = page(path, force)
        val rows = HtmlParser.parseRows(html)
        return TablePage(
            headers = HtmlParser.parseRowHeaders(html),
            rows = rows,
            // Only worth carrying when there is nothing else to show.
            note = if (rows.isEmpty()) HtmlParser.pageNote(html) else null,
        )
    }

    /* ------------------------------------------------------------------ */
    /*  Helpers                                                           */
    /* ------------------------------------------------------------------ */

    private suspend fun ensureAntiforgery() {
        if (http.currentAntiforgery() == null) {
            val res = http.get("/")
            http.updateAntiforgery(HtmlParser.antiforgeryToken(HtmlParser.document(res.body)))
        }
    }

    private fun interpret(res: DeliResponse, what: String = "İşlem"): ActionResult =
        interpretAction(res, what)
}

/**
 * The exact body `/comment` expects, built from the form the server rendered.
 *
 * The shipped build names the thread field `parent_fullname` and encodes the target as
 * `p_<post uuid>` for a top-level comment and `c_<comment uuid>` for a reply — the same
 * values the site's own JavaScript writes into the hidden input it generates. A build
 * without that field falls back to the older `post_id`/`parent_id` names.
 */
internal fun commentFields(
    form: DiscoveredForm?,
    postId: String,
    body: String,
    parentId: String?,
    hole: String?,
): List<Pair<String, String>> {
    val formFields = form?.fields.orEmpty()
    val parentField = formFields.firstOrNull { it.name.contains("parent", ignoreCase = true) }?.name
    val fields = mutableListOf<Pair<String, String>>()

    // Start from the discovered form so hidden fields are preserved.
    for (f in formFields) {
        if (f.kind == FieldKind.CHECKBOX && f.value.isEmpty()) continue
        val value = when (f.name) {
            form?.bodyField -> body
            parentField -> if (parentId.isNullOrBlank()) {
                f.value.ifBlank { "p_$postId" }
            } else {
                "c_$parentId"
            }
            else -> f.value
        }
        fields += f.name to value
    }

    // Body: use the discovered field name, otherwise the one the reply box posts.
    if (form?.bodyField == null || formFields.none { it.name == form.bodyField }) {
        fields += (form?.bodyField ?: "body") to body
    }

    // Older builds carry no thread field at all: fall back to the usual names.
    if (parentField == null) {
        if (parentId != null) fields += "parent_id" to parentId
        fields += "post_id" to postId
        if (hole != null) fields += "hole" to hole
    }

    return fields
}

/**
 * Turns whatever a write endpoint answered into a success/failure the UI can show.
 *
 * It lives outside the repository because it is the part most likely to be wrong: the
 * site's endpoints answer with HTTP 200 even when they fail, with the real outcome in a
 * JSON body (`{error}` or a payload such as `{cid}`).
 */
private val actionJson = Json { ignoreUnknownKeys = true; isLenient = true }

/**
 * What a picture upload actually did.
 *
 * The server's shape here is `{meta:{status, messages}, data:{profileurl}}` (a banner
 * answers the same way under its own key), and `data` is the proof a file was stored. A
 * 200 with a cheerful message is *not* proof: `/settings/personal` — the outer form on the
 * settings page — answers `{"message":"Saved!"}` to a request that carries a picture it
 * ignores, which is how the app could report a change that never happened. So a JSON
 * answer without a stored URL is treated as a failure, never as a silent success.
 */
internal fun interpretPictureResponse(res: DeliResponse): ActionResult {
    if (res.isJson) {
        val root = runCatching { actionJson.parseToJsonElement(res.body).jsonObject }.getOrNull()
        if (root != null) {
            val meta = root["meta"] as? kotlinx.serialization.json.JsonObject
            val status = runCatching { meta?.get("status")?.jsonPrimitive?.content }.getOrNull()
            val message = runCatching {
                (meta?.get("messages") as? kotlinx.serialization.json.JsonArray)
                    ?.firstOrNull()?.jsonPrimitive?.content
            }.getOrNull()?.takeIf { it.isNotBlank() && it != "[]" }
            val url = runCatching {
                (root["data"] as? kotlinx.serialization.json.JsonObject)
                    ?.let { d -> (d["profileurl"] ?: d["bannerurl"])?.jsonPrimitive?.content }
            }.getOrNull()?.takeIf { it.isNotBlank() }

            if (url != null) return ActionResult(true, null, res)
            return ActionResult(
                ok = false,
                message = message
                    ?: if (status == "false") tr("Sunucu görseli kabul etmedi.") else tr("Görsel kaydedilmedi."),
                raw = res,
            )
        }
    }
    return interpretAction(res, tr("Görsel"))
}

internal fun interpretAction(res: DeliResponse, what: String = tr("İşlem")): ActionResult {
    run {
        if (res.isRedirect) {
            val loc = res.location.orEmpty()
            return if (loc.contains("/login")) {
                ActionResult(false, trf("{0} için oturum açman gerekiyor.", what), res)
            } else {
                ActionResult(true, null, res)
            }
        }

        if (res.isJson) {
            val parsed = runCatching {
                actionJson.parseToJsonElement(res.body).jsonObject
            }.getOrNull()
            // The site answers its own JavaScript with `{error}` on failure and a
            // payload (e.g. `{cid}`) on success — and it does so with HTTP 200, so the
            // status code alone cannot be trusted.
            val error = parsed?.let { o ->
                (o["error"] ?: o["detail"])?.jsonPrimitive?.content?.takeIf { it.isNotBlank() }
            }
            val message = parsed?.let { o ->
                (o["message"] ?: o["error"] ?: o["detail"])?.jsonPrimitive?.content
                    ?.takeIf { it.isNotBlank() }
            }
            return ActionResult(
                ok = res.isSuccess && error == null,
                message = message ?: if (res.isSuccess) null else DeliErrors.explainHttp(res.code, what),
                raw = res,
            )
        }

        // Server rendered error pages: pull out the visible message if there is one.
        val serverMessage = runCatching {
            val doc = HtmlParser.document(res.body)
            doc.select(".invalid-feedback, .alert-danger li, .alert-danger, .validation-summary-errors li, .text-danger")
                .firstOrNull { it.text().isNotBlank() }
                ?.text()
                ?.trim()
                ?.take(240)
        }.getOrNull()

        val looksLikeError = res.code >= 400 || serverMessage != null
        val fallback = HtmlParser.stripTags(res.body).take(240).ifBlank { null }

        return ActionResult(
            ok = !looksLikeError,
            message = when {
                !looksLikeError -> null
                serverMessage != null -> serverMessage
                res.code >= 400 -> DeliErrors.explainHttp(res.code, "İşlem")
                else -> fallback ?: "İşlem tamamlanamadı. Site beta olduğu için arada hata veriyor."
            },
            raw = res,
        )
    }
}
