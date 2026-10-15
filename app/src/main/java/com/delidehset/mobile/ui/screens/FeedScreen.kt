package com.delidehset.mobile.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import com.delidehset.mobile.ui.Text
import com.delidehset.mobile.ui.t
import com.delidehset.mobile.ui.tf
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import com.delidehset.mobile.ui.components.PostCard
import com.delidehset.mobile.ui.theme.DeliColors
import com.delidehset.mobile.vm.FeedViewModel
import kotlinx.coroutines.flow.distinctUntilChanged

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
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val context = LocalContext.current

    LaunchedEffect(hole, effortOnly) {
        vm.start(hole = hole, sort = state.sort, effortOnly = effortOnly)
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
        // ---- sort bar, same four buttons as the site ----
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(DeliColors.Background)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FeedSort.entries.forEach { sort ->
                val selected = state.sort == sort
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
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
            isRefreshing = state.loading && state.posts.isNotEmpty(),
            onRefresh = { vm.refresh() },
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
                    items(state.posts, key = { it.id }) { post: FeedPost ->
                        PostCard(
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
