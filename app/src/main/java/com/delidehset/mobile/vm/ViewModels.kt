package com.delidehset.mobile.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.delidehset.mobile.data.ActionResult
import com.delidehset.mobile.data.BadgeRow
import com.delidehset.mobile.data.CommentNode
import com.delidehset.mobile.data.DeliErrors
import com.delidehset.mobile.data.DeliRepository
import com.delidehset.mobile.data.DiscoveredForm
import com.delidehset.mobile.data.FeedPost
import com.delidehset.mobile.data.FeedSort
import com.delidehset.mobile.data.Hole
import com.delidehset.mobile.data.LangStore
import com.delidehset.mobile.data.LoginResult
import com.delidehset.mobile.data.NotificationPage
import com.delidehset.mobile.data.NotSignedInException
import com.delidehset.mobile.data.PersonalSettings
import com.delidehset.mobile.data.PostDetail
import com.delidehset.mobile.data.SecuritySettings
import com.delidehset.mobile.data.SimpleRow
import com.delidehset.mobile.data.SiteUser
import com.delidehset.mobile.data.SessionStore
import com.delidehset.mobile.data.CustomThemeSpec
import com.delidehset.mobile.data.ThemeStore
import com.delidehset.mobile.data.UserComment
import com.delidehset.mobile.data.UserProfile
import com.delidehset.mobile.data.Viewer
import com.delidehset.mobile.ui.AppLang
import com.delidehset.mobile.ui.CurrentLang
import com.delidehset.mobile.ui.theme.DeliColors
import com.delidehset.mobile.ui.theme.DeliPalettes
import com.delidehset.mobile.ui.theme.Palette
import com.delidehset.mobile.ui.tr
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/* ------------------------------------------------------------------ */
/*  App shell                                                          */
/* ------------------------------------------------------------------ */

private const val WARM_START_DELAY_MS = 4_000L

class AppViewModel(
    private val repo: DeliRepository,
    private val session: SessionStore,
    private val langStore: LangStore,
    private val themeStore: ThemeStore,
) : ViewModel() {

    private val _viewer = MutableStateFlow(Viewer(isSignedIn = false))
    val viewer: StateFlow<Viewer> = _viewer.asStateFlow()

    /**
     * The language of the app, which is also the site's — one setting, picked once in
     * Ayarlar. The choice is written to the device first, so it holds with no account and
     * no network, and sent to the site when there is a session to send it with.
     */
    val lang: StateFlow<AppLang> = langStore.language
        .map { AppLang.of(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppLang.TR)

    fun setLang(lang: AppLang) {
        viewModelScope.launch(Dispatchers.IO) {
            langStore.save(lang.code)
            // A joke language is the app's own: the site's list is `tr, en, de, …`, and
            // sending it `yavsar` would either be ignored or set something unintended.
            if (!lang.easter && _viewer.value.isSignedIn) runCatching { repo.saveLanguage(lang.code) }
        }
    }

    /**
     * The app's own look. Unlike the language this never goes to the site: the site's theme
     * setting is the site's, and the app has its own screens, so it has its own palette.
     */
    val theme: StateFlow<Palette> = kotlinx.coroutines.flow.combine(
        themeStore.theme,
        themeStore.customSpecs,
    ) { id, specs ->
        val customPalettes = specs.map { it.toPalette() }
        DeliPalettes.setCustom(customPalettes)
        DeliPalettes.of(id)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, DeliPalettes.fallback)

    fun setTheme(palette: Palette) {
        // Applied here rather than only through the stored flow, so the tap repaints
        // immediately instead of one disk read later.
        DeliColors.apply(palette)
        viewModelScope.launch(Dispatchers.IO) { themeStore.save(palette.id) }
    }

    fun saveCustomTheme(spec: CustomThemeSpec) {
        val palette = spec.toPalette()
        DeliPalettes.setCustom(DeliPalettes.custom.filterNot { it.id == palette.id } + palette)
        DeliColors.apply(palette)
        viewModelScope.launch(Dispatchers.IO) { themeStore.saveCustom(spec) }
    }

    fun deleteCustomTheme(id: String) {
        val next = DeliPalettes.custom.filterNot { it.id == id }
        DeliPalettes.setCustom(next)
        if (DeliColors.palette.id == id) DeliColors.apply(DeliPalettes.fallback)
        viewModelScope.launch(Dispatchers.IO) { themeStore.deleteCustom(id) }
    }

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    /** True until the first bootstrap finishes, so we can show a splash. */
    private val _booting = MutableStateFlow(true)
    val booting: StateFlow<Boolean> = _booting.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            session.viewer.collect { cached -> if (cached.isSignedIn) _viewer.value = cached }
        }
        // The data layer writes its errors and results as Turkish templates and cannot read
        // the composition, so the chosen language is mirrored where it can see it.
        viewModelScope.launch(Dispatchers.IO) {
            langStore.language.collect { code -> if (!code.isNullOrBlank()) CurrentLang.code = code }
        }
        // The stored palette is pushed into the colours the screens read. Collected on the
        // main dispatcher on purpose: this writes a snapshot state that recomposes the UI.
        viewModelScope.launch {
            theme.collect { palette -> DeliColors.apply(palette) }
        }
        refresh()
    }

    fun refresh() {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { repo.bootstrap() }
                .onSuccess { _viewer.value = it }
                .onFailure { /* keep cached state; the network will be retried */ }
            _booting.value = false
            // The site is slow enough that even a plain menu showed a spinner, so the
            // most used menu pages are warmed in the background — but only once the
            // first screen has had time to land, otherwise the warm-up steals the
            // connection from the feed the user is waiting for.
            warming?.cancel()
            warming = viewModelScope.launch(Dispatchers.IO) {
                delay(WARM_START_DELAY_MS)
                repo.warmPages()
                // The account's language is the app's language, so a choice made on the site
                // is picked up here as well, instead of the app quietly disagreeing with the
                // pages it is reading from. Signed out there is nothing to read — and the
                // settings page is already warm by now, so this costs no request of its own.
                if (_viewer.value.isSignedIn) {
                    val accountLanguage = runCatching { repo.accountLanguage() }.getOrNull()
                    if (!accountLanguage.isNullOrBlank()) langStore.save(accountLanguage)
                }
            }
        }
    }

    private var warming: Job? = null

    fun logout() {
        viewModelScope.launch(Dispatchers.IO) {
            repo.logout()
            _viewer.value = Viewer(isSignedIn = false)
            _message.value = tr("Çıkış yapıldı.")
        }
    }

    fun consumeMessage() { _message.value = null }
    fun showMessage(text: String) { _message.value = text }
}

