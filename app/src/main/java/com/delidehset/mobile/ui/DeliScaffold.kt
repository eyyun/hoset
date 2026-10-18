package com.delidehset.mobile.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import com.delidehset.mobile.ui.Text
import com.delidehset.mobile.ui.t
import com.delidehset.mobile.ui.tf
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.delidehset.mobile.R
import com.delidehset.mobile.data.Viewer
import com.delidehset.mobile.ui.components.DeliAvatar
import com.delidehset.mobile.ui.nav.DrawerItem
import com.delidehset.mobile.ui.nav.Routes
import com.delidehset.mobile.ui.nav.drawerItems
import com.delidehset.mobile.ui.nav.drawerSections
import com.delidehset.mobile.ui.theme.DeliColors
import kotlinx.coroutines.launch
import com.delidehset.mobile.ui.theme.DeliShapes

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DeliScaffold(
    title: String,
    viewer: Viewer,
    currentRoute: String,
    onNavigate: (String) -> Unit,
    onLogout: () -> Unit,
    snackbarHostState: SnackbarHostState,
    showBack: Boolean = false,
    onBack: () -> Unit = {},
    /** Set on screens that should offer a way into settings from the top bar. */
    onOpenSettings: (() -> Unit)? = null,
    /** The bell's number: unread notifications, as the site's own header counts them. */
    unreadNotifications: Int = 0,
    /** Set to put a bell in the top bar; absent on signed-out screens. */
    onOpenNotifications: (() -> Unit)? = null,
    /** The app's own language, for the frame's own lines (the drawer, the bars). */
    lang: AppLang = AppLang.TR,
    floatingActionButton: @Composable () -> Unit = {},
    /** The drawer's bottom line: the app's version, and the way to ask about a newer one. */
    drawerFooter: @Composable () -> Unit = {},
    content: @Composable (androidx.compose.foundation.layout.PaddingValues) -> Unit,
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = DeliColors.Background,
                modifier = Modifier.width(300.dp).fillMaxHeight(),
            ) {
                DrawerBody(
                    viewer = viewer,
                    lang = lang,
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        scope.launch { drawerState.close() }
                        onNavigate(route)
                    },
                    onLogout = {
                        scope.launch { drawerState.close() }
                        onLogout()
                    },
                    footer = drawerFooter,
                )
            }
        },
    ) {
        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            containerColor = DeliColors.Background,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            floatingActionButton = floatingActionButton,
            topBar = {
                TopAppBar(
                    modifier = Modifier.statusBarsPadding(),
                    title = {
                        Text(
                            text = translate(lang, title),
                            color = DeliColors.OnBackground,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                        )
                    },
                    navigationIcon = {
                        if (showBack) {
                            // A real icon rather than a text arrow: the glyph used to sit
                            // off-centre in its touch target and read as a stray dash.
                            IconButton(onClick = onBack) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = translate(lang, "Geri"),
                                    tint = DeliColors.OnBackground,
                                    modifier = Modifier.size(22.dp),
                                )
                            }
                        } else {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(
                                    Icons.Filled.Menu,
                                    contentDescription = translate(lang, "Menü"),
                                    tint = DeliColors.OnBackground,
                                )
                            }
                        }
                    },
                    actions = {
                        // Search moved out of the bottom bar (that slot is the room now),
                        // and the top bar is where every other app keeps it.
                        if (currentRoute != Routes.SEARCH) {
                            IconButton(onClick = { onNavigate(Routes.SEARCH) }) {
                                Icon(
                                    Icons.Filled.Search,
                                    contentDescription = translate(lang, "Ara"),
                                    tint = DeliColors.OnBackground,
                                    modifier = Modifier.size(22.dp),
                                )
                            }
                        }
                        if (onOpenNotifications != null) {
                            IconButton(onClick = onOpenNotifications) {
                                BadgedBox(
                                    badge = {
                                        if (unreadNotifications > 0) {
                                            Badge(containerColor = DeliColors.Danger) {
                                                Text(
                                                    text = if (unreadNotifications > 9) "9+" else unreadNotifications.toString(),
                                                    fontSize = 9.sp,
                                                )
                                            }
                                        }
                                    },
                                ) {
                                    Icon(
                                        Icons.Filled.Notifications,
                                        contentDescription = translate(lang, "Bildirimler"),
                                        tint = DeliColors.OnBackground,
                                        modifier = Modifier.size(22.dp),
                                    )
                                }
                            }
                        }
                        if (onOpenSettings != null) {
                            IconButton(onClick = onOpenSettings) {
                                Icon(
                                    Icons.Filled.Settings,
                                    contentDescription = translate(lang, "Ayarlar"),
                                    tint = DeliColors.OnBackground,
                                    modifier = Modifier.size(22.dp),
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = DeliColors.Surface),
                )
                // Kits that line their surfaces up carry a hairline under the bar; the
                // ones that separate with air leave it out.
                if (DeliColors.palette.dividers) {
                    HorizontalDivider(color = DeliColors.Border, thickness = 1.dp)
                }
            },
            bottomBar = {
                // The bar is fixed: it neither disappears nor jumps up over the keyboard,
                // which is what every app of this shape does. The keyboard simply covers
                // it, and the content above (see the padding below) is lifted instead.
                Column(modifier = Modifier.navigationBarsPadding()) {
                if (DeliColors.palette.dividers) {
                    HorizontalDivider(color = DeliColors.Border, thickness = 1.dp)
                }
                NavigationBar(
                    modifier = Modifier,
                    windowInsets = WindowInsets(0, 0, 0, 0),
                    containerColor = DeliColors.Surface,
                    contentColor = DeliColors.OnBackground,
                ) {
                    NavigationBarItem(
                        selected = currentRoute.startsWith(Routes.FEED),
                        onClick = { onNavigate(Routes.FEED) },
                        icon = { Icon(Icons.Filled.Home, contentDescription = translate(lang, "Feed")) },
                        label = { Text(translate(lang, "Feed"), fontSize = 11.sp) },
                    )
                    NavigationBarItem(
                        selected = currentRoute == Routes.HOLES,
                        onClick = { onNavigate(Routes.HOLES) },
                        icon = { Icon(Icons.Filled.Groups, contentDescription = translate(lang, "Topluluklar")) },
                        label = { Text(translate(lang, "Topluluklar"), fontSize = 11.sp) },
                    )
                    // The room takes the middle slot: it is the one thing you come back to
                    // all day, and the site's own mobile bar leads with it too.
                    val chatRoute = Routes.page("chat")
                    NavigationBarItem(
                        selected = currentRoute == chatRoute,
                        onClick = {
                            if (viewer.isSignedIn) onNavigate(chatRoute) else onNavigate(Routes.LOGIN)
                        },
                        icon = { Icon(Icons.Filled.Forum, contentDescription = translate(lang, "Sohbet")) },
                        label = { Text(translate(lang, "Sohbet"), fontSize = 11.sp) },
                    )
                    // The tab opens the viewer's own profile, the way a profile tab is
                    // expected to behave; settings are one tap further in (from the
                    // profile hero, or the drawer's "Ayarlar").
                    val ownProfile = viewer.username?.let { Routes.user(it) }
                    NavigationBarItem(
                        selected = currentRoute == ownProfile ||
                            currentRoute == Routes.SETTINGS ||
                            currentRoute == Routes.LOGIN,
                        onClick = {
                            if (ownProfile != null) onNavigate(ownProfile) else onNavigate(Routes.LOGIN)
                        },
                        icon = {
                            if (viewer.isSignedIn) {
                                DeliAvatar(url = viewer.avatarUrl, size = 24.dp, name = viewer.username)
                            } else {
                                Icon(Icons.Filled.Person, contentDescription = translate(lang, "Profil"))
                            }
                        },
                        label = {
                            Text(
                                translate(lang, if (viewer.isSignedIn) "Profil" else "Giriş"),
                                fontSize = 11.sp,
                            )
                        },
                    )
                }
                }
            },
        ) { padding ->
            // Edge to edge means the window is not resized for the keyboard. While the
            // keyboard is up the content is lifted by exactly the keyboard's height — not
            // by the keyboard *plus* the bar, which is what left a strip of empty space
            // between a composer and the keyboard.
            val density = LocalDensity.current
            val fontScale = com.delidehset.mobile.data.HosetPrefs.fontScaleState.floatValue
            val scaledDensity = androidx.compose.ui.unit.Density(
                density = density.density,
                fontScale = density.fontScale * fontScale,
            )
            val imeBottom = WindowInsets.ime.getBottom(density)
            val contentPadding = if (imeBottom > 0) {
                PaddingValues(
                    top = padding.calculateTopPadding(),
                    bottom = with(density) { imeBottom.toDp() },
                )
            } else {
                padding
            }
            Box(modifier = Modifier.fillMaxSize()) {
                // Every screen is written in Turkish and reads its lines through `t`, which
                // resolves them against the language picked in Ayarlar.
                CompositionLocalProvider(
                    LocalAppLang provides lang,
                    LocalDensity provides scaledDensity,
                ) {
                    content(contentPadding)
                }
            }
        }
    }
}

