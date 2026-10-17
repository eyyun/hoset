package com.delidehset.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.delidehset.mobile.ui.AppLang
import com.delidehset.mobile.ui.DeliScaffold
import com.delidehset.mobile.ui.components.UpdateDialog
import com.delidehset.mobile.ui.components.UpdateRow
import com.delidehset.mobile.ui.deliContainer
import com.delidehset.mobile.ui.translate
import com.delidehset.mobile.ui.tr
import com.delidehset.mobile.ui.deliViewModel
import com.delidehset.mobile.ui.nav.Routes
import com.delidehset.mobile.ui.screens.AccountSettingsScreen
import com.delidehset.mobile.ui.screens.AuthWebScreen
import com.delidehset.mobile.ui.screens.BadgesScreen
import com.delidehset.mobile.ui.screens.ChatScreen
import com.delidehset.mobile.ui.screens.ContactScreen
import com.delidehset.mobile.ui.screens.FeedScreen
import com.delidehset.mobile.ui.screens.HolesScreen
import com.delidehset.mobile.ui.screens.LoginScreen
import com.delidehset.mobile.ui.screens.PageScreen
import com.delidehset.mobile.ui.screens.PostScreen
import com.delidehset.mobile.ui.screens.ProfileScreen
import com.delidehset.mobile.ui.screens.RandomScreen
import com.delidehset.mobile.ui.screens.SearchScreen
import com.delidehset.mobile.ui.screens.SettingsScreen
import com.delidehset.mobile.ui.screens.SubmitScreen
import com.delidehset.mobile.ui.screens.UsersScreen
import com.delidehset.mobile.ui.screens.WebFrameScreen
import com.delidehset.mobile.ui.theme.DeliTheme
import com.delidehset.mobile.vm.AccountSettingsViewModel
import com.delidehset.mobile.vm.AppViewModel
import com.delidehset.mobile.vm.AuthViewModel
import com.delidehset.mobile.vm.FeedViewModel
import com.delidehset.mobile.vm.HolesViewModel
import com.delidehset.mobile.vm.PostViewModel
import com.delidehset.mobile.vm.ProfileViewModel
import com.delidehset.mobile.vm.SearchViewModel
import com.delidehset.mobile.vm.SubmitViewModel
import com.delidehset.mobile.vm.UpdateState
import com.delidehset.mobile.vm.UpdateViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { DeliTheme { DeliRoot() } }
    }
}