/* ------------------------------------------------------------------ */
/*  Feed                                                               */
/* ------------------------------------------------------------------ */

data class FeedUiState(
    val posts: List<FeedPost> = emptyList(),
    val sort: FeedSort = FeedSort.HOT,
    val hole: String? = null,
    val effortOnly: Boolean = false,
    val canVote: Boolean = false,
    val loading: Boolean = false,
    val loadingMore: Boolean = false,
    val error: String? = null,
    val nextCursor: String? = null,
    val endReached: Boolean = false,
)

class FeedViewModel(private val repo: DeliRepository) : ViewModel() {

    private val _state = MutableStateFlow(FeedUiState())
    val state: StateFlow<FeedUiState> = _state.asStateFlow()

    private var started = false

    /** Whether the viewer follows the hole on screen; null before anything was asked. */
    private val _holeFollowing = MutableStateFlow<Boolean?>(null)
    val holeFollowing: StateFlow<Boolean?> = _holeFollowing.asStateFlow()

    /** Whether the viewer hid the hole. The page's own eye-slash button flips this. */
    private val _holeBlocked = MutableStateFlow<Boolean?>(null)
    val holeBlocked: StateFlow<Boolean?> = _holeBlocked.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun consumeMessage() { _message.value = null }

    fun start(hole: String?, sort: FeedSort, effortOnly: Boolean = false) {
        if (started && _state.value.hole == hole && _state.value.sort == sort) return
        started = true
        _holeFollowing.value = null
        _holeBlocked.value = null
        _state.value = FeedUiState(sort = sort, hole = hole, effortOnly = effortOnly)
        // Cache first: opening a feed you already visited renders immediately.
        load(reset = true, force = false)
        if (hole != null) loadHoleState(hole)
    }

    /** Reads the community's own page so its join/leave state is right on arrival. */
    private fun loadHoleState(hole: String) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { repo.holeState(hole) }
                .onSuccess { state ->
                    state.joined?.let { _holeFollowing.value = it }
                    state.blocked?.let { _holeBlocked.value = it }
                }
        }
    }

    /** Hiding a community from the feed, as the hole page's own button does. */
    fun toggleHoleBlock(hole: String) {
        val target = !(_holeBlocked.value ?: false)
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { repo.blockHole(hole, target) }
                .onSuccess { result ->
                    if (result.ok) {
                        _holeBlocked.value = target
                        _message.value = tr(if (target) "Topluluk engellendi." else "Topluluk engeli kaldırıldı.")
                    } else {
                        _message.value = result.message ?: tr("İşlem tamamlanamadı.")
                    }
                }
                .onFailure { _message.value = DeliErrors.explain(it) }
        }
    }

    /** Follow or leave a community; the hole's own page uses the same two routes. */
    fun toggleHoleFollow(hole: String) {
        val target = !(_holeFollowing.value ?: false)
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { repo.followHole(hole, target) }
                .onSuccess { result ->
                    if (result.ok) {
                        _holeFollowing.value = target
                        _message.value = tr(
                            if (target) "Topluluk takip edildi." else "Topluluk takibi bırakıldı."
                        )
                    } else {
                        _message.value = result.message ?: tr("İşlem tamamlanamadı.")
                    }
                }
                .onFailure { _message.value = DeliErrors.explain(it) }
        }
    }

    fun setSort(sort: FeedSort) {
        if (_state.value.sort == sort) return
        _state.update { it.copy(sort = sort, posts = emptyList(), nextCursor = null, endReached = false) }
        load(reset = true, force = false)
    }

    fun setEffortOnly(value: Boolean) {
        _state.update { it.copy(effortOnly = value, posts = emptyList(), nextCursor = null, endReached = false) }
        load(reset = true, force = false)
    }

    /** Explicit user pull-to-refresh always goes to the network. */
    fun refresh() = load(reset = true, force = true)

    fun loadMore() {
        val s = _state.value
        if (s.loading || s.loadingMore || s.endReached || s.nextCursor == null) return
        load(reset = false, force = false)
    }

    private fun load(reset: Boolean, force: Boolean) {
        val s = _state.value
        _state.update { it.copy(loading = reset, loadingMore = !reset, error = null) }
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                repo.feed(
                    sort = s.sort,
                    hole = s.hole,
                    cursor = if (reset) null else s.nextCursor,
                    effortOnly = s.effortOnly,
                    force = force,
                )
            }.onSuccess { res ->
                _state.update { prev ->
                    prev.copy(
                        posts = if (reset) res.posts else prev.posts + res.posts,
                        nextCursor = res.nextCursor,
                        endReached = res.nextCursor.isNullOrBlank() || res.posts.isEmpty(),
                        loading = false,
                        loadingMore = false,
                        error = null,
                    )
                }
                // Warm the first few post pages so tapping one is instant.
                if (reset) {
                    runCatching { repo.prefetchPosts(res.posts.map { it.permalink }) }
                }
            }.onFailure { e ->
                _state.update { it.copy(loading = false, loadingMore = false, error = actOn(e)) }
            }
        }
    }

    /** Optimistic vote, reverted when the server refuses. */
    fun vote(post: FeedPost, value: Int) {
        val before = _state.value.posts
        _state.update { prev ->
            prev.copy(posts = prev.posts.map { p ->
                if (p.id != post.id) p else {
                    val delta = value - p.viewerVote
                    p.copy(score = p.score + delta, viewerVote = value)
                }
            })
        }
        viewModelScope.launch(Dispatchers.IO) {
            val res = runCatching { repo.vote("post", post.id, value) }.getOrNull()
            if (res == null || !res.ok) {
                _state.update { it.copy(posts = before) }
                lastError = res?.message ?: "Oy kaydedilemedi."
            }
        }
    }

    var lastError: String? = null
        private set

    /** Failures that mean "sign in first" also nudge the user towards the login screen. */
    private fun actOn(e: Throwable): String = when (e) {
        is NotSignedInException -> "Bu akış için oturum açman gerekiyor."
        else -> DeliErrors.explain(e)
    }
}

