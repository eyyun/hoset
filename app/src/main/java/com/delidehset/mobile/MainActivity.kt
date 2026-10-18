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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.delidehset.mobile.ui.nav.DeepLinks
import com.delidehset.mobile.ui.nav.Routes
import com.delidehset.mobile.ui.screens.AccountSettingsScreen
import com.delidehset.mobile.ui.screens.AuthWebScreen
import com.delidehset.mobile.ui.screens.BadgesScreen
import com.delidehset.mobile.ui.screens.ChatScreen
import com.delidehset.mobile.ui.screens.HoleSettingsScreen
import com.delidehset.mobile.ui.screens.RulesScreen
import com.delidehset.mobile.ui.screens.ContactScreen
import com.delidehset.mobile.ui.screens.FeedScreen
import com.delidehset.mobile.ui.screens.HolesScreen
import com.delidehset.mobile.ui.screens.LoginScreen
import com.delidehset.mobile.ui.screens.PageScreen
import com.delidehset.mobile.ui.screens.PostScreen
import com.delidehset.mobile.ui.screens.ProfileScreen
import com.delidehset.mobile.ui.UpdateSummaryDialog
import com.delidehset.mobile.ui.screens.RandomScreen
import com.delidehset.mobile.ui.screens.RelationsScreen
import com.delidehset.mobile.ui.screens.SearchScreen
import com.delidehset.mobile.ui.screens.SettingsScreen
import com.delidehset.mobile.ui.screens.SubmitScreen
import com.delidehset.mobile.ui.screens.HosetSettingsScreen
import com.delidehset.mobile.ui.screens.NotificationsScreen
import com.delidehset.mobile.vm.NotificationsViewModel
import com.delidehset.mobile.ui.screens.ThemeStoreScreen
import com.delidehset.mobile.ui.screens.UsersScreen
import com.delidehset.mobile.ui.screens.WebFrameScreen
import com.delidehset.mobile.ui.theme.DeliTheme
import com.delidehset.mobile.vm.AccountSettingsViewModel
import com.delidehset.mobile.vm.AppViewModel
import com.delidehset.mobile.vm.AuthViewModel
import com.delidehset.mobile.vm.FeedViewModel
import com.delidehset.mobile.vm.HoleSettingsViewModel
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
        incomingLink.value = intent?.dataString?.takeIf { it.startsWith("http") }
        setContent { DeliTheme { DeliRoot() } }
    }

    /**
     * A delidehset.com link, while the app is already running.
     *
     * singleTop means the system hands the new intent to this instance instead of building a
     * second one, so the link arrives here rather than in [onCreate].
     */
    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        incomingLink.value = intent.dataString?.takeIf { it.startsWith("http") }
    }

    /**
     * The app left the front. The room stays connected for a while — coming back to a chat
     * you just left should not rebuild it — and the session closes it on its own once it has
     * been out of sight long enough.
     */
    override fun onStop() {
        super.onStop()
        (application as? DeliApp)?.container?.chat?.onBackground()
    }

    companion object {
        /** What a system notification asks the app to open, if anything. */
        const val EXTRA_OPEN = "hoset.open"
        const val OPEN_NOTIFICATIONS = "notifications"

        /**
         * The site link the app was asked to open, until the router has taken it.
         *
         * A plain state, not a saved one: a link is a one-off instruction, and replaying it
         * after a screen rotation would move the reader a second time.
         */
        val incomingLink = androidx.compose.runtime.mutableStateOf<String?>(null)
    }
}

