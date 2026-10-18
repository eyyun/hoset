package com.delidehset.mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import com.delidehset.mobile.ui.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.delidehset.mobile.ui.theme.DeliPalettes
import com.delidehset.mobile.ui.theme.DeliShapes
import com.delidehset.mobile.ui.theme.Palette
import coil.compose.AsyncImage
import com.delidehset.mobile.data.BadgeRow
import com.delidehset.mobile.data.DeliHttp
import com.delidehset.mobile.data.SimpleRow
import com.delidehset.mobile.data.SiteUser
import com.delidehset.mobile.data.Viewer
import com.delidehset.mobile.ui.AppLang
import com.delidehset.mobile.ui.components.DeliButtonPrimary
import com.delidehset.mobile.ui.components.DeliAvatar
import com.delidehset.mobile.ui.components.DeliCard
import com.delidehset.mobile.ui.components.DeliOutlinedButton
import com.delidehset.mobile.ui.components.EmptyBox
import com.delidehset.mobile.ui.components.ErrorBox
import com.delidehset.mobile.ui.components.LoadingBox
import com.delidehset.mobile.ui.components.MutedText
import com.delidehset.mobile.ui.fill
import com.delidehset.mobile.ui.t
import com.delidehset.mobile.ui.tf
import com.delidehset.mobile.ui.components.pullUp
import com.delidehset.mobile.ui.theme.DeliColors
import com.delidehset.mobile.vm.BadgesViewModel
import com.delidehset.mobile.vm.RandomViewModel
import com.delidehset.mobile.vm.RelationUsersViewModel
import com.delidehset.mobile.vm.TableViewModel
import com.delidehset.mobile.vm.UsersViewModel
import com.delidehset.mobile.ui.theme.kitSurface

/* ------------------------------------------------------------------ */
/*  Generic table page: admins, moderation log, leaderboard, stats     */
/* ------------------------------------------------------------------ */