/* ------------------------------------------------------------------ */
/*  Post detail                                                        */
/* ------------------------------------------------------------------ */

data class PostUiState(
    val post: PostDetail? = null,
    val comments: List<CommentNode> = emptyList(),
    val commentVotes: Map<String, Int> = emptyMap(),
    val commentForm: DiscoveredForm? = null,
    val loading: Boolean = true,
    val error: String? = null,
    val sending: Boolean = false,
    val replyTo: CommentNode? = null,
    val canInteract: Boolean = false,
    val message: String? = null,
    /**
     * A comment this app just sent that the refreshed page has not shown yet.
     *
     * The site answers `/comment` with the new id and only renders it into the page on the
     * next request; until that request carries it, the app draws it itself at the bottom of
     * the thread so the person sees the comment they just wrote instead of an unchanged
     * page.
     */
    val posted: CommentNode? = null,
)

class PostViewModel(private val repo: DeliRepository) : ViewModel() {

    private val _state = MutableStateFlow(PostUiState())
    val state: StateFlow<PostUiState> = _state.asStateFlow()

    private var permalink: String = ""
    private var canVote = false

    fun setVoting(canVoteNow: Boolean) { canVote = canVoteNow }

    fun load(permalink: String, force: Boolean = false) {
        if (!force && this.permalink == permalink && _state.value.post != null) return
        // Opening a post re-reads it: the page cache is what made a thread look frozen — a
        // post opened an hour later still showed the comments it had then, and a reply
        // somebody else had left in between never arrived.
        val fresh = force || this.permalink != permalink
        this.permalink = permalink
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { repo.post(permalink, fresh) }
                .onSuccess { page ->
                    _state.update {
                        it.copy(
                            post = page.post,
                            comments = page.comments,
                            commentVotes = page.commentVotes,
                            commentForm = page.commentForm,
                            canInteract = page.commentForm != null,
                            loading = false,
                            error = null,
                            // Once the refreshed page carries the comment, the local copy
                            // is no longer needed — the real row takes over.
                            posted = it.posted?.takeIf { local -> !page.comments.carriesComment(local) },
                        )
                    }
                }
                .onFailure { e ->
                    _state.update { it.copy(loading = false, error = DeliErrors.explain(e)) }
                }
        }
    }

    fun votePost(value: Int) {
        val p = _state.value.post ?: return
        val delta = value - p.viewerVote
        val shown = p.score?.let { it + delta }
        _state.update { it.copy(post = p.copy(score = shown, viewerVote = value)) }
        viewModelScope.launch(Dispatchers.IO) {
            val res = runCatching { repo.vote("post", p.id, value) }.getOrNull()
            if (res == null || !res.ok) {
                _state.update { it.copy(post = p, message = res?.message ?: "Oy kaydedilemedi.") }
            }
        }
    }

    fun voteComment(comment: CommentNode, value: Int) {
        val before = _state.value.commentVotes[comment.id] ?: comment.viewerVote
        _state.update { it.copy(commentVotes = it.commentVotes + (comment.id to value)) }
        viewModelScope.launch(Dispatchers.IO) {
            val res = runCatching { repo.vote("comment", comment.id, value) }.getOrNull()
            if (res == null || !res.ok) {
                _state.update { it.copy(commentVotes = it.commentVotes + (comment.id to before)) }
            }
        }
    }

    fun startReply(comment: CommentNode?) = _state.update { it.copy(replyTo = comment) }
    fun cancelReply() = _state.update { it.copy(replyTo = null) }

    fun sendComment(body: String, authorName: String? = null) {
        val s = _state.value
        val post = s.post ?: return
        if (body.isBlank()) return
        _state.update { it.copy(sending = true, message = null) }
        viewModelScope.launch(Dispatchers.IO) {
            val res = runCatching {
                repo.comment(s.commentForm, post.id, body, s.replyTo?.id, post.hole)
            }.getOrNull()
            _state.update { it.copy(sending = false, replyTo = null) }
            if (res?.ok == true) {
                val cid = res.commentId()
                // A reply is drawn where it belongs — under the comment it answers — instead
                // of at the end of the thread, where it read as one more new comment.
                val parent = s.replyTo
                val local = CommentNode(
                    id = cid ?: "local-${System.currentTimeMillis()}",
                    parentId = parent?.id,
                    depth = if (parent == null) 0 else parent.depth + 1,
                    authorName = authorName,
                    authorHref = authorName?.let { "/@$it" },
                    authorAvatar = null,
                    bodyHtml = "<p>" + body.replace("<", "&lt;") + "</p>",
                    score = 1,
                    viewerVote = 1,
                    ago = tr("şimdi"),
                    isDeleted = false,
                )
                _state.update { it.copy(message = tr("Yorum gönderildi."), posted = local) }
                load(permalink, force = true)
            } else {
                _state.update { it.copy(message = res?.message ?: "Yorum gönderilemedi.") }
            }
        }
    }

    fun reportPost(reason: String, platform: Boolean) {
        val post = _state.value.post ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val res = runCatching { repo.reportPost(post.id, reason, platform) }.getOrNull()
            _state.update { it.copy(message = res?.message ?: "Şikâyet iletildi.") }
        }
    }

    fun deletePost() {
        val post = _state.value.post ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val res = runCatching { repo.deletePost(post.id) }.getOrNull()
            _state.update { it.copy(message = res?.message ?: "Gönderi silindi.") }
            if (res?.ok == true) load(permalink, force = true)
        }
    }

    fun consumeMessage() = _state.update { it.copy(message = null) }
}