@Composable
private fun DrawerBody(
    viewer: Viewer,
    lang: AppLang,
    currentRoute: String,
    onNavigate: (String) -> Unit,
    onLogout: () -> Unit,
    footer: @Composable () -> Unit,
) {
    Column(modifier = Modifier.fillMaxHeight().padding(vertical = 12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // The app's own mark now, not the site's banner: this client is its own thing.
            Image(
                painter = painterResource(R.drawable.logo_hoset),
                contentDescription = null,
                modifier = Modifier.size(36.dp),
            )
            Spacer(Modifier.width(10.dp))
            Text(
                com.delidehset.mobile.data.HosetPrefs.appTitleState.value,
                color = DeliColors.OnBackground,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
            )
        }

        HorizontalDivider(color = DeliColors.Border)

        LazyColumn(modifier = Modifier.weight(1f).padding(vertical = 6.dp)) {
            for (section in drawerSections) {
                val visibleItems = section.items.filter { item ->
                    (!item.signedInOnly || viewer.isSignedIn) && (!item.adminOnly || viewer.isAdmin)
                }
                if (visibleItems.isNotEmpty()) {
                    item(key = "header-${section.title}") {
                        Text(
                            text = translate(lang, section.title).uppercase(),
                            color = DeliColors.OnSurfaceVariant,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(start = 18.dp, top = 14.dp, bottom = 4.dp),
                        )
                    }
                    items(visibleItems, key = { it.route }) { item: DrawerItem ->
                        DrawerRow(
                            label = translate(lang, item.label),
                            badge = item.badge,
                            selected = currentRoute == item.route,
                            icon = {
                                Icon(
                                    item.icon,
                                    contentDescription = null,
                                    tint = if (currentRoute == item.route) DeliColors.Primary else DeliColors.OnBackground,
                                    modifier = Modifier.size(19.dp)
                                )
                            },
                            onClick = { onNavigate(item.route) },
                        )
                    }
                }
            }
        }

        footer()

        HorizontalDivider(color = DeliColors.Border)

        if (viewer.isSignedIn) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DeliAvatar(url = viewer.avatarUrl, size = 34.dp, name = viewer.username)
                Spacer(Modifier.width(10.dp))
                Text(
                    text = tf("@{0}", viewer.username ?: t("Kullanıcı")),
                    color = DeliColors.OnBackground,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }
            DrawerRow(
                label = translate(lang, "Çıkış yap"),
                badge = null,
                selected = false,
                icon = { Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = DeliColors.Danger, modifier = Modifier.size(19.dp)) },
                onClick = onLogout,
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(DeliShapes.small)
                        .background(DeliColors.Primary)
                        .clickable { onNavigate(Routes.LOGIN) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) { Text(translate(lang, "Oturum aç"), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(DeliShapes.small)
                        .background(DeliColors.SurfaceVariant)
                        .clickable { onNavigate(Routes.LOGIN + "?mode=signup") }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) { Text(translate(lang, "Kaydol"), color = DeliColors.OnBackground, fontSize = 13.sp) }
            }
            DrawerRow(
                label = translate(lang, "Bize ulaşın"),
                badge = null,
                selected = false,
                icon = { Icon(Icons.AutoMirrored.Filled.Login, contentDescription = null, tint = DeliColors.OnBackground, modifier = Modifier.size(19.dp)) },
                onClick = { onNavigate(Routes.page("contact")) },
            )
        }
    }
}

@Composable
private fun DrawerRow(
    label: String,
    badge: String?,
    selected: Boolean,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .clip(DeliShapes.control)
            .background(if (selected) DeliColors.Primary.copy(alpha = 0.15f) else Color.Transparent)
            .clickable { onClick() }
            // The kit's own air, so a roomy kit is roomy in the drawer too.
            .padding(horizontal = 12.dp, vertical = DeliColors.palette.spacing * 0.8f),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            icon()
            Spacer(Modifier.width(12.dp))
            Text(
                label,
                color = if (selected) DeliColors.Primary else DeliColors.OnBackground,
                fontSize = 13.5.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                modifier = Modifier.weight(1f),
            )
            if (badge != null) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(DeliColors.PrimaryContainer)
                        .padding(horizontal = 7.dp, vertical = 2.dp)
                ) {
                    Text(
                        badge,
                        color = DeliColors.OnBackground,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
