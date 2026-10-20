package com.delidehset.mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import com.delidehset.mobile.ui.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.delidehset.mobile.data.FeedPost
import com.delidehset.mobile.data.DeliHttp
import com.delidehset.mobile.ui.theme.DeliColors
import com.delidehset.mobile.ui.theme.DeliShapes

/**
 * The native twin of an `article.feed-card`:
 * hole header, title, excerpt, optional media, then the action row.
 */
@Composable
fun PostCard(
    post: FeedPost,
    canVote: Boolean,
    onOpen: () -> Unit,
    onOpenHole: (String) -> Unit,
    onOpenUser: (String) -> Unit,
    onVote: (Int) -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Both of these are Jsoup parses. Doing them on every composition (which is what
    // happens while a list scrolls) is the app's most expensive bit of per-frame work,
    // so they are pinned to the post's own content instead.
    val titleText = remember(post.id, post.titleHtml) { DeliHtml.excerpt(post.titleHtml) }
    val excerptText = remember(post.id, post.excerptMobile, post.excerptDesktop) {
        DeliHtml.excerpt(post.excerptMobile ?: post.excerptDesktop)
    }
    val context = androidx.compose.ui.platform.LocalContext.current
    val clipboard = androidx.compose.ui.platform.LocalClipboardManager.current
    val doubleTapVote = com.delidehset.mobile.data.HosetPrefs.doubleTapVoteState.value
    val dimSeen = com.delidehset.mobile.data.HosetPrefs.dimSeenPostsState.value
    val copyLongPress = com.delidehset.mobile.data.HosetPrefs.copyOnLongPressState.value
    val showDownload = com.delidehset.mobile.data.HosetPrefs.mediaDownloadState.value
    val seenPosts = com.delidehset.mobile.data.HosetPrefs.seenPostsState.value
    val isSeen = dimSeen && (post.permalink in seenPosts || post.id in seenPosts)

    DeliCard(
        modifier = modifier,
        onClick = {
            com.delidehset.mobile.data.HosetPrefs.markPostSeen(context, post.permalink.ifBlank { post.id })
            onOpen()
        },
        onDoubleClick = if (doubleTapVote && canVote) {
            {
                val next = if (post.viewerVote == 1) 0 else 1
                onVote(next)
            }
        } else null,
        onLongClick = if (copyLongPress) {
            {
                val full = if (excerptText.isNotBlank()) "$titleText\n\n$excerptText" else titleText
                clipboard.setText(androidx.compose.ui.text.AnnotatedString(full))
                android.widget.Toast.makeText(
                    context,
                    com.delidehset.mobile.ui.tr("Metin kopyalandı."),
                    android.widget.Toast.LENGTH_SHORT,
                ).show()
            }
        } else null,
    ) {
        // ---- hole header ----
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { post.hole?.let(onOpenHole) },
            ) {
                com.delidehset.mobile.ui.screens.HoleLogo(
                    url = post.holeLogo,
                    name = post.hole ?: post.holeLabel ?: "general",
                    size = 24.dp,
                )
                Spacer(Modifier.width(8.dp))
                val label = com.delidehset.mobile.data.HoleStore.cachedLabel(post.hole)
                    ?.let { "$it · /h/${post.hole}" }
                    ?: post.holeLabel
                    ?: post.hole?.let { "/h/$it" }
                    ?: "general"
                Text(
                    text = label,
                    color = DeliColors.Blue,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            if (post.nsfw) {
                Spacer(Modifier.width(8.dp))
                Box(
                    Modifier
                        .clip(DeliShapes.small)
                        .background(DeliColors.Danger)
                        .padding(horizontal = 5.dp, vertical = 1.dp)
                ) { Text("NSFW", color = androidx.compose.ui.graphics.Color.White, fontSize = 10.sp) }
            }
            Spacer(Modifier.weight(1f))
            post.ago?.let { MutedText(it) }
        }

        Spacer(Modifier.height(6.dp))

        // ---- title ----
        Text(
            text = titleText,
            color = if (isSeen) DeliColors.OnSurfaceVariant else DeliColors.OnBackground,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )

        val compact = com.delidehset.mobile.data.HosetPrefs.compactFeedState.value
        val hideNsfw = com.delidehset.mobile.data.HosetPrefs.hideNsfwMediaState.value

        // ---- excerpt ----
        if (!compact && excerptText.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = excerptText,
                color = DeliColors.OnSurfaceVariant,
                fontSize = 13.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }

        // ---- media ----
        // A post can carry a picture or a clip; a clip gets the play badge and is opened
        // in the app's own player (the post page) instead of an empty thumbnail.
        val video = post.mediaHref?.takeIf { post.externalMedia && isVideoUrl(it) }
        val image = post.thumbUrl
            ?.takeIf { it.isNotBlank() && !it.contains("default_thumb") }
            ?: post.images.firstOrNull()?.takeIf { !video.equals(it) }
        val showMedia = !compact && !(post.nsfw && hideNsfw)
        if (showMedia && video != null) {
            Spacer(Modifier.height(8.dp))
            // The clip's own frame stands in when the site has no picture for it.
            VideoThumb(url = image, videoUrl = video, modifier = Modifier.fillMaxWidth())
        } else if (showMedia && image != null && !isVideoUrl(image)) {
            Spacer(Modifier.height(8.dp))
            Box(modifier = Modifier.fillMaxWidth()) {
                AsyncImage(
                    model = DeliHttp.BASE_URL + image,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .clip(DeliShapes.small),
                    contentScale = ContentScale.Crop,
                )
                if (showDownload) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.65f))
                            .clickable {
                                val ok = com.delidehset.mobile.data.HosetPrefs.downloadMedia(context, image)
                                android.widget.Toast.makeText(
                                    context,
                                    com.delidehset.mobile.ui.tr(if (ok) "İndirme başlatıldı." else "İndirilemedi."),
                                    android.widget.Toast.LENGTH_SHORT,
                                ).show()
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Filled.Download,
                            contentDescription = com.delidehset.mobile.ui.t("İndir"),
                            tint = androidx.compose.ui.graphics.Color.White,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // ---- actions ----
        Row(verticalAlignment = Alignment.CenterVertically) {
            VoteArrows(
                score = post.score,
                viewerVote = post.viewerVote,
                canVote = canVote,
                onVote = onVote,
                compact = true,
            )
            Spacer(Modifier.width(12.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.clickable { onOpen() },
            ) {
                Icon(
                    Icons.Filled.ChatBubbleOutline,
                    contentDescription = com.delidehset.mobile.ui.t("Yorumlar"),
                    tint = DeliColors.OnSurfaceVariant,
                    modifier = Modifier.size(15.dp),
                )
                Text(post.commentCount.toString(), color = DeliColors.OnSurfaceVariant, fontSize = 12.sp)
            }
            Spacer(Modifier.weight(1f))
            post.authorName?.let { name ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onOpenUser(name) },
                ) {
                    DeliAvatar(url = post.authorAvatar, size = 18.dp, name = name)
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = "@$name",
                        color = DeliColors.OnSurfaceVariant,
                        fontSize = 12.sp,
                    )
                }
                Spacer(Modifier.width(10.dp))
            }
            Icon(
                Icons.Filled.Share,
                contentDescription = com.delidehset.mobile.ui.t("Paylaş"),
                tint = DeliColors.OnSurfaceVariant,
                modifier = Modifier.size(16.dp).clickable { onShare() },
            )
        }
    }
}