/** Whether the thread already carries this comment, at any depth. */
internal fun List<CommentNode>.carriesComment(local: CommentNode): Boolean =
    any { it.sameCommentAs(local) || it.children.carriesComment(local) }

/**
 * Whether a row in the thread is the comment that was just sent.
 *
 * The id alone is not enough: `/comment` answers with the platform's own `cid`, while the
 * rows in the thread carry the id the site rendered, and the two need not be the same
 * string. Matching only on the id left the local copy on screen next to the real row, so
 * the first comment appeared twice. The body — and the author, when both are known — is
 * the fallback.
 */
internal fun CommentNode.sameCommentAs(local: CommentNode): Boolean {
    val mine = id.trim().removePrefix("comment-")
    val theirs = local.id.trim().removePrefix("comment-")
    if (mine.isNotEmpty() && mine == theirs) return true
    if (bodyHtml.trim() != local.bodyHtml.trim()) return false
    val a = authorName?.trim()
    val b = local.authorName?.trim()
    return a == null || b == null || a == b
}

/** The id the site hands back for a freshly created comment, out of `{"cid":"…"}`. */
private fun ActionResult.commentId(): String? = runCatching {
    val body = raw?.body ?: return null
    val root = kotlinx.serialization.json.Json.parseToJsonElement(body)
    (root as? kotlinx.serialization.json.JsonObject)
        ?.get("cid")?.let { (it as? kotlinx.serialization.json.JsonPrimitive)?.content }
        ?.takeIf { it.isNotBlank() }
}.getOrNull()

/* ------------------------------------------------------------------ */
/*  Simple lists: holes, users, badges, log pages, profiles            */
/* ------------------------------------------------------------------ */

data class ListUiState<T>(
    val items: List<T> = emptyList(),
    val headers: List<String> = emptyList(),
    val loading: Boolean = true,
    val error: String? = null,
    /** What the page itself says when it has nothing to list. */
    val note: String? = null,
)

class HolesViewModel(private val repo: DeliRepository) : ViewModel() {
    private val _state = MutableStateFlow(ListUiState<Hole>())
    val state: StateFlow<ListUiState<Hole>> = _state.asStateFlow()
    private var loaded = false

    fun load(force: Boolean = false) {
        if (loaded && !force) return
        loaded = true
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { repo.holes(force) }
                .onSuccess { _state.value = ListUiState(items = it, loading = false) }
                .onFailure { _state.value = ListUiState(loading = false, error = DeliErrors.explain(it)) }
        }
    }
}

class UsersViewModel(private val repo: DeliRepository) : ViewModel() {
    private val _state = MutableStateFlow(ListUiState<SiteUser>())
    val state: StateFlow<ListUiState<SiteUser>> = _state.asStateFlow()
    private var loaded = false

    fun load(force: Boolean = false) {
        if (loaded && !force) return
        loaded = true
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { repo.users(force) }
                .onSuccess { _state.value = ListUiState(items = it, loading = false) }
                .onFailure { _state.value = ListUiState(loading = false, error = DeliErrors.explain(it)) }
        }
    }
}

/**
 * The two lists a profile links to: its followers and the accounts it follows.
 *
 * The site renders both as the same table `/users` uses, so the same reader serves all
 * three — and the app no longer has to hand the person off to a web frame for them.
 */
class RelationUsersViewModel(private val repo: DeliRepository) : ViewModel() {
    private val _state = MutableStateFlow(ListUiState<SiteUser>())
    val state: StateFlow<ListUiState<SiteUser>> = _state.asStateFlow()
    private var loadedPath: String? = null

    fun load(name: String, followers: Boolean, force: Boolean = false) {
        if (name.isBlank()) return
        val path = "/@$name/" + if (followers) "followers" else "following"
        if (loadedPath == path && !force) return
        loadedPath = path
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { repo.usersAt(path, force) }
                .onSuccess { _state.value = ListUiState(items = it, loading = false) }
                .onFailure {
                    _state.value = ListUiState(loading = false, error = DeliErrors.explain(it))
                }
        }
    }
}

class BadgesViewModel(private val repo: DeliRepository) : ViewModel() {
    private val _state = MutableStateFlow(ListUiState<BadgeRow>())
    val state: StateFlow<ListUiState<BadgeRow>> = _state.asStateFlow()
    private var loaded = false

    fun load(force: Boolean = false) {
        if (loaded && !force) return
        loaded = true
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { repo.badges(force) }
                .onSuccess { _state.value = ListUiState(items = it, loading = false) }
                .onFailure { _state.value = ListUiState(loading = false, error = DeliErrors.explain(it)) }
        }
    }
}

