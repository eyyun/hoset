package com.delidehset.mobile.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
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
import com.delidehset.mobile.ui.theme.DeliShapes
import com.delidehset.mobile.ui.theme.KitCategory
import com.delidehset.mobile.ui.theme.KitMotion
import com.delidehset.mobile.ui.theme.Palette
import com.delidehset.mobile.ui.theme.kitDetailedDecoration
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

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
    val palette = DeliColors.palette
    val isDetailed = palette.category == KitCategory.DETAILED

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
                Column {
                    TopAppBar(
                        modifier = Modifier.statusBarsPadding(),
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isDetailed) {
                                    Box(
                                        modifier = Modifier
                                            .size(26.dp)
                                            .clip(RoundedCornerShape(palette.smallRadius.coerceAtLeast(4.dp)))
                                            .background(
                                                Brush.linearGradient(
                                                    listOf(
                                                        palette.primary.copy(alpha = 0.25f),
                                                        palette.blue.copy(alpha = 0.15f),
                                                    )
                                                )
                                            )
                                            .border(
                                                1.dp,
                                                palette.primary.copy(alpha = 0.55f),
                                                RoundedCornerShape(palette.smallRadius.coerceAtLeast(4.dp)),
                                            ),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            imageVector = palette.icon,
                                            contentDescription = null,
                                            tint = palette.primary,
                                            modifier = Modifier.size(15.dp),
                                        )
                                    }
                                    Spacer(Modifier.width(9.dp))
                                }
                                Text(
                                    text = translate(lang, title),
                                    color = DeliColors.OnBackground,
                                    fontSize = 17.sp,
                                    fontWeight = if (isDetailed) palette.titleWeight else FontWeight.SemiBold,
                                    letterSpacing = if (isDetailed) palette.letterSpacing else 0.sp,
                                    maxLines = 1,
                                )
                            }
                        },
                        navigationIcon = {
                            if (showBack) {
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
                    if (isDetailed) {
                        val accent = if (palette.glowColor != Color.Transparent) palette.glowColor else palette.primary
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.dp)
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(
                                            Color.Transparent,
                                            accent.copy(alpha = 0.85f),
                                            palette.blue.copy(alpha = 0.85f),
                                            accent.copy(alpha = 0.85f),
                                            Color.Transparent,
                                        )
                                    )
                                )
                        )
                    } else if (DeliColors.palette.dividers) {
                        HorizontalDivider(color = DeliColors.Border, thickness = 1.dp)
                    }
                }
            },
            bottomBar = {
                val chatRoute = Routes.page("chat")
                val ownProfile = viewer.username?.let { Routes.user(it) }
                if (isDetailed) {
                    DetailedBottomDock(
                        palette = palette,
                        viewer = viewer,
                        lang = lang,
                        currentRoute = currentRoute,
                        chatRoute = chatRoute,
                        ownProfile = ownProfile,
                        onNavigate = onNavigate,
                    )
                } else {
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
                            NavigationBarItem(
                                selected = currentRoute == chatRoute,
                                onClick = {
                                    if (viewer.isSignedIn) onNavigate(chatRoute) else onNavigate(Routes.LOGIN)
                                },
                                icon = { Icon(Icons.Filled.Forum, contentDescription = translate(lang, "Sohbet")) },
                                label = { Text(translate(lang, "Sohbet"), fontSize = 11.sp) },
                            )
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
                }
            },
        ) { padding ->
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
                CompositionLocalProvider(
                    LocalAppLang provides lang,
                    LocalDensity provides scaledDensity,
                ) {
                    content(contentPadding)
                }
                if (isDetailed) {
                    DetailedAmbientOverlay(palette = palette)
                }
            }
        }
    }
}

/**
 * Floating glass/cyber capsule dock used exclusively by [KitCategory.DETAILED] themes
 * so switching from a Simple theme to a Detailed theme transforms the app's navigation chrome.
 */
