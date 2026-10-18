package com.delidehset.mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import com.delidehset.mobile.ui.Text
import com.delidehset.mobile.ui.tf
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.delidehset.mobile.data.DeliHttp
import com.delidehset.mobile.data.Hole
import com.delidehset.mobile.ui.components.DeliCard
import com.delidehset.mobile.ui.components.EmptyBox
import com.delidehset.mobile.ui.components.ErrorBox
import com.delidehset.mobile.ui.components.LoadingBox
import com.delidehset.mobile.ui.components.rememberManualRefresh
import com.delidehset.mobile.ui.components.MutedText
import com.delidehset.mobile.ui.theme.DeliColors
import com.delidehset.mobile.ui.theme.nameColor
import com.delidehset.mobile.vm.HolesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HolesScreen(
    vm: HolesViewModel,
    contentPadding: PaddingValues,
    onOpenHole: (String) -> Unit,
) {
    val state by vm.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { vm.load() }

    // The spinner belongs to the person's own pull, not to the load this screen starts.
    val pull = rememberManualRefresh(state.loading)

    Column(modifier = Modifier.fillMaxSize().background(DeliColors.Background)) {
        when {
            state.loading -> LoadingBox(Modifier.fillMaxSize())
            state.error != null -> ErrorBox(state.error!!, Modifier.fillMaxSize(), onRetry = { vm.load(true) })
            state.items.isEmpty() -> EmptyBox("Henüz topluluk yok.", Modifier.fillMaxSize())
            // Pull to refresh, like the feed: the community list and its counts are read
            // again from the site instead of only the first time the screen was opened.
            else -> PullToRefreshBox(
                isRefreshing = pull.active && state.loading,
                onRefresh = {
                    pull.start()
                    vm.load(true)
                },
                modifier = Modifier.fillMaxSize(),
            ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 12.dp,
                    end = 12.dp,
                    top = 12.dp,
                    bottom = contentPadding.calculateBottomPadding() + 16.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    Text(
                        "Topluluklar",
                        color = DeliColors.OnBackground,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                }
                items(state.items, key = { it.name }) { hole: Hole ->
                    DeliCard(onClick = { onOpenHole(hole.name) }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            HoleMark(name = hole.name, logo = hole.logo)
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "/h/${hole.name}",
                                    color = DeliColors.Blue,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Spacer(Modifier.height(2.dp))
                                Row {
                                    MutedText(tf("{0} gönderi", hole.postCount))
                                    hole.createdOn?.let { MutedText("  ·  $it") }
                                }
                            }
                        }
                        // The stealth value used to be repeated here, bare, at the end of
                        // the title row: a lonely "No" with nothing next to it saying what
                        // it was a no about. The labelled line below is where it belongs.
                        if (hole.stealth != null || hole.publicUse != null) {
                            Spacer(Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                hole.stealth?.let { MutedText("Stealth: $it") }
                                hole.publicUse?.let { MutedText(tf("Herkese açık: {0}", it)) }
                            }
                        }
                    }
                }
            }
            }
        }
    }
}

/**
 * A community's picture, or a tile with its initial when the site gave none.
 *
 * The list used to be names only, which made twenty communities read as twenty identical
 * rows; post cards have shown these logos all along. The fallback keeps its colour stable
 * per name, the same way an account's initial does. The drawing itself lives in
 * [HoleLogo], which the community page and its settings use too.
 */
@Composable
private fun HoleMark(name: String, logo: String?) {
    HoleLogo(url = logo, name = name, size = 34.dp)
}
