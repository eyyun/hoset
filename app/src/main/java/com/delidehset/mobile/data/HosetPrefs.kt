package com.delidehset.mobile.data

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import com.delidehset.mobile.R

enum class LauncherVariant(
    val id: String,
    val componentClass: String,
    val label: String,
    val subtitle: String,
) {
    DEFAULT("default", "com.delidehset.mobile.MainActivity", "Hoset (Beyaz)", "Varsayılan beyaz simge"),
    DARK("dark", "com.delidehset.mobile.MainActivityDark", "Hoset (Koyu)", "Gece zeminli simge"),
    PURPLE("purple", "com.delidehset.mobile.MainActivityPurple", "Hoset (Mor)", "Vurgu renkli simge"),
    DELI("deli", "com.delidehset.mobile.MainActivityDeli", "Delidehşet", "Site adıyla koyu simge"),
    CAT("cat", "com.delidehset.mobile.MainActivityCat", "Delidehşet (Kedi)", "Yeşil kedi simgesi"),
    DD("dd", "com.delidehset.mobile.MainActivityDD", "DD", "Kısa ad ve mor simge"),
}

/**
 * Local install preferences that belong to Hoset itself: custom app header name,
 * launcher icon/label alias, font scale, default feed sort, NSFW blur, and auto-update check.
 */
object HosetPrefs {
    private const val PREFS_NAME = "hoset.prefs"
    private const val KEY_APP_TITLE = "app_title"
    private const val KEY_LAUNCHER = "launcher_variant"
    private const val KEY_FONT_SCALE = "font_scale"
    private const val KEY_DEFAULT_SORT = "default_sort"
    private const val KEY_HIDE_NSFW_MEDIA = "hide_nsfw_media"
    private const val KEY_COMPACT_FEED = "compact_feed"
    private const val KEY_AUTO_UPDATE = "auto_update"
    private const val KEY_LAST_VERSION = "last_version"
    private const val KEY_RULES_ACCEPTED = "rules_accepted"
    private const val KEY_RULES_TEXT = "rules_text"
    private const val KEY_EXPERIMENTAL = "experimental"
    private const val KEY_REDUCE_MOTION = "reduce_motion"
    private const val KEY_GHOST_TYPING = "mod_ghost_typing"
    private const val KEY_STEALTH_ONLINE = "mod_stealth_online"
    private const val KEY_SHOW_HIDDEN_INFO = "mod_show_hidden_info"
    private const val KEY_DOUBLE_TAP_VOTE = "mod_double_tap_vote"
    private const val KEY_MEDIA_DOWNLOAD = "mod_media_download"
    private const val KEY_DIM_SEEN_POSTS = "mod_dim_seen_posts"
    private const val KEY_COPY_LONG_PRESS = "mod_copy_long_press"
    private const val KEY_SEEN_POSTS = "mod_seen_posts"

    val appTitleState = mutableStateOf("Hoset")
    val fontScaleState = mutableFloatStateOf(1.0f)
    val hideNsfwMediaState = mutableStateOf(false)
    val compactFeedState = mutableStateOf(false)
    val ghostTypingState = mutableStateOf(false)
    val stealthOnlineState = mutableStateOf(false)
    val showHiddenInfoState = mutableStateOf(true)
    val doubleTapVoteState = mutableStateOf(true)
    val mediaDownloadState = mutableStateOf(true)
    val dimSeenPostsState = mutableStateOf(false)
    val copyOnLongPressState = mutableStateOf(true)
    val seenPostsState = mutableStateOf<Set<String>>(emptySet())

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun init(context: Context) {
        val p = prefs(context)
        appTitleState.value = p.getString(KEY_APP_TITLE, "Hoset")?.takeIf { it.isNotBlank() } ?: "Hoset"
        fontScaleState.floatValue = p.getFloat(KEY_FONT_SCALE, 1.0f).coerceIn(0.85f, 1.25f)
        hideNsfwMediaState.value = p.getBoolean(KEY_HIDE_NSFW_MEDIA, false)
        compactFeedState.value = p.getBoolean(KEY_COMPACT_FEED, false)
        ghostTypingState.value = p.getBoolean(KEY_GHOST_TYPING, false)
        stealthOnlineState.value = p.getBoolean(KEY_STEALTH_ONLINE, false)
        showHiddenInfoState.value = p.getBoolean(KEY_SHOW_HIDDEN_INFO, true)
        doubleTapVoteState.value = p.getBoolean(KEY_DOUBLE_TAP_VOTE, true)
        mediaDownloadState.value = p.getBoolean(KEY_MEDIA_DOWNLOAD, true)
        dimSeenPostsState.value = p.getBoolean(KEY_DIM_SEEN_POSTS, false)
        copyOnLongPressState.value = p.getBoolean(KEY_COPY_LONG_PRESS, true)
        seenPostsState.value = p.getStringSet(KEY_SEEN_POSTS, emptySet())?.toSet().orEmpty()
    }

