package com.delidehset.mobile.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import com.delidehset.mobile.ui.Text
import com.delidehset.mobile.ui.t
import com.delidehset.mobile.ui.tf
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.delidehset.mobile.data.FeedPost
import com.delidehset.mobile.data.FeedSort
import com.delidehset.mobile.data.DeliHttp
import com.delidehset.mobile.ui.components.EmptyBox
import com.delidehset.mobile.ui.components.ErrorBox
import com.delidehset.mobile.ui.components.LoadingBox
import com.delidehset.mobile.ui.components.rememberManualRefresh
import com.delidehset.mobile.ui.components.DeliButtonPrimary
import com.delidehset.mobile.ui.components.PostCard
import com.delidehset.mobile.ui.theme.DeliColors
import com.delidehset.mobile.vm.FeedViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import com.delidehset.mobile.ui.theme.DeliShapes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    vm: FeedViewModel,
    hole: String?,
    effortOnly: Boolean,
    canVote: Boolean,
    contentPadding: PaddingValues,
    onOpenPost: (String) -> Unit,
    onOpenHole: (String) -> Unit,
    onOpenUser: (String) -> Unit,
    onMessage: (String) -> Unit = {},
    /** A community's own "post in this hole" button. */
    onOpenSubmit: (hole: String) -> Unit = {},
    /** Opened only for the people the community itself lets in. */
    onOpenHoleSettings: (hole: String) -> Unit = {},
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val holeFollowing by vm.holeFollowing.collectAsStateWithLifecycle()
    val holeBlocked by vm.holeBlocked.collectAsStateWithLifecycle()
    val holeCanManage by vm.holeCanManage.collectAsStateWithLifecycle()
    val holeLogo by vm.holeLogo.collectAsStateWithLifecycle()
    val holeDisplayName by vm.holeDisplayName.collectAsStateWithLifecycle()
    val vmMessage by vm.message.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val context = LocalContext.current
    val reduceMotion = remember { com.delidehset.mobile.data.HosetPrefs.reduceMotion(context) }
    val pull = rememberManualRefresh(state.loading)

    // A private community: the lock screen is the whole feed, and the dialog is the only
    // way in.
    val lock = state.lock
    var showJoin by remember { mutableStateOf(false) }
    var joinReason by remember { mutableStateOf("") }
    var requested by remember(hole) { mutableStateOf(false) }

    LaunchedEffect(vmMessage) {
        vmMessage?.let { onMessage(it); vm.consumeMessage() }
    }

    LaunchedEffect(hole, effortOnly) {
        val initialSort = com.delidehset.mobile.data.HosetPrefs.defaultSort(context)
        vm.start(hole = hole, sort = initialSort, effortOnly = effortOnly)
    }

    // Choosing a sort replaces the list, and the list keeps its scroll offset — which left
    // the person in the middle of a list they had not seen the top of. Any change of sort
    // starts at the top again.
    LaunchedEffect(state.sort) {
        runCatching { listState.scrollToItem(0) }
    }

    // Infinite scroll: ask for the next page a little before the end.
    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }
            .distinctUntilChanged()
            .collect { last ->
                if (last >= state.posts.size - 3) vm.loadMore()
            }
    }

    Column(modifier = Modifier.fillMaxSize().background(DeliColors.Background)) {
        // A hole's feed carries the hole itself: its name, and the follow button the
        // site's own hole page has. The community is the subject here, not the app.
        if (hole != null) {
            // A community page, not just a filtered feed: its name, joining and leaving,
            // and the "post in this hole" button the site puts on top of its own page.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 12.dp, top = 10.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // The community's own picture off its page's sidebar, and its initial
                    // until that page has answered.
                    HoleLogo(url = holeLogo, name = hole, size = 42.dp)
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = holeDisplayName?.takeIf { it.isNotBlank() } ?: "/h/$hole",
                            color = DeliColors.OnBackground,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = if (!holeDisplayName.isNullOrBlank()) "/h/$hole · ${t("Topluluk")}" else t("Topluluk"),
                            color = DeliColors.OnSurfaceVariant,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    val following = holeFollowing == true
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(if (following) DeliColors.SurfaceVariant else DeliColors.Primary)
                            .clickable { vm.toggleHoleFollow(hole) }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                    ) {
                        // The site's own wording for the two: "leave this community" has to
                        // read like an exit, not like a follow that stopped.
                        Text(
                            text = t(if (following) "Topluluktan ayrıl" else "Topluluğa katıl"),
                            color = if (following) DeliColors.OnBackground else androidx.compose.ui.graphics.Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    Spacer(Modifier.width(6.dp))
                    Icon(
                        imageVector = if (holeBlocked == true) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                        contentDescription = t(if (holeBlocked == true) "Engeli kaldır" else "Engelle"),
                        tint = DeliColors.OnSurfaceVariant,
                        modifier = Modifier
                            .size(22.dp)
                            .clickable { vm.toggleHoleBlock(hole) },
                    )
                    if (holeCanManage) {
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = t("Topluluk ayarları"),
                            tint = DeliColors.Primary,
                            modifier = Modifier
                                .size(22.dp)
                                .clickable { onOpenHoleSettings(hole) },
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))
                DeliButtonPrimary(
                    text = t("Gönderi oluştur"),
                    modifier = Modifier.fillMaxWidth(),
                ) { onOpenSubmit(hole) }
            }
        }

        if (lock?.locked == true) {
            LockedHole(
                hole = hole.orEmpty(),
                reason = lock.reason,
                canRequest = lock.canRequest,
                requested = requested,
                onRequest = { showJoin = true },
            )
        } else {
        // ---- sort bar, same four buttons as the site ----
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(DeliColors.Background)
                .horizontalScroll(androidx.compose.foundation.rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FeedSort.entries.forEach { sort ->
                val selected = state.sort == sort
                Box(
                    modifier = Modifier
                        .clip(DeliShapes.small)
                        .background(if (selected) DeliColors.Primary else DeliColors.SurfaceVariant)
                        .clickable { vm.setSort(sort) }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = sort.label,
                        color = if (selected) androidx.compose.ui.graphics.Color.White else DeliColors.OnBackground,
                        fontSize = 12.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    )
                }
            }
        }

        PullToRefreshBox(
            // Only a pull the person made shows the spinner: opening the screen, changing
            // the sort or returning with a cached list are all loads nobody asked to watch.
            isRefreshing = pull.active && state.loading && state.posts.isNotEmpty(),
            onRefresh = {
                pull.start()
                vm.refresh()
            },
            modifier = Modifier.fillMaxSize(),
        ) {
            when {
                state.loading && state.posts.isEmpty() -> LoadingBox(Modifier.fillMaxSize())

                state.error != null && state.posts.isEmpty() ->
                    ErrorBox(state.error!!, Modifier.fillMaxSize(), onRetry = { vm.refresh() })

                state.posts.isEmpty() ->
                    EmptyBox(
                        text = if (hole != null) tf("/h/{0} için gönderi yok.", hole) else t("Henüz gönderi yok."),
                        modifier = Modifier.fillMaxSize(),
                    )

                else -> LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 12.dp,
                        end = 12.dp,
                        top = 4.dp,
                        bottom = contentPadding.calculateBottomPadding() + 16.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(state.posts, key = { it.id }, contentType = { "post" }) { post: FeedPost ->
                        PostCard(
                            modifier = if (reduceMotion) Modifier else Modifier.animateItem(),
                            post = post,
                            canVote = canVote,
                            onOpen = { onOpenPost(post.permalink) },
                            onOpenHole = { onOpenHole(it) },
                            onOpenUser = onOpenUser,
                            onVote = { vm.vote(post, it) },
                            onShare = {
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, DeliHttp.BASE_URL + post.permalink)
                                }
                                context.startActivity(Intent.createChooser(intent, "Paylaş"))
                            },
                        )
                    }
                    if (state.loadingMore) {
                        item { LoadingBox() }
                    }
                }
            }
        }
        }
    }

    if (showJoin) {
        AlertDialog(
            onDismissRequest = { showJoin = false },
            title = { Text(t("Katılma isteği gönder"), color = DeliColors.OnBackground) },
            text = {
                Column {
                    MutedBox(t("Neden katılmak istediğini kısaca yaz."))
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = joinReason,
                        onValueChange = { if (it.length <= 500) joinReason = it },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 6,
                        placeholder = { Text("İsteğe bağlı", color = DeliColors.OnSurfaceVariant) },
                        shape = DeliShapes.control,
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showJoin = false
                        vm.requestJoin(hole.orEmpty(), joinReason) { ok ->
                            if (ok) {
                                requested = true
                                joinReason = ""
                            }
                        }
                    },
                ) { Text(t("İsteği gönder"), color = DeliColors.Primary) }
            },
            dismissButton = {
                TextButton(onClick = { showJoin = false }) {
                    Text(t("İptal"), color = DeliColors.OnSurfaceVariant)
                }
            },
            containerColor = DeliColors.Surface,
        )
    }
}