@Composable
private fun DeliRoot() {
    val container = deliContainer()
    val appContext = LocalContext.current.applicationContext
    // Read once per composition: the setting is a preference, not something the screens
    // switch while a transition is running.
    val reduceMotion by remember { mutableStateOf(com.delidehset.mobile.data.HosetPrefs.reduceMotion(appContext)) }
    val appVm = deliViewModel { AppViewModel(it.repository, it.session, it.lang, it.theme) }
    val viewer by appVm.viewer.collectAsStateWithLifecycle()
    // The app's own language: the frame, the titles and every screen read it from here.
    val lang by appVm.lang.collectAsStateWithLifecycle()
    // Likewise the app's own palette, which the colours every screen reads follow.
    val theme by appVm.theme.collectAsStateWithLifecycle()
    val appMessage by appVm.message.collectAsStateWithLifecycle()

    // The bell in the top bar reads the site's own count, refreshed whenever the app comes
    // up and after the notifications screen has marked them read.
    val notificationsVm = deliViewModel("notifications") { NotificationsViewModel(it.repository) }
    var unreadNotifications by remember { mutableStateOf(0) }
    LaunchedEffect(viewer.isSignedIn) {
        if (viewer.isSignedIn) notificationsVm.unread { unreadNotifications = it } else unreadNotifications = 0
    }

    // The app updates itself from its own GitHub releases; nothing here goes through a
    // store, so this is the only way a fix reaches an install that is already out there.
    val updateVm = deliViewModel { UpdateViewModel(appContext) }
    val updateState by updateVm.state.collectAsStateWithLifecycle()
    val updateVisible by updateVm.visible.collectAsStateWithLifecycle()
    val updateHistory by updateVm.history.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { updateVm.maybeCheckOnLaunch() }

    val snackbar = remember { SnackbarHostState() }
    val nav = rememberNavController()

    // A tapped system notification asks for the notifications screen; the rest of the
    // launch is an ordinary cold start.
    val activityContext = LocalContext.current
    LaunchedEffect(Unit) {
        val requested = (activityContext as? android.app.Activity)?.intent
            ?.getStringExtra(MainActivity.EXTRA_OPEN)
        if (requested == MainActivity.OPEN_NOTIFICATIONS) nav.navigate(Routes.NOTIFICATIONS)
    }
    // A delidehset.com link — from a browser, a message or another app — opens the screen
    // that link is about: a post, a profile, a community. Anything else the site has is
    // shown in the app's own web frame rather than the browser.
    val incoming = MainActivity.incomingLink.value
    LaunchedEffect(incoming) {
        val raw = incoming ?: return@LaunchedEffect
        MainActivity.incomingLink.value = null
        val own = DeepLinks.routeFor(raw)
        val path = DeepLinks.pathFor(raw)
        when {
            own != null -> nav.navigate(own)
            path != null -> nav.navigate(Routes.web(path, "Bağlantı"))
        }
    }

    val backStack by nav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route ?: Routes.FEED
    // The route *pattern* is the same for every "page" destination, so the sidebar needs
    // the concrete screen (pattern + its argument) to know what to highlight.
    val currentKey = currentScreenKey(backStack)

    LaunchedEffect(appMessage) {
        appMessage?.let { snackbar.showSnackbar(it); appVm.consumeMessage() }
    }

    // Anything the person can come back from gets an arrow. This used to be a hand-written
    // list of routes, which meant every newly added screen — the follower lists, for one —
    // arrived with a menu icon and no way out except the system back gesture.
    val showBack = nav.previousBackStackEntry != null

    // Says what changed, once, the first time this version runs. A dialog renders over
    // everything, so where it sits in the tree does not matter.
    UpdateSummaryDialog(BuildConfig.VERSION_NAME)

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
            // The room's socket belongs to the account that was signed in.
            container.chat.close()
            clearWebSession()
            nav.navigate(Routes.FEED) {
                popUpTo(Routes.FEED) { inclusive = false }
            }
        },
        unreadNotifications = unreadNotifications,
        onOpenNotifications = if (viewer.isSignedIn) {
            { nav.navigate(Routes.NOTIFICATIONS) }
        } else null,
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
            // Screens slide in the way the sheet under them suggests: forward pushes
            // come from the right, going back leaves to the right. Short durations and a
            // small travel keep a fast app feeling fast.
            enterTransition = {
                androidx.compose.animation.slideInHorizontally(
                    animationSpec = androidx.compose.animation.core.tween(if (reduceMotion) 0 else 220),
                    initialOffsetX = { it / 6 },
                ) + androidx.compose.animation.fadeIn(
                    animationSpec = androidx.compose.animation.core.tween(if (reduceMotion) 0 else 180)
                )
            },
            exitTransition = {
                androidx.compose.animation.fadeOut(
                    animationSpec = androidx.compose.animation.core.tween(if (reduceMotion) 0 else 140)
                )
            },
            popEnterTransition = {
                androidx.compose.animation.fadeIn(
                    animationSpec = androidx.compose.animation.core.tween(if (reduceMotion) 0 else 180)
                )
            },
            popExitTransition = {
                androidx.compose.animation.slideOutHorizontally(
                    animationSpec = androidx.compose.animation.core.tween(if (reduceMotion) 0 else 200),
                    targetOffsetX = { it / 8 },
                ) + androidx.compose.animation.fadeOut(
                    animationSpec = androidx.compose.animation.core.tween(if (reduceMotion) 0 else 160)
                )
            },
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
                    onMessage = { appVm.showMessage(it) },
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
                    onMessage = { appVm.showMessage(it) },
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
                    onMessage = { appVm.showMessage(it) },
                    onOpenSubmit = { hole -> nav.navigate(Routes.submit(hole)) },
                    onOpenHoleSettings = { name -> nav.navigate(Routes.holeSettings(name)) },
                )
            }

            composable(
                route = Routes.HOLE_SETTINGS_PATTERN,
                arguments = listOf(navArgument("hole") { type = NavType.StringType }),
            ) { entry ->
                val hole = entry.arguments?.getString("hole").orEmpty()
                val vm = deliViewModel("hole-settings-$hole") { HoleSettingsViewModel(it.repository) }
                HoleSettingsScreen(
                    vm = vm,
                    hole = hole,
                    contentPadding = noPad,
                    onMessage = { appVm.showMessage(it) },
                    onOpenWeb = { path, title -> nav.navigate(Routes.web(path, title)) },
                )
            }

            composable(Routes.RULES) {
                RulesScreen(contentPadding = noPad)
            }

            composable(Routes.HOLES) {
                val vm = deliViewModel { HolesViewModel(it.repository) }
                HolesScreen(
                    vm = vm,
                    contentPadding = noPad,
                    onOpenHole = { nav.navigate("feed/$it") },
                    onMessage = { appVm.showMessage(it) },
                )
            }

            composable(
                route = Routes.RELATIONS_PATTERN,
                arguments = listOf(
                    navArgument("name") { type = NavType.StringType; defaultValue = "" },
                    navArgument("followers") { type = NavType.BoolType; defaultValue = true },
                ),
            ) { entry ->
                RelationsScreen(
                    name = entry.arguments?.getString("name").orEmpty(),
                    followers = entry.arguments?.getBoolean("followers") ?: true,
                    contentPadding = noPad,
                    onOpenUser = { nav.navigate(Routes.user(it)) },
                )
            }

            composable(
                route = "post?permalink={permalink}&comment={comment}",
                arguments = listOf(
                    navArgument("permalink") {
                        type = NavType.StringType
                        defaultValue = ""
                    },
                    // The comment to land on, when the post was opened from a profile's
                    // comment row: the screen scrolls to it and marks it for a moment.
                    navArgument("comment") {
                        type = NavType.StringType
                        defaultValue = ""
                    },
                ),
            ) { entry ->
                val permalink = entry.arguments?.getString("permalink").orEmpty()
                val comment = entry.arguments?.getString("comment").orEmpty().takeIf { it.isNotBlank() }
                val vm = deliViewModel("post-$permalink") { PostViewModel(it.repository) }
                PostScreen(
                    vm = vm,
                    permalink = permalink,
                    highlightComment = comment,
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

            composable(
                route = Routes.SUBMIT_PATTERN,
                arguments = listOf(navArgument("hole") {
                    type = NavType.StringType
                    defaultValue = ""
                }),
            ) { entry ->
                val vm = deliViewModel { SubmitViewModel(it.repository) }
                SubmitScreen(
                    vm = vm,
                    signedIn = viewer.isSignedIn,
                    contentPadding = noPad,
                    onSubmitted = { nav.navigate(Routes.FEED) { launchSingleTop = true } },
                    onMessage = { appVm.showMessage(it) },
                    onOpenLogin = { nav.navigate(Routes.LOGIN) },
                    initialHole = entry.arguments?.getString("hole")?.takeIf { it.isNotBlank() },
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
                    // Only the moderation history is still the site's own page: the
                    // followers, the following list and every community the profile names
                    // are drawn by the app.
                    onOpenPath = { path, title -> nav.navigate(Routes.web(path, title)) },
                    onOpenHole = { hole -> nav.navigate("feed/$hole") },
                    onOpenRelations = { owner, followers ->
                        nav.navigate(Routes.relations(owner, followers))
                    },
                    onOpenPost = { nav.navigate(Routes.post(it)) },
                    onOpenPostAtComment = { permalink, comment ->
                        nav.navigate(Routes.post(permalink, comment))
                    },
                    onOpenUser = { nav.navigate(Routes.user(it)) },
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
                        onOpenUser = { nav.navigate(Routes.user(it)) },
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
                        val dest = if (kind == "user") {
                            Routes.user(target.substringAfter("/@"))
                        } else {
                            Routes.post(target)
                        }
                        nav.navigate(dest) {
                            popUpTo("random?kind={kind}") { inclusive = true }
                        }
                    },
                    onError = { nav.popBackStack() },
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

            composable(Routes.NOTIFICATIONS) {
                NotificationsScreen(
                    vm = notificationsVm,
                    contentPadding = noPad,
                    // The row's link is `/post/<uuid>[/comment/<id>]`, and the site serves
                    // that post page at that very path — so the app can read it natively
                    // instead of handing the person a browser.
                    onOpenPost = { href -> nav.navigate(Routes.post(href.substringBefore("/comment/"))) },
                    onChanged = { notificationsVm.unread { unreadNotifications = it } },
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
                    theme = theme,
                    onThemeChange = { appVm.setTheme(it) },
                    onOpenThemeStore = { nav.navigate(Routes.THEMES) },
                    onOpenHosetSettings = { nav.navigate(Routes.HOSET_SETTINGS) },
                    onLogout = {
                        appVm.logout()
                        com.delidehset.mobile.notify.HosetNotifications.clear(activityContext)
                        clearWebSession()
                        nav.navigate(Routes.FEED) { popUpTo(Routes.FEED) { inclusive = false } }
                    },
                    onOpenSettings = { tab -> nav.navigate("account?tab=$tab") },
                )
            }

            composable(Routes.THEMES) {
                ThemeStoreScreen(
                    current = theme,
                    onApply = { appVm.setTheme(it) },
                    onSaveCustom = { appVm.saveCustomTheme(it) },
                    onDeleteCustom = { appVm.deleteCustomTheme(it) },
                    contentPadding = noPad,
                )
            }

            composable(Routes.HOSET_SETTINGS) {
                HosetSettingsScreen(
                    theme = theme,
                    updateState = updateState,
                    contentPadding = noPad,
                    onOpenThemeStore = { nav.navigate(Routes.THEMES) },
                    onCheckUpdates = { updateVm.checkNow() },
                    history = updateHistory,
                    onLoadHistory = { updateVm.loadHistory() },
                    onInstallVersion = { updateVm.installVersion(it) },
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
    route.startsWith("themes") -> "Tema Mağazası"
    route.startsWith(Routes.HOSET_SETTINGS) -> "Hoset Ayarları"
    route.startsWith(Routes.NOTIFICATIONS) -> "Bildirimler"
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
        else -> com.delidehset.mobile.data.HosetPrefs.appTitleState.value
    }
    else -> com.delidehset.mobile.data.HosetPrefs.appTitleState.value
}