@Composable
private fun DeliRoot() {
    val container = deliContainer()
    val appVm = deliViewModel { AppViewModel(it.repository, it.session, it.lang) }
    val viewer by appVm.viewer.collectAsStateWithLifecycle()
    // The app's own language: the frame, the titles and every screen read it from here.
    val lang by appVm.lang.collectAsStateWithLifecycle()
    val appMessage by appVm.message.collectAsStateWithLifecycle()

    // The app updates itself from its own GitHub releases; nothing here goes through a
    // store, so this is the only way a fix reaches an install that is already out there.
    val appContext = LocalContext.current.applicationContext
    val updateVm = deliViewModel { UpdateViewModel(appContext) }
    val updateState by updateVm.state.collectAsStateWithLifecycle()
    val updateVisible by updateVm.visible.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { updateVm.maybeCheckOnLaunch() }

    val snackbar = remember { SnackbarHostState() }
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route ?: Routes.FEED
    // The route *pattern* is the same for every "page" destination, so the sidebar needs
    // the concrete screen (pattern + its argument) to know what to highlight.
    val currentKey = currentScreenKey(backStack)

    LaunchedEffect(appMessage) {
        appMessage?.let { snackbar.showSnackbar(it); appVm.consumeMessage() }
    }

    val showBack = currentRoute.startsWith("post") ||
        currentRoute.startsWith("user") ||
        currentRoute.startsWith("page") ||
        currentRoute.startsWith("random") ||
        currentRoute.startsWith("web") ||
        currentRoute.startsWith("account") ||
        currentRoute.startsWith("settings") ||
        currentRoute.startsWith("contact")

    DeliScaffold(
        title = translate(lang, titleFor(currentRoute, backStack?.arguments)),
        lang = lang,
        viewer = viewer,
        currentRoute = currentKey,
        onNavigate = { route ->
            // Always build a fresh entry: reusing the top entry for the same route
            // pattern would keep the previous screen's arguments (e.g. tapping Sohbet
            // after Leaderboard would reopen Leaderboard). Popping back to the feed
            // first keeps the stack one level deep, so launchSingleTop below can only
            // ever match the feed itself.
            nav.navigate(route) {
                popUpTo(Routes.FEED) { inclusive = false }
                launchSingleTop = true
            }
        },
        onLogout = {
            appVm.logout()
            clearWebSession()
            nav.navigate(Routes.FEED) {
                popUpTo(Routes.FEED) { inclusive = false }
            }
        },
        snackbarHostState = snackbar,
        drawerFooter = {
            UpdateRow(
                version = BuildConfig.VERSION_NAME,
                state = updateState,
                onCheck = {
                    if (updateState is UpdateState.Available) updateVm.open() else updateVm.checkNow()
                },
            )
        },
        showBack = showBack,
        onBack = { nav.popBackStack() },
        // Settings live one tap from your own profile's top bar, like every other app.
        onOpenSettings = if (
            viewer.isSignedIn &&
            viewer.username?.let { currentRoute == "user?name={name}" && backStack?.arguments?.getString("name").equals(it, true) } == true
        ) {
            { nav.navigate(Routes.SETTINGS) }
        } else null,
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = Routes.FEED,
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            val noPad = PaddingValues(0.dp)

            composable(Routes.FEED) {
                val vm = deliViewModel("feed") { FeedViewModel(it.repository) }
                FeedScreen(
                    vm = vm,
                    hole = null,
                    effortOnly = false,
                    canVote = true,
                    contentPadding = noPad,
                    onOpenPost = { nav.navigate(Routes.post(it)) },
                    onOpenHole = { nav.navigate("feed/$it") },
                    onOpenUser = { nav.navigate(Routes.user(it)) },
                )
            }

            composable(Routes.FEED_EFFORT) {
                val vm = deliViewModel("feed-effort") { FeedViewModel(it.repository) }
                FeedScreen(
                    vm = vm,
                    hole = null,
                    effortOnly = true,
                    canVote = true,
                    contentPadding = noPad,
                    onOpenPost = { nav.navigate(Routes.post(it)) },
                    onOpenHole = { nav.navigate("feed/$it") },
                    onOpenUser = { nav.navigate(Routes.user(it)) },
                )
            }

            composable(
                route = "feed/{hole}",
                arguments = listOf(navArgument("hole") { type = NavType.StringType }),
            ) { entry ->
                val hole = entry.arguments?.getString("hole")
                val vm = deliViewModel("feed-$hole") { FeedViewModel(it.repository) }
                FeedScreen(
                    vm = vm,
                    hole = hole,
                    effortOnly = false,
                    canVote = true,
                    contentPadding = noPad,
                    onOpenPost = { nav.navigate(Routes.post(it)) },
                    onOpenHole = { nav.navigate("feed/$it") },
                    onOpenUser = { nav.navigate(Routes.user(it)) },
                )
            }

            composable(Routes.HOLES) {
                val vm = deliViewModel { HolesViewModel(it.repository) }
                HolesScreen(
                    vm = vm,
                    contentPadding = noPad,
                    onOpenHole = { nav.navigate("feed/$it") },
                )
            }

            composable(
                route = "post?permalink={permalink}",
                arguments = listOf(navArgument("permalink") {
                    type = NavType.StringType
                    defaultValue = ""
                }),
            ) { entry ->
                val permalink = entry.arguments?.getString("permalink").orEmpty()
                val vm = deliViewModel("post-$permalink") { PostViewModel(it.repository) }
                PostScreen(
                    vm = vm,
                    permalink = permalink,
                    canVote = true,
                    signedIn = viewer.isSignedIn,
                    viewerName = viewer.username,
                    contentPadding = noPad,
                    onOpenHole = { nav.navigate("feed/$it") },
                    onOpenUser = { nav.navigate(Routes.user(it)) },
                    onOpenLogin = { nav.navigate(Routes.LOGIN) },
                    onMessage = { appVm.showMessage(it) },
                    onBack = { nav.popBackStack() },
                )
            }

            composable(Routes.SEARCH) {
                val vm = deliViewModel { SearchViewModel(it.repository) }
                SearchScreen(
                    vm = vm,
                    contentPadding = noPad,
                    onOpenPost = { nav.navigate(Routes.post(it)) },
                    onOpenUser = { nav.navigate(Routes.user(it)) },
                )
            }

            composable(Routes.SUBMIT) {
                val vm = deliViewModel { SubmitViewModel(it.repository) }
                SubmitScreen(
                    vm = vm,
                    signedIn = viewer.isSignedIn,
                    contentPadding = noPad,
                    onSubmitted = { nav.navigate(Routes.FEED) { launchSingleTop = true } },
                    onMessage = { appVm.showMessage(it) },
                    onOpenLogin = { nav.navigate(Routes.LOGIN) },
                )
            }

            composable(Routes.LOGIN) {
                val vm = deliViewModel { AuthViewModel(it.repository) }
                LoginScreen(
                    vm = vm,
                    contentPadding = noPad,
                    onSignedIn = {
                        appVm.refresh()
                        appVm.showMessage(tr("Giriş başarılı."))
                        nav.popBackStack()
                    },
                    onOpenWebAuth = { mode -> nav.navigate("authweb?mode=$mode") },
                    onMessage = { appVm.showMessage(it) },
                )
            }

            // Sign-up (and a fallback sign-in) run against the real page because of
            // Cloudflare Turnstile; the session is handed to the native client right after.
            composable(
                route = "authweb?mode={mode}",
                arguments = listOf(navArgument("mode") {
                    type = NavType.StringType
                    defaultValue = "login"
                }),
            ) { entry ->
                val mode = entry.arguments?.getString("mode").orEmpty()
                AuthWebScreen(
                    mode = mode,
                    cookies = deliContainer().cookies,
                    contentPadding = noPad,
                    onAuthenticated = {
                        appVm.showMessage(tr("Oturum uygulamaya aktarıldı."))
                        appVm.refresh()
                        nav.navigate(Routes.FEED) { launchSingleTop = true }
                    },
                    onCancel = { nav.popBackStack() },
                )
            }

            // Account settings (Personal + Security), drawn natively.
            composable(
                route = "account?tab={tab}",
                arguments = listOf(navArgument("tab") {
                    type = NavType.StringType
                    defaultValue = "personal"
                }),
            ) { entry ->
                val tab = entry.arguments?.getString("tab").orEmpty()
                val vm = deliViewModel { AccountSettingsViewModel(it.repository) }
                AccountSettingsScreen(
                    vm = vm,
                    initialTab = tab,
                    contentPadding = noPad,
                    onMessage = { appVm.showMessage(it) },
                    // A new picture is only visible once the session snapshot is taken
                    // again (the drawer, the bottom bar and the profile read it from
                    // there) and once Coil has forgotten the old bytes — the site serves
                    // the new picture from the same URL as the old one.
                    onProfileChanged = {
                        container.forgetImages()
                        appVm.refresh()
                    },
                )
            }

            composable(
                route = "user?name={name}",
                arguments = listOf(navArgument("name") {
                    type = NavType.StringType
                    defaultValue = ""
                }),
            ) { entry ->
                val name = entry.arguments?.getString("name").orEmpty()
                val vm = deliViewModel("profile-$name") { ProfileViewModel(it.repository) }
                ProfileScreen(
                    vm = vm,
                    name = name,
                    isSelf = name.isNotBlank() && name.equals(viewer.username, ignoreCase = true),
                    contentPadding = noPad,
                    onEditProfile = { nav.navigate(Routes.account("personal")) },
                    onMessage = { appVm.showMessage(it) },
                )
            }

            composable(
                route = "page?kind={kind}",
                arguments = listOf(navArgument("kind") {
                    type = NavType.StringType
                    defaultValue = "badges"
                }),
            ) { entry ->
                val kind = entry.arguments?.getString("kind").orEmpty()
                when (kind) {
                    "users" -> UsersScreen(
                        contentPadding = noPad,
                        onOpenUser = { nav.navigate(Routes.user(it)) },
                    )
                    "badges" -> BadgesScreen(contentPadding = noPad)
                    // The room is native now — text and voice both: the websocket and the
                    // call live in a one-pixel WebView behind the list (see ChatBridge),
                    // and everything the user sees and taps is Compose.
                    "chat" -> ChatScreen(
                        container = deliContainer(),
                        signedIn = viewer.isSignedIn,
                        viewerName = viewer.username,
                        contentPadding = noPad,
                        onOpenLogin = { nav.navigate(Routes.LOGIN) },
                    )
                    else -> PageScreen(
                        kind = kind,
                        contentPadding = noPad,
                        onOpenPost = { nav.navigate(Routes.post(it)) },
                        onOpenInApp = { path, title -> nav.navigate(Routes.web(path, title)) },
                    )
                }
            }

            composable(
                route = "random?kind={kind}",
                arguments = listOf(navArgument("kind") {
                    type = NavType.StringType
                    defaultValue = "post"
                }),
            ) { entry ->
                val kind = entry.arguments?.getString("kind").orEmpty()
                RandomScreen(
                    kind = kind,
                    onResolved = { target ->
                        if (kind == "user") {
                            nav.navigate(Routes.user(target.substringAfter("/@")))
                        } else {
                            nav.navigate(Routes.post(target))
                        }
                    },
                    onMessage = { appVm.showMessage(it) },
                )
            }

            // Any site page the app has no native reader for, hosted in-app.
            composable(
                route = "web?path={path}&title={title}",
                arguments = listOf(
                    navArgument("path") { type = NavType.StringType; defaultValue = "/" },
                    navArgument("title") { type = NavType.StringType; defaultValue = "Hoset" },
                ),
            ) { entry ->
                WebFrameScreen(
                    path = entry.arguments?.getString("path").orEmpty().ifBlank { "/" },
                    title = entry.arguments?.getString("title").orEmpty().ifBlank { "Hoset" },
                    cookies = deliContainer().cookies,
                    onClose = { nav.popBackStack() },
                )
            }

            composable(Routes.CONTACT) {
                ContactScreen(
                    contentPadding = noPad,
                    onMessage = { appVm.showMessage(it) },
                )
            }

            composable(Routes.SETTINGS) {
                SettingsScreen(
                    contentPadding = noPad,
                    lang = lang,
                    onLangChange = { appVm.setLang(it) },
                    onLogout = {
                        appVm.logout()
                        clearWebSession()
                        nav.navigate(Routes.FEED) { popUpTo(Routes.FEED) { inclusive = false } }
                    },
                    onOpenSettings = { tab -> nav.navigate("account?tab=$tab") },
                )
            }
        }
    }

    if (updateVisible) {
        UpdateDialog(
            state = updateState,
            onDownload = { updateVm.download() },
            onRetry = { updateVm.retry() },
            onDismiss = { updateVm.dismiss() },
        )
    }
}

