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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import com.delidehset.mobile.data.DeliNotification
import com.delidehset.mobile.ui.Text
import com.delidehset.mobile.ui.components.DeliButtonPrimary
import com.delidehset.mobile.ui.components.ErrorBox
import com.delidehset.mobile.ui.components.LoadingBox
import com.delidehset.mobile.ui.components.rememberManualRefresh
import com.delidehset.mobile.ui.t
import com.delidehset.mobile.ui.tf
import com.delidehset.mobile.ui.theme.DeliColors
import com.delidehset.mobile.ui.theme.DeliShapes
import com.delidehset.mobile.ui.theme.kitSurface
import com.delidehset.mobile.vm.NotificationsViewModel
import kotlin.math.absoluteValue

/**
 * `/notifications`, drawn natively.
 *
 * The site's own page is one `list-group` of the same rows: an unread badge, an age, the
 * post it happened on, the comment quoted under it, and one button that marks the lot as
 * read. The app keeps that shape — the only thing it adds is a tap target that goes
 * straight to the comment.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    vm: NotificationsViewModel,
    contentPadding: PaddingValues,
    onOpenPost: (permalink: String) -> Unit,
    onChanged: () -> Unit = {},
) {
    val state by vm.state.collectAsStateWithLifecycle()

    // A notification list is only worth showing current: every visit asks again.
    LaunchedEffect(Unit) { vm.load(force = true) }

    // The visit's own load stays quiet; only a pull shows the spinner.
    val pull = rememberManualRefresh(state.loading)

    when {
        state.loading && state.page.items.isEmpty() -> LoadingBox(Modifier.fillMaxSize())

        state.error != null && state.page.items.isEmpty() ->
            ErrorBox(state.error!!, Modifier.fillMaxSize(), onRetry = { vm.load(force = true) })

        // Pull to refresh, like the feed, the profile and the communities: the list is asked
        // again instead of trusting whatever the screen was opened with.
        else -> PullToRefreshBox(
            isRefreshing = pull.active && state.loading,
            onRefresh = {
                pull.start()
                vm.load(force = true)
            },
            modifier = Modifier.fillMaxSize().background(DeliColors.Background),
        ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 12.dp,
                end = 12.dp,
                top = 10.dp,
                bottom = contentPadding.calculateBottomPadding() + 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Column(modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
                    Text(
                        text = t("Bildirimler"),
                        color = DeliColors.OnBackground,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = if (state.page.unreadCount > 0) {
                            tf("{0} okunmamış", state.page.unreadCount)
                        } else {
                            t("Hepsi okundu.")
                        },
                        color = DeliColors.OnSurfaceVariant,
                        fontSize = 12.sp,
                    )
                    if (state.page.items.any { it.unread }) {
                        Spacer(Modifier.height(8.dp))
                        DeliButtonPrimary(t("Tümünü okundu işaretle")) {
                            vm.markAllRead()
                            onChanged()
                        }
                    }
                }
            }

            if (state.page.items.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().kitSurface().padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = t("Henüz bildirim yok."),
                            color = DeliColors.OnBackground,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = t("Biri size yanıt verdiğinde veya sizden bahsettiğinde burada görünür."),
                            color = DeliColors.OnSurfaceVariant,
                            fontSize = 12.sp,
                        )
                    }
                }
            }

            items(state.page.items, key = { it.id + it.hashCode().absoluteValue }) { row ->
                NotificationRow(row) {
                    vm.markRead(row)
                    row.postHref?.let(onOpenPost)
                }
            }
        }
        }
    }
}

@Composable
private fun NotificationRow(row: DeliNotification, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .kitSurface(padding = 0.dp)
            .clickable(enabled = row.postHref != null, onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        // A dot where the site puts its `new` badge: same information, one glance.
        Box(
            modifier = Modifier
                .padding(top = 6.dp, end = 10.dp)
                .size(7.dp)
                .clip(CircleShape)
                .background(if (row.unread) DeliColors.Danger else DeliColors.Border),
        )
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = row.postTitle ?: t("Bildirim"),
                    color = DeliColors.OnBackground,
                    fontSize = 13.sp,
                    fontWeight = if (row.unread) FontWeight.SemiBold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = row.ago,
                    color = DeliColors.OnSurfaceVariant,
                    fontSize = 11.sp,
                )
            }
            Spacer(Modifier.height(3.dp))
            Text(
                text = row.text.ifBlank { t("Yeni bir bildirim var.") },
                color = DeliColors.OnSurfaceVariant,
                fontSize = 12.5.sp,
                lineHeight = 17.sp,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
