package com.delidehset.mobile.ui.nav

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.ContactMail
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Hexagon
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Verified
import androidx.compose.ui.graphics.vector.ImageVector

object Routes {
    const val FEED = "feed"

    /** Effortpostlar is its own destination; a bare query string would not navigate. */
    const val FEED_EFFORT = "feed/effort"
    const val POST = "post"
    const val HOLES = "holes"
    const val SEARCH = "search"
    const val SUBMIT = "submit"

    /** [SUBMIT] with the community the composer should start on. */
    const val SUBMIT_PATTERN = "submit?hole={hole}"

    fun submit(hole: String? = null) = if (hole.isNullOrBlank()) SUBMIT else "$SUBMIT?hole=$hole"
    const val LOGIN = "login"
    const val USER = "user"
    const val PAGE = "page"
    const val SETTINGS = "settings"

    /** Where the app's kits are shown, each one previewed as itself. */
    const val THEMES = "themes"

    /**
     * Hoset's own settings — look, updates, licence — as opposed to the account settings
     * the site owns. Kept beside [SETTINGS] because that is where it is reached from.
     */
    const val HOSET_SETTINGS = "hoset-settings"
    const val ACCOUNT = "account"
    const val RANDOM = "random"
    const val CONTACT = "contact"
    const val WEB = "web"

    /** The site's own notification list, drawn natively. */
    const val NOTIFICATIONS = "notifications"

    fun feed(hole: String? = null) = if (hole.isNullOrBlank()) FEED else "$FEED?hole=$hole"

    /**
     * Permalinks are already URL-safe slugs, so they are passed through verbatim.
     * Encoding them here would double-encode once Navigation decodes the argument.
     */
    fun post(permalink: String) = "$POST?permalink=$permalink"
    fun user(name: String) = "$USER?name=$name"
    fun page(kind: String) = "$PAGE?kind=$kind"

    /** The navigation pattern for [page]; used to build the "current screen" key. */
    const val PAGE_PATTERN = "page?kind={kind}"

    /** Settings tabs the site offers: "personal" and "security". */
    fun account(tab: String = "personal") = "$ACCOUNT?tab=$tab"

    /**
     * Any site page, hosted in the app's own web frame. Used as the fallback for
     * prototype pages the app has no native reader for, so nothing is a dead end.
     */
    fun web(path: String, title: String) =
        "$WEB?path=${android.net.Uri.encode(path)}&title=${android.net.Uri.encode(title)}"
}

/** Mirrors the website's own left sidebar, structured cleanly into categories. */
data class DrawerItem(
    val label: String,
    val route: String,
    val icon: ImageVector,
    val signedInOnly: Boolean = false,
    val adminOnly: Boolean = false,
    val badge: String? = null,
)

data class DrawerSection(
    val title: String,
    val items: List<DrawerItem>,
)

val drawerSections: List<DrawerSection> = listOf(
    DrawerSection(
        title = "Akış & İçerik",
        items = listOf(
            DrawerItem("Home", Routes.FEED, Icons.Filled.Home),
            DrawerItem("Topluluklar", Routes.HOLES, Icons.Filled.Groups),
            DrawerItem("Sohbet", Routes.page("chat"), Icons.Filled.Forum, signedInOnly = true),
            DrawerItem("Effortpostlar", Routes.FEED_EFFORT, Icons.Filled.Verified),
            DrawerItem("Gönderi oluştur", Routes.SUBMIT, Icons.Filled.Edit),
        ),
    ),
    DrawerSection(
        title = "Topluluk & İstatistik",
        items = listOf(
            DrawerItem("Kullanıcılar", Routes.page("users"), Icons.Filled.Person),
            DrawerItem("Rozetler", Routes.page("badges"), Icons.Filled.Hexagon),
            DrawerItem("Leaderboard", Routes.page("leaderboard"), Icons.Filled.EmojiEvents, signedInOnly = true),
            DrawerItem("İstatistikler", Routes.page("stats"), Icons.Filled.QueryStats, signedInOnly = true),
        ),
    ),
    DrawerSection(
        title = "Keşfet",
        items = listOf(
            DrawerItem("Rastgele gönderi", Routes.RANDOM + "?kind=post", Icons.Filled.Shuffle),
            DrawerItem("Rastgele kullanıcı", Routes.RANDOM + "?kind=user", Icons.Filled.Casino),
        ),
    ),
    DrawerSection(
        title = "Yönetim & Destek",
        items = listOf(
            DrawerItem("Yönetim paneli", Routes.page("admin"), Icons.Filled.AdminPanelSettings, signedInOnly = true, adminOnly = true),
            DrawerItem("Yöneticiler", Routes.page("admins"), Icons.Filled.AdminPanelSettings),
            DrawerItem("Moderasyon kaydı", Routes.page("comments"), Icons.Filled.History),
            DrawerItem("Bize ulaşın", Routes.CONTACT, Icons.Filled.ContactMail),
            DrawerItem("Tema Mağazası", Routes.THEMES, Icons.Filled.Palette),
            DrawerItem("Ayarlar", Routes.SETTINGS, Icons.Filled.Settings),
        ),
    ),
)

val drawerItems: List<DrawerItem> = drawerSections.flatMap { it.items }


val searchIcon: ImageVector = Icons.Filled.Search