class TableViewModel(private val repo: DeliRepository) : ViewModel() {
    private val _state = MutableStateFlow(ListUiState<SimpleRow>())
    val state: StateFlow<ListUiState<SimpleRow>> = _state.asStateFlow()
    private var loadedPath: String? = null

    fun load(path: String, force: Boolean = false) {
        if (loadedPath == path && !force && _state.value.items.isNotEmpty()) return
        loadedPath = path
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { repo.table(path, force) }
                .onSuccess { page ->
                    _state.value = ListUiState(
                        items = page.rows,
                        headers = page.headers,
                        loading = false,
                        note = page.note,
                    )
                }
                .onFailure { _state.value = ListUiState(loading = false, error = DeliErrors.explain(it)) }
        }
    }
}

class ProfileViewModel(private val repo: DeliRepository) : ViewModel() {
    private val _state = MutableStateFlow<Map<String, UserProfile?>>(emptyMap())
    val state: StateFlow<Map<String, UserProfile?>> = _state.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private var loadedName: String? = null

    /** True while a follow/mute/block request is in flight, so the buttons stay put. */
    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    /** The profile's other two tabs, kept per profile so switching back is instant. */
    private val _posts = MutableStateFlow<Map<String, List<FeedPost>>>(emptyMap())
    val posts: StateFlow<Map<String, List<FeedPost>>> = _posts.asStateFlow()

    private val _comments = MutableStateFlow<Map<String, List<UserComment>>>(emptyMap())
    val comments: StateFlow<Map<String, List<UserComment>>> = _comments.asStateFlow()

    private val _listLoading = MutableStateFlow(false)
    val listLoading: StateFlow<Boolean> = _listLoading.asStateFlow()

    fun consumeMessage() { _message.value = null }

    /** Loads `/@name/posts` once; a second visit uses what the first one brought. */
    fun loadPosts(name: String, force: Boolean = false) {
        if (!force && _posts.value.containsKey(name)) return
        _listLoading.value = true
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { repo.userPosts(name, force) }
                .onSuccess { list -> _posts.update { it + (name to list) } }
                .onFailure { e -> _message.value = DeliErrors.explain(e) }
            _listLoading.value = false
        }
    }

    fun loadComments(name: String, force: Boolean = false) {
        if (!force && _comments.value.containsKey(name)) return
        _listLoading.value = true
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { repo.userComments(name, force) }
                .onSuccess { list -> _comments.update { it + (name to list) } }
                .onFailure { e -> _message.value = DeliErrors.explain(e) }
            _listLoading.value = false
        }
    }

    /** The three things a profile page can do to another account, all in one place. */
    fun follow(name: String, follow: Boolean) {
        if (_busy.value) return
        _busy.value = true
        val prev = _state.value[name]
        if (prev != null) {
            val delta = if (follow) 1 else -1
            val newFollowers = ((prev.followers ?: 0) + delta).coerceAtLeast(0)
            _state.update { m -> m + (name to prev.copy(isFollowing = follow, followers = newFollowers)) }
        }
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { repo.followUser(name, follow) }
                .onSuccess { result ->
                    if (result.ok) {
                        _message.value = tr(if (follow) "Takip edildi." else "Takipten çıkıldı.")
                        load(name, force = true)
                    } else {
                        if (prev != null) _state.update { m -> m + (name to prev) }
                        _message.value = result.message ?: tr("İşlem tamamlanamadı.")
                    }
                }
                .onFailure {
                    if (prev != null) _state.update { m -> m + (name to prev) }
                    _message.value = DeliErrors.explain(it)
                }
            _busy.value = false
        }
    }

    fun mute(name: String, id: String, mute: Boolean) = act(
        name,
        tr(if (mute) "Bu hesabın bildirimleri sessize alındı." else "Bu hesabın bildirimleri açıldı."),
    ) {
        repo.muteUserNotifications(id, mute)
    }

    fun block(name: String, block: Boolean, onDone: (Boolean) -> Unit = {}) {
        if (_busy.value) return
        _busy.value = true
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { repo.blockUser(name, block) }
                .onSuccess { result ->
                    if (result.ok) {
                        _message.value = tr(if (block) "Bu hesabı engelledin." else "Engel kaldırıldı.")
                        load(name, force = true)
                        onDone(true)
                    } else {
                        _message.value = result.message ?: tr("Engel değiştirilemedi.")
                        onDone(false)
                    }
                }
                .onFailure {
                    _message.value = DeliErrors.explain(it)
                    onDone(false)
                }
            _busy.value = false
        }
    }

    private fun act(name: String, success: String, call: suspend () -> ActionResult) {
        if (_busy.value) return
        _busy.value = true
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { call() }
                .onSuccess { result ->
                    if (result.ok) {
                        _message.value = success
                        load(name, force = true)
                    } else {
                        _message.value = result.message ?: tr("İşlem tamamlanamadı.")
                    }
                }
                .onFailure { _message.value = DeliErrors.explain(it) }
            _busy.value = false
        }
    }

    fun load(name: String, force: Boolean = false) {
        if (loadedName == name && !force) return
        loadedName = name
        _loading.value = true
        _error.value = null
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { repo.profile(name, force) }
                .onSuccess { profile ->
                    _state.update { m -> m + (name to profile) }
                    _loading.value = false
                }
                .onFailure { e -> _error.value = DeliErrors.explain(e); _loading.value = false }
        }
    }
}

/* ------------------------------------------------------------------ */
/*  Notifications                                                      */
/* ------------------------------------------------------------------ */

data class NotificationsUiState(
    val page: NotificationPage = NotificationPage(),
    val loading: Boolean = false,
    val error: String? = null,
)

