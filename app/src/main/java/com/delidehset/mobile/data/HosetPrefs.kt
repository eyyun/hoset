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

    val appTitleState = mutableStateOf("Hoset")
    val fontScaleState = mutableFloatStateOf(1.0f)
    val hideNsfwMediaState = mutableStateOf(false)
    val compactFeedState = mutableStateOf(false)

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun init(context: Context) {
        val p = prefs(context)
        appTitleState.value = p.getString(KEY_APP_TITLE, "Hoset")?.takeIf { it.isNotBlank() } ?: "Hoset"
        fontScaleState.floatValue = p.getFloat(KEY_FONT_SCALE, 1.0f).coerceIn(0.85f, 1.25f)
        hideNsfwMediaState.value = p.getBoolean(KEY_HIDE_NSFW_MEDIA, false)
        compactFeedState.value = p.getBoolean(KEY_COMPACT_FEED, false)
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