@Composable
fun PageScreen(
    kind: String,
    contentPadding: PaddingValues,
    onOpenPost: (String) -> Unit,
    onOpenInApp: (path: String, title: String) -> Unit,
) {
    val vm = com.delidehset.mobile.ui.deliViewModel { com.delidehset.mobile.vm.TableViewModel(it.repository) }
    val state by vm.state.collectAsStateWithLifecycle()
    val path = pagePathFor(kind)

    LaunchedEffect(kind) { vm.load(path) }

    when {
        state.loading -> LoadingBox(Modifier.fillMaxSize())

        state.error != null -> ErrorBox(state.error!!, Modifier.fillMaxSize(), onRetry = { vm.load(path, true) })

        // Several prototype pages are empty on the site as well, so the server's own
        // explanation is shown, along with a way to see the page itself.
        state.items.isEmpty() -> EmptyPage(
            title = pageTitleFor(kind),
            note = state.note ?: "Bu sayfada listelenecek bir şey yok.",
            onOpenInApp = { onOpenInApp(path, pageTitleFor(kind)) },
        )

        else -> LazyColumn(
            modifier = Modifier.fillMaxSize().background(DeliColors.Background),
            contentPadding = PaddingValues(
                start = 12.dp,
                end = 12.dp,
                top = 12.dp,
                bottom = contentPadding.calculateBottomPadding() + 20.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            item {
                Text(
                    pageTitleFor(kind),
                    color = DeliColors.OnBackground,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }
            items(state.items) { row: SimpleRow ->
                DeliCard(onClick = row.href?.takeIf { it.contains("/post/") }?.let { { onOpenPost(it) } }) {
                    if (state.headers.isNotEmpty()) {
                        Text(
                            text = row.cells.firstOrNull().orEmpty(),
                            color = DeliColors.OnBackground,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                        )
                        val rest = row.cells.drop(1).filter { it.isNotBlank() }
                        if (rest.isNotEmpty()) {
                            Spacer(Modifier.height(3.dp))
                            MutedText(rest.joinToString("  ·  "))
                        }
                    } else {
                        Text(row.cells.joinToString(" "), color = DeliColors.OnBackground, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

private fun pagePathFor(kind: String): String = when (kind) {
    "chat" -> "/chat/1"
    "admins" -> "/admins"
    // The site labels /comments "Moderation Log" in its own sidebar.
    "comments", "modlog" -> "/comments"
    "leaderboard" -> "/leaderboard"
    "stats" -> "/stats"
    else -> "/$kind"
}

/** Empty state for a page the site itself renders without any list. */
@Composable
private fun EmptyPage(title: String, note: String, onOpenInApp: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeliColors.Background)
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(title, color = DeliColors.OnBackground, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        MutedText(note)
        Spacer(Modifier.height(18.dp))
        DeliOutlinedButton("Sayfayı uygulamada aç") { onOpenInApp() }
    }
}

private fun pageTitleFor(kind: String): String = when (kind) {
    "chat" -> "Sohbet"
    "admin" -> "Yönetim paneli"
    "admins" -> "Yöneticiler"
    "comments", "modlog" -> "Moderasyon kaydı"
    "leaderboard" -> "Leaderboard"
    "stats" -> "İstatistikler"
    else -> kind
}

/* ------------------------------------------------------------------ */
/*  Users                                                              */
/* ------------------------------------------------------------------ */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsersScreen(
    contentPadding: PaddingValues,
    onOpenUser: (String) -> Unit,
) {
    val vm = com.delidehset.mobile.ui.deliViewModel { UsersViewModel(it.repository) }
    val state by vm.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { vm.load() }

    when {
        state.loading -> LoadingBox(Modifier.fillMaxSize())
        state.error != null -> ErrorBox(state.error!!, Modifier.fillMaxSize(), onRetry = { vm.load(true) })
        state.items.isEmpty() -> EmptyBox("Kullanıcı yok.", Modifier.fillMaxSize())
        // Pull to refresh, like the feed and the profile: the table is read again instead of
        // only once when the screen was first opened.
        else -> PullToRefreshBox(
            isRefreshing = state.loading,
            onRefresh = { vm.load(true) },
            modifier = Modifier.fillMaxSize().background(DeliColors.Background),
        ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 12.dp,
                end = 12.dp,
                top = 12.dp,
                bottom = contentPadding.calculateBottomPadding() + 20.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            item {
                Text(
                    "Kayıtlı kullanıcılar",
                    color = DeliColors.OnBackground,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            items(state.items) { user: SiteUser ->
                SiteUserRow(user = user, onOpenUser = onOpenUser)
            }
        }
        }
    }
}

/** One row of the site's user table — `/users`, a profile's followers, who it follows. */
@Composable
private fun SiteUserRow(user: SiteUser, onOpenUser: (String) -> Unit) {
    DeliCard(onClick = { onOpenUser(user.name) }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            DeliAvatar(url = null, size = 34.dp, name = user.name)
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "@${user.name}",
                    color = DeliColors.Blue,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                )
                user.joined?.let { MutedText(tf("katıldı: {0}", it)) }
            }
            Column(horizontalAlignment = Alignment.End) {
                MutedText(tf("{0} gönderi", user.posts ?: "0"))
                MutedText(tf("{0} yorum", user.comments ?: "0"))
            }
        }
    }
}

/**
 * A profile's followers, or the accounts it follows.
 *
 * Both used to open in the app's web frame. The site answers them with the same table
 * `/users` does, so the app draws them itself now and the person stays in the app.
 */
@Composable
fun RelationsScreen(
    name: String,
    followers: Boolean,
    contentPadding: PaddingValues,
    onOpenUser: (String) -> Unit,
) {
    val vm = com.delidehset.mobile.ui.deliViewModel("relations-$name-$followers") {
        RelationUsersViewModel(it.repository)
    }
    val state by vm.state.collectAsStateWithLifecycle()
    LaunchedEffect(name, followers) { vm.load(name, followers) }

    val title = if (followers) t("Takipçi") else t("Takip")

    when {
        state.loading -> LoadingBox(Modifier.fillMaxSize())
        state.error != null -> ErrorBox(
            state.error!!,
            Modifier.fillMaxSize(),
            onRetry = { vm.load(name, followers, true) },
        )
        state.items.isEmpty() -> EmptyBox("Kullanıcı yok.", Modifier.fillMaxSize())
        else -> LazyColumn(
            modifier = Modifier.fillMaxSize().background(DeliColors.Background),
            contentPadding = PaddingValues(
                start = 12.dp,
                end = 12.dp,
                top = 12.dp,
                bottom = contentPadding.calculateBottomPadding() + 20.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            item {
                Text(
                    "@$name · $title",
                    color = DeliColors.OnBackground,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            items(state.items) { user: SiteUser ->
                SiteUserRow(user = user, onOpenUser = onOpenUser)
            }
        }
    }
}

/* ------------------------------------------------------------------ */
/*  Badges                                                             */
/* ------------------------------------------------------------------ */

@Composable
fun BadgesScreen(contentPadding: PaddingValues) {
    val vm = com.delidehset.mobile.ui.deliViewModel { BadgesViewModel(it.repository) }
    val state by vm.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { vm.load() }

    when {
        state.loading -> LoadingBox(Modifier.fillMaxSize())
        state.error != null -> ErrorBox(state.error!!, Modifier.fillMaxSize(), onRetry = { vm.load(true) })
        state.items.isEmpty() -> EmptyBox("Rozet bulunamadı.", Modifier.fillMaxSize())
        else -> LazyColumn(
            modifier = Modifier.fillMaxSize().background(DeliColors.Background),
            contentPadding = PaddingValues(
                start = 12.dp,
                end = 12.dp,
                top = 12.dp,
                bottom = contentPadding.calculateBottomPadding() + 20.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Text(
                    "Rozetler",
                    color = DeliColors.OnBackground,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            items(state.items) { badge: BadgeRow ->
                DeliCard {
                    Text(
                        badge.name,
                        color = DeliColors.OnBackground,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    badge.description?.takeIf { it.isNotBlank() }?.let {
                        Spacer(Modifier.height(4.dp))
                        MutedText(it)
                    }
                    badge.price?.let {
                        Spacer(Modifier.height(4.dp))
                        Text(it, color = DeliColors.Warning, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

/* ------------------------------------------------------------------ */
/*  Settings / profile of the signed-in user                           */
/* ------------------------------------------------------------------ */

/**
 * The account settings list. It is a plain list on purpose: the profile itself (with its
 * "Profili Düzenle" button) is the screen behind the Profil tab, so this one only holds
 * the two settings pages and the sign-out row.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SettingsScreen(
    contentPadding: PaddingValues,
    onLogout: () -> Unit,
    onOpenSettings: (tab: String) -> Unit,
    /** The app's own language, and the way to change it. */
    lang: AppLang = AppLang.TR,
    onLangChange: (AppLang) -> Unit = {},
    /** The app's own look, and the way to change it — the kits, kept on the device. */
    theme: Palette = DeliPalettes.fallback,
    onThemeChange: (Palette) -> Unit = {},
    /** Opens the store, where every kit is shown with a preview of itself. */
    onOpenThemeStore: () -> Unit = {},
    /** Hoset's own settings: version, updates, licence — the install, not the account. */
    onOpenHosetSettings: () -> Unit = {},
) {
    var showLogoutDialog by remember { mutableStateOf(false) }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = {
                Text(
                    t("Çıkış Yap"),
                    color = DeliColors.OnBackground,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                )
            },
            text = {
                Text(
                    t("Hesabınızdan çıkış yapmak istediğinize emin misiniz?"),
                    color = DeliColors.OnSurfaceVariant,
                    fontSize = 14.sp,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        onLogout()
                    }
                ) {
                    Text(t("Çıkış Yap"), color = DeliColors.Danger, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text(t("Vazgeç"), color = DeliColors.OnBackground)
                }
            },
            containerColor = DeliColors.Surface,
            shape = DeliShapes.card,
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeliColors.Background)
            .verticalScroll(rememberScrollState())
            .padding(
                start = 14.dp,
                end = 14.dp,
                top = 10.dp,
                bottom = contentPadding.calculateBottomPadding() + 24.dp,
            ),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Section Title
        SettingsSectionTitle(t("Hesap Ayarları"))

        // Settings items card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .kitSurface(padding = 0.dp),
        ) {
            Column {
                SettingsRowItem(
                    icon = Icons.Filled.Person,
                    iconTint = DeliColors.Primary,
                    title = t("Kişisel Ayarlar"),
                    subtitle = t("Profil resmi, biyografi ve tercihler"),
                    onClick = { onOpenSettings("personal") },
                )

                HorizontalDivider(color = DeliColors.Border)

                SettingsRowItem(
                    icon = Icons.Filled.Lock,
                    iconTint = DeliColors.Blue,
                    title = t("Güvenlik ve Şifre"),
                    subtitle = t("Parola güncelleme ve oturum güvenliği"),
                    onClick = { onOpenSettings("security") },
                )
            }
        }

        // One language, in one place: the app and the site are the same setting now, so
        // picking here changes both. Closed by default — the eight languages spread out in
        // the open turned this screen into a wall of names that nobody reads.
        SettingsSectionTitle(t("Dil"))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .kitSurface(padding = 0.dp),
        ) {
            var open by remember { mutableStateOf(false) }
            // A long press here is the way into Yavşarca: it is not one of the site's
            // languages, so it is not offered with them — someone who wants it gets it by
            // holding the row they would change the language from.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = { open = true },
                        onLongClick = { onLangChange(AppLang.YAVSAR) },
                    )
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Filled.Language,
                    contentDescription = null,
                    tint = DeliColors.Primary,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(t("Dil"), color = DeliColors.OnBackground, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Text(lang.label, color = DeliColors.OnSurfaceVariant, fontSize = 12.sp)
                }
                Icon(
                    Icons.Filled.ExpandMore,
                    contentDescription = null,
                    tint = DeliColors.OnSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
            DropdownMenu(expanded = open, onDismissRequest = { open = false }, containerColor = DeliColors.Surface) {
                // The joke language is only listed once it is the one in force, so it can
                // always be left again; otherwise it is found, not offered.
                for (option in AppLang.entries.filter { !it.easter || it == lang }) {
                    val active = option == lang
                    DropdownMenuItem(
                        text = {
                            Text(
                                option.label,
                                color = if (active) DeliColors.Primary else DeliColors.OnBackground,
                                fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                            )
                        },
                        trailingIcon = {
                            if (active) {
                                Icon(
                                    Icons.Filled.Check,
                                    contentDescription = null,
                                    tint = DeliColors.Primary,
                                    modifier = Modifier.size(17.dp),
                                )
                            }
                        },
                        onClick = {
                            open = false
                            onLangChange(option)
                        },
                    )
                }
            }
        }

        // The app's own look. Ten palettes, laid out as swatches: the colour is the label,
        // and a list of ten names would say less than the colours do.
        SettingsSectionTitle(t("Uygulama Teması"))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .kitSurface(padding = 0.dp),
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        theme.icon,
                        contentDescription = null,
                        tint = theme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(t("Uygulama Teması"), color = DeliColors.OnBackground, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Text(t(theme.name), color = DeliColors.OnSurfaceVariant, fontSize = 12.sp)
                    }
                    val allAvailable = DeliPalettes.available
                    Text(
                        text = tf("{0} / {1}", (allAvailable.indexOfFirst { it.id == theme.id }.takeIf { it >= 0 } ?: 0) + 1, allAvailable.size),
                        color = DeliColors.OnSurfaceVariant,
                        fontSize = 12.sp,
                    )
                }

                HorizontalDivider(color = DeliColors.Border)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    for (option in DeliPalettes.available) {
                        val active = option.id == theme.id
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(DeliShapes.control)
                                .clickable { onThemeChange(option) }
                                .padding(4.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(DeliShapes.control)
                                    .background(option.background)
                                    .border(
                                        width = if (active) 2.dp else 1.dp,
                                        color = if (active) option.primary else DeliColors.Border,
                                        shape = DeliShapes.control,
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                // Three colours say what a palette is: the surface it puts
                                // text on, and the two it paints with.
                                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Box(Modifier.size(9.dp).clip(CircleShape).background(option.surfaceVariant))
                                    Box(Modifier.size(9.dp).clip(CircleShape).background(option.primary))
                                    Box(Modifier.size(9.dp).clip(CircleShape).background(option.blue))
                                }
                            }
                            Spacer(Modifier.height(5.dp))
                            Text(
                                text = t(option.name),
                                color = if (active) DeliColors.Primary else DeliColors.OnSurfaceVariant,
                                fontSize = 10.sp,
                                fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }

        // The kit in force, opening the store where all of them are drawn as themselves.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .kitSurface(padding = 0.dp),
        ) {
            Column {
                SettingsRowItem(
                    icon = Icons.Filled.Palette,
                    iconTint = DeliColors.Primary,
                    title = t("Tema Mağazası"),
                    subtitle = t("On bir kit, her biri kendi önizlemesiyle"),
                    onClick = onOpenThemeStore,
                )

                HorizontalDivider(color = DeliColors.Border)

                // The install's own settings, next to the look: version, updates, licence.
                SettingsRowItem(
                    icon = Icons.Filled.Info,
                    iconTint = DeliColors.Blue,
                    title = t("Hoset Ayarları"),
                    subtitle = t("Sürüm, güncelleme ve lisans"),
                    onClick = onOpenHosetSettings,
                )
            }
        }

        // Section Title: Session
        SettingsSectionTitle(t("Oturum"))

        // Logout Row
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .kitSurface(padding = 0.dp)
                .clickable { showLogoutDialog = true }
                .padding(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.AutoMirrored.Filled.Logout,
                    contentDescription = null,
                    tint = DeliColors.Danger,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(t("Çıkış Yap"), color = DeliColors.Danger, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text(t("Mevcut oturumu sonlandır"), color = DeliColors.OnSurfaceVariant, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun SettingsSectionTitle(text: String) {
    Text(
        text = text.uppercase(),
        color = DeliColors.OnSurfaceVariant,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.8.sp,
        modifier = Modifier.padding(start = 4.dp, top = 2.dp),
    )
}

@Composable
private fun SettingsRowItem(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(DeliShapes.control)
                .background(iconTint.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = DeliColors.OnBackground, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(subtitle, color = DeliColors.OnSurfaceVariant, fontSize = 12.sp)
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = DeliColors.OnSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
    }
}

/* ------------------------------------------------------------------ */
/*  Random post / random user                                          */
/* ------------------------------------------------------------------ */

@Composable
fun RandomScreen(
    kind: String,
    onResolved: (String) -> Unit,
    onMessage: (String) -> Unit,
) {
    val vm = com.delidehset.mobile.ui.deliViewModel { RandomViewModel(it.repository) }
    val state by vm.state.collectAsStateWithLifecycle()

    // Built here rather than in the effect below: `t` reads the language from the
    // composition, and a coroutine is not a composable.
    val pickFailed = t("Rastgele hedef bulunamadı: {0}")
    LaunchedEffect(kind) { vm.pick(kind) }
    LaunchedEffect(state.target, state.error) {
        state.target?.let { onResolved(it) }
        state.error?.let { onMessage(fill(pickFailed, it)) }
    }

    if (state.error != null) {
        ErrorBox(state.error!!, Modifier.fillMaxSize(), onRetry = { vm.pick(kind, force = true) })
    } else {
        LoadingBox(Modifier.fillMaxSize())
    }
}