    fun ghostTyping(context: Context): Boolean =
        prefs(context).getBoolean(KEY_GHOST_TYPING, false)

    fun setGhostTyping(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_GHOST_TYPING, enabled).apply()
        ghostTypingState.value = enabled
    }

    fun stealthOnline(context: Context): Boolean =
        prefs(context).getBoolean(KEY_STEALTH_ONLINE, false)

    fun setStealthOnline(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_STEALTH_ONLINE, enabled).apply()
        stealthOnlineState.value = enabled
    }

    fun showHiddenInfo(context: Context): Boolean =
        prefs(context).getBoolean(KEY_SHOW_HIDDEN_INFO, true)

    fun setShowHiddenInfo(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_SHOW_HIDDEN_INFO, enabled).apply()
        showHiddenInfoState.value = enabled
    }

    fun doubleTapVote(context: Context): Boolean =
        prefs(context).getBoolean(KEY_DOUBLE_TAP_VOTE, true)

    fun setDoubleTapVote(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_DOUBLE_TAP_VOTE, enabled).apply()
        doubleTapVoteState.value = enabled
    }

    fun mediaDownload(context: Context): Boolean =
        prefs(context).getBoolean(KEY_MEDIA_DOWNLOAD, true)

    fun setMediaDownload(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_MEDIA_DOWNLOAD, enabled).apply()
        mediaDownloadState.value = enabled
    }

    fun dimSeenPosts(context: Context): Boolean =
        prefs(context).getBoolean(KEY_DIM_SEEN_POSTS, false)

    fun setDimSeenPosts(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_DIM_SEEN_POSTS, enabled).apply()
        dimSeenPostsState.value = enabled
    }

    fun copyOnLongPress(context: Context): Boolean =
        prefs(context).getBoolean(KEY_COPY_LONG_PRESS, true)

    fun setCopyOnLongPress(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_COPY_LONG_PRESS, enabled).apply()
        copyOnLongPressState.value = enabled
    }

    fun markPostSeen(context: Context, key: String) {
        val clean = key.trim().ifBlank { return }
        if (clean in seenPostsState.value) return
        val next = (seenPostsState.value + clean).toList().takeLast(500).toSet()
        seenPostsState.value = next
        prefs(context).edit().putStringSet(KEY_SEEN_POSTS, next).apply()
    }

    fun downloadMedia(context: Context, rawUrl: String): Boolean {
        val fullUrl = when {
            rawUrl.startsWith("http://") || rawUrl.startsWith("https://") -> rawUrl
            rawUrl.startsWith("/") -> DeliHttp.BASE_URL + rawUrl
            else -> "${DeliHttp.BASE_URL}/$rawUrl"
        }
        return runCatching {
            val uri = android.net.Uri.parse(fullUrl)
            val ext = uri.lastPathSegment?.substringAfterLast('.', "")
                ?.takeIf { it.length in 2..5 } ?: "webp"
            val fileName = "hoset_${System.currentTimeMillis()}.$ext"
            val req = android.app.DownloadManager.Request(uri)
                .setTitle(fileName)
                .setNotificationVisibility(android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(android.os.Environment.DIRECTORY_DOWNLOADS, fileName)
            val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as? android.app.DownloadManager
            dm?.enqueue(req) != null
        }.getOrDefault(false)
    }

    fun appTitle(context: Context): String =
        prefs(context).getString(KEY_APP_TITLE, "Hoset")?.takeIf { it.isNotBlank() } ?: "Hoset"

    fun setAppTitle(context: Context, title: String) {
        val clean = title.trim().ifBlank { "Hoset" }.take(24)
        prefs(context).edit().putString(KEY_APP_TITLE, clean).apply()
        appTitleState.value = clean
    }

    fun fontScale(context: Context): Float =
        prefs(context).getFloat(KEY_FONT_SCALE, 1.0f).coerceIn(0.85f, 1.25f)

    fun setFontScale(context: Context, scale: Float) {
        val clamped = scale.coerceIn(0.85f, 1.25f)
        prefs(context).edit().putFloat(KEY_FONT_SCALE, clamped).apply()
        fontScaleState.floatValue = clamped
    }

    fun defaultSort(context: Context): FeedSort =
        FeedSort.fromKey(prefs(context).getString(KEY_DEFAULT_SORT, FeedSort.HOT.key))

    fun setDefaultSort(context: Context, sort: FeedSort) {
        prefs(context).edit().putString(KEY_DEFAULT_SORT, sort.key).apply()
    }

    fun hideNsfwMedia(context: Context): Boolean =
        prefs(context).getBoolean(KEY_HIDE_NSFW_MEDIA, false)

    fun setHideNsfwMedia(context: Context, hide: Boolean) {
        prefs(context).edit().putBoolean(KEY_HIDE_NSFW_MEDIA, hide).apply()
        hideNsfwMediaState.value = hide
    }

    fun compactFeed(context: Context): Boolean =
        prefs(context).getBoolean(KEY_COMPACT_FEED, false)

    fun setCompactFeed(context: Context, compact: Boolean) {
        prefs(context).edit().putBoolean(KEY_COMPACT_FEED, compact).apply()
        compactFeedState.value = compact
    }

    /**
     * The version this install last ran.
     *
     * Null means the app has never run here — a fresh install, which is told nothing about
     * what changed, because there is no previous version on this phone for it to have
     * changed from.
     */
    fun lastVersion(context: Context): String? =
        prefs(context).getString(KEY_LAST_VERSION, null)?.takeIf { it.isNotBlank() }

    fun setLastVersion(context: Context, version: String) {
        prefs(context).edit().putString(KEY_LAST_VERSION, version).apply()
    }

    /**
     * Whether the site's rules have been read on this install.
     *
     * A plain flag, not a version: the rules are shown once and then never again, which is
     * what was asked for. The text itself is kept too, so the screen still opens them
     * when the site cannot be reached.
     */
    fun rulesAccepted(context: Context): Boolean =
        prefs(context).getBoolean(KEY_RULES_ACCEPTED, false)

    fun setRulesAccepted(context: Context, accepted: Boolean) {
        prefs(context).edit().putBoolean(KEY_RULES_ACCEPTED, accepted).apply()
    }

    fun rulesText(context: Context): String? =
        prefs(context).getString(KEY_RULES_TEXT, null)?.takeIf { it.isNotBlank() }

    fun setRulesText(context: Context, text: String) {
        prefs(context).edit().putString(KEY_RULES_TEXT, text).apply()
    }

    /**
     * Whether this install follows the experimental channel.
     *
     * Off by default. An install with it on is offered pre-releases and the builds that
     * carry unfinished features; an install with it off only ever sees a stable release,
     * which is what `releases/latest` returns.
     */
    fun experimental(context: Context): Boolean =
        prefs(context).getBoolean(KEY_EXPERIMENTAL, false)

    fun setExperimental(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_EXPERIMENTAL, enabled).apply()
    }

    /** Turns the app's animations off, for anyone who would rather have less movement. */
    fun reduceMotion(context: Context): Boolean =
        prefs(context).getBoolean(KEY_REDUCE_MOTION, false)

    fun setReduceMotion(context: Context, reduce: Boolean) {
        prefs(context).edit().putBoolean(KEY_REDUCE_MOTION, reduce).apply()
    }

    fun autoUpdate(context: Context): Boolean =
        prefs(context).getBoolean(KEY_AUTO_UPDATE, true)

    fun setAutoUpdate(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_AUTO_UPDATE, enabled).apply()
    }

    fun launcherVariant(context: Context): LauncherVariant {
        val id = prefs(context).getString(KEY_LAUNCHER, LauncherVariant.DEFAULT.id)
        return LauncherVariant.entries.firstOrNull { it.id == id } ?: LauncherVariant.DEFAULT
    }

    fun setLauncherVariant(context: Context, variant: LauncherVariant) {
        prefs(context).edit().putString(KEY_LAUNCHER, variant.id).apply()
        val pm = context.applicationContext.packageManager
        val pkg = context.applicationContext.packageName
        runCatching {
            // Enable the chosen entry first so the launcher never sees zero enabled entrypoints.
            pm.setComponentEnabledSetting(
                ComponentName(pkg, variant.componentClass),
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP,
            )
            for (other in LauncherVariant.entries) {
                if (other == variant) continue
                pm.setComponentEnabledSetting(
                    ComponentName(pkg, other.componentClass),
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP,
                )
            }
        }
    }
}