/** The site's `/notifications` page, read and cleared from the app. */
class NotificationsViewModel(private val repo: DeliRepository) : ViewModel() {

    private val _state = MutableStateFlow(NotificationsUiState())
    val state: StateFlow<NotificationsUiState> = _state.asStateFlow()

    private var started = false

    fun load(force: Boolean = false) {
        if (started && !force) return
        started = true
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { repo.notifications(force = force) }
                .onSuccess { page -> _state.value = NotificationsUiState(page = page) }
                .onFailure { e -> _state.update { it.copy(loading = false, error = DeliErrors.explain(e)) } }
        }
    }

    /** "Mark all notifications as read": the page's own button, without the reload. */
    fun markAllRead() {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { repo.clearNotifications() }
                .onSuccess { load(force = true) }
                .onFailure { e -> _state.update { it.copy(error = DeliErrors.explain(e)) } }
        }
    }

    /** The number the bell shows; fetched without opening the screen. */
    fun unread(onCount: (Int) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { repo.notifications(force = true) }.onSuccess { onCount(it.unreadCount) }
        }
    }
}

data class RandomUiState(val target: String? = null, val error: String? = null, val loading: Boolean = true)

/** /random_post and /random_user answer with a redirect to a concrete page. */
class RandomViewModel(private val repo: DeliRepository) : ViewModel() {
    private val _state = MutableStateFlow(RandomUiState())
    val state: StateFlow<RandomUiState> = _state.asStateFlow()
    private var started = false

    fun pick(kind: String, force: Boolean = false) {
        if (started && !force) return
        started = true
        _state.value = RandomUiState(loading = true)
        viewModelScope.launch(Dispatchers.IO) {
            val path = if (kind == "user") "/random_user" else "/random_post"
            runCatching { repo.resolveRedirect(path) }
                .onSuccess { _state.value = RandomUiState(target = it, loading = false) }
                .onFailure { _state.value = RandomUiState(error = DeliErrors.explain(it), loading = false) }
        }
    }

    fun reset() { started = false; _state.value = RandomUiState() }
}

/* ------------------------------------------------------------------ */
/*  Search                                                             */
/* ------------------------------------------------------------------ */

data class SearchUiState(
    val query: String = "",
    /** Which of the site's search tabs this is: `posts`, `comments` or `users`. */
    val kind: String = "posts",
    val results: List<SimpleRow> = emptyList(),
    val suggestions: List<String> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null,
    val searched: Boolean = false,
)

class SearchViewModel(private val repo: DeliRepository) : ViewModel() {
    private val _state = MutableStateFlow(SearchUiState())
    val state: StateFlow<SearchUiState> = _state.asStateFlow()

    fun onQueryChange(q: String) {
        _state.update { it.copy(query = q) }
        if (q.length >= 2) {
            viewModelScope.launch(Dispatchers.IO) {
                val s = runCatching { repo.searchSuggest(q) }.getOrDefault(emptyList())
                _state.update { it.copy(suggestions = s) }
            }
        } else {
            _state.update { it.copy(suggestions = emptyList()) }
        }
    }

    /** Switching tab re-runs the same query against that tab, the way the site does. */
    fun setKind(kind: String) {
        if (_state.value.kind == kind) return
        _state.update { it.copy(kind = kind, results = emptyList(), searched = false, error = null) }
        if (_state.value.query.isNotBlank()) search()
    }

    fun search(query: String = _state.value.query) {
        if (query.isBlank()) return
        _state.update { it.copy(loading = true, error = null, query = query, suggestions = emptyList()) }
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { repo.search(_state.value.kind, query) }
                .onSuccess { rows ->
                    _state.update { s -> s.copy(results = rows, loading = false, searched = true) }
                }
                .onFailure { e ->
                    _state.update { s -> s.copy(loading = false, error = DeliErrors.explain(e), searched = true) }
                }
        }
    }
}

/* ------------------------------------------------------------------ */
/*  Account settings (Personal + Security)                              */
/* ------------------------------------------------------------------ */

/* ------------------------------------------------------------------ */
/*  Contact                                                            */
/* ------------------------------------------------------------------ */

data class ContactUiState(
    val sending: Boolean = false,
    val sent: Boolean = false,
    val error: String? = null,
)

/** `/contact` is one textarea posted to the page itself. */
class ContactViewModel(private val repo: DeliRepository) : ViewModel() {
    private val _state = MutableStateFlow(ContactUiState())
    val state: StateFlow<ContactUiState> = _state.asStateFlow()

    fun send(message: String) {
        if (message.isBlank()) return
        _state.value = ContactUiState(sending = true)
        viewModelScope.launch(Dispatchers.IO) {
            var failure: Throwable? = null
            val res = runCatching { repo.contact(message.trim()) }
                .onFailure { failure = it }
                .getOrNull()
            _state.value = when {
                res?.ok == true -> ContactUiState(sent = true)
                else -> ContactUiState(
                    error = res?.message
                        ?: failure?.let { DeliErrors.explain(it) }
                        ?: "Mesaj gönderilemedi. Tekrar dene.",
                )
            }
        }
    }

    fun consume() { _state.value = ContactUiState() }
}

/* ------------------------------------------------------------------ */
/*  Account settings                                                   */
/* ------------------------------------------------------------------ */

data class PasswordDraft(val old: String = "", val new: String = "", val confirm: String = "")

data class EmailDraft(val password: String = "", val email: String = "")

/** Which tab of the settings screen is open. */
enum class SettingsTab(val key: String) {
    PERSONAL("personal"),
    SECURITY("security");

    companion object {
        fun of(key: String?): SettingsTab =
            entries.firstOrNull { it.key.equals(key, ignoreCase = true) } ?: PERSONAL
    }
}