@Composable
private fun DetailedBottomDock(
    palette: Palette,
    viewer: Viewer,
    lang: AppLang,
    currentRoute: String,
    chatRoute: String,
    ownProfile: String?,
    onNavigate: (String) -> Unit,
) {
    val dockShape = RoundedCornerShape(
        when {
            palette.cardRadius <= 6.dp -> 8.dp
            palette.cardRadius >= 16.dp -> 26.dp
            else -> 20.dp
        }
    )
    val accent = if (palette.glowColor != Color.Transparent) palette.glowColor else palette.primary
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 14.dp,
                    shape = dockShape,
                    ambientColor = accent.copy(alpha = 0.45f),
                    spotColor = accent.copy(alpha = 0.55f),
                )
                .clip(dockShape)
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            palette.surface,
                            palette.surfaceVariant.copy(alpha = 0.92f),
                            palette.surface,
                        )
                    )
                )
                .kitDetailedDecoration(palette, glowAlpha = 0.85f)
                .border(1.2.dp, accent.copy(alpha = 0.5f), dockShape)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DetailedDockItem(
                palette = palette,
                selected = currentRoute.startsWith(Routes.FEED),
                label = translate(lang, "Feed"),
                onClick = { onNavigate(Routes.FEED) },
                icon = { tint -> Icon(Icons.Filled.Home, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp)) },
                modifier = Modifier.weight(1f),
            )
            DetailedDockItem(
                palette = palette,
                selected = currentRoute == Routes.HOLES,
                label = translate(lang, "Topluluklar"),
                onClick = { onNavigate(Routes.HOLES) },
                icon = { tint -> Icon(Icons.Filled.Groups, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp)) },
                modifier = Modifier.weight(1f),
            )
            DetailedDockItem(
                palette = palette,
                selected = currentRoute == chatRoute,
                label = translate(lang, "Sohbet"),
                onClick = {
                    if (viewer.isSignedIn) onNavigate(chatRoute) else onNavigate(Routes.LOGIN)
                },
                icon = { tint -> Icon(Icons.Filled.Forum, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp)) },
                modifier = Modifier.weight(1f),
            )
            val profileSelected = currentRoute == ownProfile ||
                currentRoute == Routes.SETTINGS ||
                currentRoute == Routes.LOGIN
            DetailedDockItem(
                palette = palette,
                selected = profileSelected,
                label = translate(lang, if (viewer.isSignedIn) "Profil" else "Giriş"),
                onClick = {
                    if (ownProfile != null) onNavigate(ownProfile) else onNavigate(Routes.LOGIN)
                },
                icon = { tint ->
                    if (viewer.isSignedIn) {
                        DeliAvatar(url = viewer.avatarUrl, size = 22.dp, name = viewer.username)
                    } else {
                        Icon(Icons.Filled.Person, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
                    }
                },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun DetailedDockItem(
    palette: Palette,
    selected: Boolean,
    label: String,
    onClick: () -> Unit,
    icon: @Composable (Color) -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = if (palette.glowColor != Color.Transparent) palette.glowColor else palette.primary
    val itemShape = RoundedCornerShape(palette.controlRadius.coerceAtLeast(6.dp))
    val tint = if (selected) accent else palette.onSurfaceVariant
    Column(
        modifier = modifier
            .padding(horizontal = 3.dp)
            .clip(itemShape)
            .background(
                if (selected) {
                    Brush.verticalGradient(
                        listOf(
                            accent.copy(alpha = 0.22f),
                            palette.primaryContainer.copy(alpha = 0.35f),
                        )
                    )
                } else {
                    Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent))
                }
            )
            .then(
                if (selected) Modifier.border(1.dp, accent.copy(alpha = 0.45f), itemShape)
                else Modifier
            )
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (selected) {
            Box(
                modifier = Modifier
                    .width(16.dp)
                    .height(2.5.dp)
                    .clip(CircleShape)
                    .background(accent)
            )
            Spacer(Modifier.height(3.dp))
        } else {
            Spacer(Modifier.height(5.5.dp))
        }
        icon(tint)
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            color = if (selected) palette.onBackground else palette.onSurfaceVariant,
            fontSize = 10.5.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * Live non-interactive atmospheric canvas overlay for [KitCategory.DETAILED] kits.
 * Touch events pass straight through to the underlying screen.
 */
@Composable
fun DetailedAmbientOverlay(
    palette: Palette,
    modifier: Modifier = Modifier.fillMaxSize(),
) {
    val transition = rememberInfiniteTransition(label = "detailed_ambient")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 9000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "ambient_phase",
    )
    val pulse by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "ambient_pulse",
    )
    val accent = if (palette.glowColor != Color.Transparent) palette.glowColor else palette.primary

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val twoPi = (Math.PI * 2.0).toFloat()

        when (palette.motion) {
            KitMotion.NEON -> {
                // Perspective cyber grid along top/bottom + sweeping scanline beam
                val gridColor = accent.copy(alpha = 0.07f)
                val cols = 8
                for (i in 1 until cols) {
                    val x = w * i / cols
                    drawLine(gridColor, Offset(x, 0f), Offset(x, h * 0.14f), strokeWidth = 1f)
                    drawLine(gridColor, Offset(x, h * 0.86f), Offset(x, h), strokeWidth = 1f)
                }
                // Sweeping neon laser scanline
                val scanY = h * phase
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Transparent, accent.copy(alpha = 0.10f), Color.Transparent),
                        startY = (scanY - 36.dp.toPx()).coerceAtLeast(0f),
                        endY = (scanY + 36.dp.toPx()).coerceAtMost(h),
                    ),
                    topLeft = Offset(0f, (scanY - 36.dp.toPx()).coerceAtLeast(0f)),
                    size = Size(w, 72.dp.toPx()),
                )
                // Side HUD edge rails
                val railLen = 64.dp.toPx()
                val railY = h * 0.35f
                drawLine(accent.copy(alpha = 0.35f * pulse), Offset(2.dp.toPx(), railY), Offset(2.dp.toPx(), railY + railLen), strokeWidth = 2.dp.toPx())
                drawLine(palette.blue.copy(alpha = 0.35f * pulse), Offset(w - 2.dp.toPx(), railY), Offset(w - 2.dp.toPx(), railY + railLen), strokeWidth = 2.dp.toPx())
            }
            KitMotion.PULSE -> {
                // Breathing aurora spotlight orbs in top-right and bottom-left
                val r1 = w * 0.65f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(accent.copy(alpha = 0.12f * pulse), Color.Transparent),
                        center = Offset(w * 0.88f, h * 0.12f),
                        radius = r1,
                    ),
                    radius = r1,
                    center = Offset(w * 0.88f, h * 0.12f),
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(palette.blue.copy(alpha = 0.10f * (1.35f - pulse)), Color.Transparent),
                        center = Offset(w * 0.12f, h * 0.85f),
                        radius = r1 * 0.85f,
                    ),
                    radius = r1 * 0.85f,
                    center = Offset(w * 0.12f, h * 0.85f),
                )
            }
            KitMotion.FLOAT -> {
                // Drifting snow / stardust particles
                val count = 18
                for (i in 0 until count) {
                    val seedX = ((i * 37 + 13) % 100) / 100f
                    val seedY = ((i * 61 + 29) % 100) / 100f
                    val yPos = ((seedY + phase) % 1f) * h
                    val xPos = (seedX * w + sin(phase * twoPi + i) * 14.dp.toPx()).coerceIn(0f, w)
                    val radius = (1.5f + (i % 3) * 1.1f).dp.toPx()
                    val dotColor = if (i % 2 == 0) accent.copy(alpha = 0.18f * pulse) else Color.White.copy(alpha = if (palette.dark) 0.15f else 0.35f)
                    drawCircle(dotColor, radius = radius, center = Offset(xPos, yPos))
                }
            }
            KitMotion.RIPPLE -> {
                // Flowing sine wave aurora ribbons near top and bottom
                for (layer in 0..2) {
                    val path = Path()
                    val baseY = h * (0.14f + layer * 0.04f)
                    path.moveTo(0f, baseY)
                    val segs = 32
                    for (s in 1..segs) {
                        val x = w * s / segs
                        val wy = baseY + sin(s * 0.35f + phase * twoPi + layer) * 10.dp.toPx()
                        path.lineTo(x, wy)
                    }
                    drawPath(
                        path = path,
                        color = accent.copy(alpha = 0.08f - layer * 0.02f),
                        style = Stroke(width = 1.5.dp.toPx()),
                    )
                }
            }
            KitMotion.SHIFT -> {
                // Slowly orbiting ambient color mesh
                val cx = w * (0.5f + 0.32f * cos(phase * twoPi))
                val cy = h * (0.28f + 0.18f * sin(phase * twoPi))
                val r = w * 0.55f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(accent.copy(alpha = 0.10f), Color.Transparent),
                        center = Offset(cx, cy),
                        radius = r,
                    ),
                    radius = r,
                    center = Offset(cx, cy),
                )
            }
            KitMotion.BOUNCE -> {
                // Rising playful bokeh bubbles
                val bubbles = 10
                for (i in 0 until bubbles) {
                    val bx = (((i * 43 + 17) % 100) / 100f) * w
                    val by = (1f - ((i * 29 / 100f + phase) % 1f)) * h
                    val br = (8 + (i % 4) * 6).dp.toPx()
                    val bColor = if (i % 2 == 0) accent.copy(alpha = 0.065f) else palette.upvote.copy(alpha = 0.055f)
                    drawCircle(bColor, radius = br, center = Offset(bx, by))
                }
            }
            KitMotion.NONE -> {
                // Subtle editorial double frame around screen edges
                val m = 4.dp.toPx()
                drawRect(
                    color = palette.primary.copy(alpha = 0.10f),
                    topLeft = Offset(m, m),
                    size = Size((w - m * 2).coerceAtLeast(0f), (h - m * 2).coerceAtLeast(0f)),
                    style = Stroke(width = 1.dp.toPx()),
                )
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
