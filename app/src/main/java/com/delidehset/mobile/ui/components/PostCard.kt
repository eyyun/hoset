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

    DeliCard(modifier = modifier, onClick = onOpen) {
        // ---- hole header ----
        Row(verticalAlignment = Alignment.CenterVertically) {
            val logo = DeliHttp.BASE_URL + (post.holeLogo ?: "/assets/images/logo.webp")
            AsyncImage(
                model = logo,
                contentDescription = null,
                modifier = Modifier.size(22.dp).clip(CircleShape),
                contentScale = ContentScale.Crop,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = post.holeLabel ?: post.hole ?: "general",
                color = DeliColors.Blue,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable { post.hole?.let(onOpenHole) },
            )
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
            color = DeliColors.OnBackground,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )

        // ---- excerpt ----
        if (excerptText.isNotBlank()) {
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
        val image = post.thumbUrl ?: post.images.firstOrNull()
        if (image != null) {
            Spacer(Modifier.height(8.dp))
            AsyncImage(
                model = DeliHttp.BASE_URL + image,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(DeliShapes.small),
                contentScale = ContentScale.Crop,
            )
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
                    contentDescription = "Yorumlar",
                    tint = DeliColors.OnSurfaceVariant,
                    modifier = Modifier.size(15.dp),
                )
                Text(post.commentCount.toString(), color = DeliColors.OnSurfaceVariant, fontSize = 12.sp)
            }
            Spacer(Modifier.weight(1f))
            post.authorName?.let { name ->
                Text(
                    text = "@$name",
                    color = DeliColors.OnSurfaceVariant,
                    fontSize = 12.sp,
                    modifier = Modifier.clickable { onOpenUser(name) },
                )
                Spacer(Modifier.width(10.dp))
            }
            Icon(
                Icons.Filled.Share,
                contentDescription = "Paylaş",
                tint = DeliColors.OnSurfaceVariant,
                modifier = Modifier.size(16.dp).clickable { onShare() },
            )
        }
    }
}