/**
 * What a private community shows instead of its posts.
 *
 * The lock and the note are the community's own; the app only adds the way to ask for
 * entry, which is the one thing the website offers a non-member here too.
 */
@Composable
private fun LockedHole(
    hole: String,
    reason: String?,
    canRequest: Boolean,
    requested: Boolean,
    onRequest: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            Icons.Filled.Lock,
            contentDescription = null,
            tint = DeliColors.OnSurfaceVariant,
            modifier = Modifier.size(38.dp),
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "/h/$hole " + t("gizli"),
            color = DeliColors.OnBackground,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            reason?.takeIf { it.isNotBlank() } ?: t("Bu topluluk yalnızca üyelerine açık."),
            color = DeliColors.OnSurfaceVariant,
            fontSize = 14.sp,
        )
        if (canRequest) {
            Spacer(Modifier.height(18.dp))
            if (requested) {
                Text(
                    t("Katılma isteğin gönderildi. Onay bekleniyor."),
                    color = DeliColors.Success,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                )
            } else {
                DeliButtonPrimary(
                    text = t("Katılma isteği gönder"),
                    modifier = Modifier.fillMaxWidth(),
                ) { onRequest() }
            }
        }
    }
}

/** A small muted line inside a dialog, where a full MutedText would be too quiet. */
@Composable
private fun MutedBox(text: String) {
    Text(text, color = DeliColors.OnSurfaceVariant, fontSize = 13.sp)
}