/**
 * The concrete screen behind a back stack entry, in the same shape as the routes on
 * [com.delidehset.mobile.ui.nav.DrawerItem] — e.g. `page?kind=chat` rather than the
 * `page?kind={kind}` pattern, which every one of those entries would otherwise match.
 */
private fun currentScreenKey(entry: androidx.navigation.NavBackStackEntry?): String {
    val pattern = entry?.destination?.route ?: return Routes.FEED
    return when (pattern) {
        Routes.PAGE_PATTERN -> Routes.page(entry.arguments?.getString("kind").orEmpty())
        // The bottom bar highlights "Profil" by comparing against the concrete own-profile
        // route, so this entry must resolve to `user?name=<whoever>` rather than a pattern.
        "user?name={name}" -> Routes.user(entry.arguments?.getString("name").orEmpty())
        // A single hole's feed is still the Home entry, just scoped to that hole.
        "feed/{hole}" -> Routes.FEED
        else -> pattern
    }
}

/**
 * Drops the session the in-app WebView keeps for the chat frame and the sign-up sheet.
 * Without this, signing out of the native client would leave those pages signed in.
 */
private fun clearWebSession() {
    val cookies = android.webkit.CookieManager.getInstance()
    cookies.removeAllCookies(null)
    cookies.flush()
    android.webkit.WebStorage.getInstance().deleteAllData()
}

private fun titleFor(route: String, args: Bundle?): String = when {
    route == "feed/{hole}" -> "/h/" + (args?.getString("hole") ?: "")
    route == Routes.FEED_EFFORT -> "Effortpostlar"
    route.startsWith("post") -> "Gönderi"
    route.startsWith("user") -> "@" + (args?.getString("name") ?: "profil")
    route.startsWith("holes") -> "Topluluklar"
    route.startsWith("search") -> "Ara"
    route.startsWith("submit") -> "Gönderi oluştur"
    route.startsWith("login") -> "Oturum aç"
    route.startsWith("settings") -> "Ayarlar"
    route.startsWith("account") -> "Ayarlar"
    route.startsWith("contact") -> "Bize ulaşın"
    route.startsWith("random") -> "Rastgele"
    route.startsWith("page") -> when (args?.getString("kind")) {
        "admins" -> "Yöneticiler"
        "comments" -> "Moderasyon kaydı"
        "modlog" -> "Moderasyon kaydı"
        "leaderboard" -> "Leaderboard"
        "stats" -> "İstatistikler"
        "users" -> "Kullanıcılar"
        "badges" -> "Rozetler"
        "contact" -> "Bize ulaşın"
        else -> "Hoset"
    }
    else -> "Hoset"
}