data class AccountSettingsUiState(
    val personal: PersonalSettings = PersonalSettings(),
    val security: SecuritySettings = SecuritySettings(),
    val loading: Boolean = true,
    val error: String? = null,
    val saving: Boolean = false,
    val tab: SettingsTab = SettingsTab.PERSONAL,
    val message: String? = null,
    /** Set after a picture was accepted, so the app can refresh its own avatars. */
    val profileChanged: Boolean = false,
    // Editable copies, so "Kaydet" knows exactly what changed.
    val bio: String = "",
    val pronouns: String = "",
    val language: String = "",
    val theme: String = "",
    val themeColor: String = "",
    val password: PasswordDraft = PasswordDraft(),
    val email: EmailDraft = EmailDraft(),
)

/**
 * Drives the native settings screen.
 *
 * The site's own settings pages submit with JavaScript (their forms carry no `action`),
 * so instead of re-posting the page's markup this view model calls the same endpoints
 * the site's JavaScript calls, with the field names the live pages use.
 */
class AccountSettingsViewModel(private val repo: DeliRepository) : ViewModel() {

    private val _state = MutableStateFlow(AccountSettingsUiState())
    val state: StateFlow<AccountSettingsUiState> = _state.asStateFlow()

    fun load(force: Boolean = false) {
        if (!force && _state.value.message == null && _state.value.personal.languageOptions.isNotEmpty()) return
        _state.update { it.copy(loading = it.personal.languageOptions.isEmpty(), error = null) }
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { repo.accountSettings(force) }
                .onSuccess { s ->
                    _state.update {
                        it.copy(
                            loading = false,
                            error = null,
                            personal = s.personal,
                            security = s.security,
                            bio = s.personal.bio,
                            pronouns = s.personal.pronouns,
                            language = s.personal.language,
                            theme = s.personal.theme,
                            themeColor = s.personal.themeColor,
                            email = it.email.copy(email = s.security.email),
                        )
                    }
                }
                .onFailure { e ->
                    _state.update { it.copy(loading = false, error = DeliErrors.explain(e)) }
                }
        }
    }

    fun setTab(tab: SettingsTab) = _state.update { it.copy(tab = tab, message = null, error = null) }

    fun setBio(v: String) = _state.update { it.copy(bio = v, error = null, message = null) }
    fun setPronouns(v: String) = _state.update { it.copy(pronouns = v, error = null, message = null) }
    fun setTheme(v: String) = _state.update { it.copy(theme = v, error = null, message = null) }
    fun setThemeColor(v: String) = _state.update { it.copy(themeColor = v, error = null, message = null) }

    fun setPassword(old: String, new: String, confirm: String) =
        _state.update { it.copy(password = PasswordDraft(old, new, confirm), error = null, message = null) }

    fun setEmailPassword(v: String) = _state.update { it.copy(email = it.email.copy(password = v)) }
    fun setEmailAddress(v: String) = _state.update { it.copy(email = it.email.copy(email = v)) }

    /**
     * Saves the Personal tab in one go: bio + pronouns always, and the language/theme
     * only when they were actually changed, so one button behaves like the three the
     * website spreads across the page.
     */
    fun savePersonal() {
        val s = _state.value
        // Safety net: if the settings page never loaded, every field is still its
        // default, so "saving" would post an empty bio/pronouns over whatever the
        // account really has. The language `<select>` is rendered on every real
        // settings page, so an empty option list means we never read one.
        if (s.personal.languageOptions.isEmpty()) {
            _state.update {
                it.copy(
                    saving = false,
                    error = "Ayarlar okunamadı. Boş değerlerle hesabının üzerine yazmamak için kaydedilmedi.",
                )
            }
            return
        }
        _state.update { it.copy(saving = true, error = null, message = null) }
        viewModelScope.launch(Dispatchers.IO) {
            val steps = mutableListOf<suspend () -> ActionResult>()
            if (s.bio != s.personal.bio || s.pronouns != s.personal.pronouns) {
                steps += { repo.savePersonalSettings(s.bio, s.pronouns) }
            }
            // The language is not saved from this screen: it is the app's own setting, picked
            // once in Ayarlar, and pushed to the site from there (see AppViewModel.setLang).
            if (s.theme != s.personal.theme || s.themeColor != s.personal.themeColor) {
                steps += { repo.saveTheme(s.theme, s.themeColor) }
            }
            finish(steps, if (steps.isEmpty()) tr("Değişiklik yok.") else tr("Kaydedildi."))
        }
    }

    fun savePassword() {
        val p = _state.value.password
        when {
            p.old.isBlank() || p.new.isBlank() -> {
                _state.update { it.copy(error = "Mevcut ve yeni şifre gerekli.") }
                return
            }
            p.new.length < 8 -> {
                _state.update { it.copy(error = "Yeni şifre en az 8 karakter olmalı.") }
                return
            }
            p.new != p.confirm -> {
                _state.update { it.copy(error = "Yeni şifreler birbiriyle uyuşmuyor.") }
                return
            }
        }
        _state.update { it.copy(saving = true, error = null, message = null) }
        viewModelScope.launch(Dispatchers.IO) {
            finish(
                listOf<suspend () -> ActionResult>({ repo.changePassword(p.old, p.new, p.confirm) }),
                "Şifre değiştirildi.",
                onDone = { _state.update { it.copy(password = PasswordDraft()) } },
            )
        }
    }

    fun saveEmail() {
        val e = _state.value.email
        if (e.email.isBlank() || e.password.isBlank()) {
            _state.update { it.copy(error = "Yeni e-posta ve şifre gerekli.") }
            return
        }
        _state.update { it.copy(saving = true, error = null, message = null) }
        viewModelScope.launch(Dispatchers.IO) {
            finish(
                listOf<suspend () -> ActionResult>({ repo.changeEmail(e.password, e.email.trim()) }),
                "E-posta güncellendi.",
                onDone = { _state.update { it.copy(email = it.email.copy(password = "")) } },
            )
        }
    }

    /** Profile picture / banner upload — the file is picked and prepared by the screen. */
    fun uploadImage(kind: String, fileName: String, contentType: String, bytes: ByteArray) {
        _state.update { it.copy(saving = true, error = null, message = null, profileChanged = false) }
        viewModelScope.launch(Dispatchers.IO) {
            val call: suspend () -> ActionResult = {
                if (kind == "banner") repo.uploadBanner(fileName, contentType, bytes)
                else repo.uploadProfileImage(fileName, contentType, bytes)
            }
            finish(
                listOf(call),
                if (kind == "banner") "Banner güncellendi." else "Profil fotoğrafı güncellendi.",
                // The picture the rest of the app shows comes from the session snapshot,
                // so it has to be taken again once the server has accepted a new one.
                onDone = { _state.update { it.copy(profileChanged = true) } },
            )
        }
    }

    /** Says something went wrong before anything was even sent (an unreadable file). */
    fun report(message: String) =
        _state.update { it.copy(saving = false, error = message, message = null) }

    fun consumeProfileChange() = _state.update { it.copy(profileChanged = false) }

    /** Runs the steps in order, stopping at the first failure. */
    private suspend fun finish(
        steps: List<suspend () -> ActionResult>,
        successMessage: String,
        onDone: () -> Unit = {},
    ) {
        var failure: Throwable? = null
        var result: ActionResult? = null
        for (step in steps) {
            result = runCatching { step() }.onFailure { failure = it }.getOrNull()
            if (result?.ok != true) break
        }
        val reason = result?.message
            ?: failure?.let { DeliErrors.explain(it) }
        if (result == null) {
            _state.update { it.copy(saving = false, error = reason ?: "Kaydedilemedi.") }
            return
        }
        if (result.ok) {
            onDone()
            _state.update { it.copy(saving = false, message = successMessage) }
            load(force = true)
        } else {
            _state.update {
                it.copy(saving = false, error = reason ?: "Kaydedilemedi. Tekrar dene.")
            }
        }
    }

    fun consume() = _state.update { it.copy(error = null, message = null) }
}

/* ------------------------------------------------------------------ */
/*  Auth                                                               */
/* ------------------------------------------------------------------ */

data class AuthUiState(
    val busy: Boolean = false,
    val error: String? = null,
    val done: Boolean = false,
)

class AuthViewModel(private val repo: DeliRepository) : ViewModel() {
    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    fun login(username: String, password: String, onSuccess: () -> Unit) {
        if (username.isBlank() || password.isBlank()) {
            _state.value = AuthUiState(error = "Kullanıcı adı ve şifre gerekli.")
            return
        }
        _state.value = AuthUiState(busy = true)
        viewModelScope.launch(Dispatchers.IO) {
            when (val res = runCatching { repo.login(username.trim(), password) }
                .getOrElse { LoginResult.Failure(DeliErrors.explain(it)) }) {
                is LoginResult.Success -> {
                    repo.bootstrap()
                    _state.value = AuthUiState(done = true)
                    onSuccess()
                }
                is LoginResult.Failure -> _state.value = AuthUiState(error = res.message)
            }
        }
    }

    fun consume() { _state.value = AuthUiState() }
}

/* ------------------------------------------------------------------ */
/*  Submit                                                             */
/* ------------------------------------------------------------------ */

data class SubmitUiState(
    val form: DiscoveredForm? = null,
    val loading: Boolean = true,
    val sending: Boolean = false,
    val error: String? = null,
    val done: Boolean = false,
    val message: String? = null,
)

class SubmitViewModel(private val repo: DeliRepository) : ViewModel() {
    private val _state = MutableStateFlow(SubmitUiState())
    val state: StateFlow<SubmitUiState> = _state.asStateFlow()

    private var loaded = false

    fun load(force: Boolean = false) {
        if (loaded && !force) return
        loaded = true
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { repo.submitForm() }
                .onSuccess { _state.value = SubmitUiState(form = it, loading = false) }
                .onFailure { _state.value = SubmitUiState(loading = false, error = DeliErrors.explain(it)) }
        }
    }

    fun submit(
        title: String,
        body: String,
        hole: String,
        nsfw: Boolean,
        anonymous: Boolean,
        tripcodeSecret: String?,
    ) {
        if (title.isBlank() || hole.isBlank()) {
            _state.update { it.copy(error = "Başlık ve topluluk zorunlu.") }
            return
        }
        if (anonymous && tripcodeSecret.isNullOrBlank()) {
            _state.update { it.copy(error = "Anonim gönderi için tripcode gizli değeri gerekli.") }
            return
        }
        _state.update { it.copy(sending = true, error = null, message = null) }
        viewModelScope.launch(Dispatchers.IO) {
            var failure: Throwable? = null
            val res = runCatching {
                repo.submitPost(_state.value.form, title, body, hole, nsfw, anonymous, tripcodeSecret)
            }.onFailure { failure = it }.getOrNull()
            if (res?.ok == true) {
                _state.update { it.copy(sending = false, done = true, message = tr("Gönderi paylaşıldı.")) }
            } else {
                val reason = res?.message
                    ?: failure?.let { DeliErrors.explain(it) }
                    ?: "Gönderi paylaşılamadı. Bağlantını kontrol edip tekrar dene."
                _state.update { it.copy(sending = false, error = reason) }
            }
        }
    }

    fun consume() = _state.update { it.copy(message = null, error = null) }
}
