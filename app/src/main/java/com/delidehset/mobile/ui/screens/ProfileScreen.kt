package com.delidehset.mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.delidehset.mobile.data.DeliHttp
import com.delidehset.mobile.ui.Text
import com.delidehset.mobile.ui.components.DeliAvatar
import com.delidehset.mobile.ui.components.DeliButtonPrimary
import com.delidehset.mobile.ui.components.DeliCard
import com.delidehset.mobile.ui.components.DeliOutlinedButton
import com.delidehset.mobile.ui.components.EmptyBox
import com.delidehset.mobile.ui.components.ErrorBox
import com.delidehset.mobile.ui.components.LoadingBox
import com.delidehset.mobile.ui.components.MutedText
import com.delidehset.mobile.ui.components.PostCard
import com.delidehset.mobile.ui.components.pullUp
import com.delidehset.mobile.ui.t
import com.delidehset.mobile.ui.tf
import com.delidehset.mobile.ui.theme.DeliColors
import com.delidehset.mobile.ui.theme.DeliShapes
import com.delidehset.mobile.ui.theme.avatarShape
import com.delidehset.mobile.ui.theme.kitSurface
import com.delidehset.mobile.vm.ProfileViewModel

/**
 * A native `@user` page: the picture, the name, the counters, and the three tabs the site
 * has — the wall, the posts and the comments — all drawn by the app.
 *
 * The wall used to be a blank area under the counters: the page's posts were never read,
 * even though the profile page ships them. Now the wall is those cards, the other two tabs
 * are their own pages, and every list opens a post in the app rather than in a browser.
 */
@Composable
fun ProfileScreen(
    vm: ProfileViewModel,
    name: String,
    contentPadding: PaddingValues,
    onMessage: (String) -> Unit,
    isSelf: Boolean = false,
    onEditProfile: () -> Unit = {},
    /** Opens a path the site renders itself — the follower lists, the moderation log. */
    onOpenPath: (path: String, title: String) -> Unit = { _, _ -> },
    /** A community the profile belongs to, opened as the app's own feed. */
    onOpenHole: (String) -> Unit = {},
    /** The followers/following tables, which the app draws itself. */
    onOpenRelations: (name: String, followers: Boolean) -> Unit = { _, _ -> },
    /** A post from any of the three tabs, opened natively. */
    onOpenPost: (permalink: String) -> Unit = {},
    /** An account named on a post card. */
    onOpenUser: (name: String) -> Unit = {},
) {
    val profiles by vm.state.collectAsStateWithLifecycle()
    val loading by vm.loading.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val vmMessage by vm.message.collectAsStateWithLifecycle()
    val postsByUser by vm.posts.collectAsStateWithLifecycle()
    val commentsByUser by vm.comments.collectAsStateWithLifecycle()
    val listLoading by vm.listLoading.collectAsStateWithLifecycle()

    var tab by remember(name) { mutableStateOf("wall") }
    LaunchedEffect(tab, name) {
        when (tab) {
            "posts" -> vm.loadPosts(name)
            "comments" -> vm.loadComments(name)
        }
    }

    LaunchedEffect(vmMessage) {
        vmMessage?.let { onMessage(it); vm.consumeMessage() }
    }

    LaunchedEffect(name) { vm.load(name) }
    val profile = profiles[name]

    val followersLabel = t("Takipçi")
    val followingLabel = t("Takip")
    val coinsLabel = t("Coin")
    val moderationLabel = t("Moderasyon geçmişi")
    val selfName = profile?.name ?: name

    when {
        loading && profile == null -> LoadingBox(Modifier.fillMaxSize())

        profile == null -> ErrorBox(
            error ?: "Profil yüklenemedi.",
            Modifier.fillMaxSize(),
            onRetry = { vm.load(name, force = true) },
        )

        else -> LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(DeliColors.Background),
            contentPadding = PaddingValues(
                start = 14.dp,
                end = 14.dp,
                top = 10.dp,
                bottom = contentPadding.calculateBottomPadding() + 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "header") {
                Box(modifier = Modifier.fillMaxWidth().kitSurface(padding = 0.dp)) {
                    Column {
                        // The account's own banner when there is one, the app's gradient
                        // when there is not — the site does the same.
                        val banner = profile.bannerUrl
                        if (banner != null) {
                            AsyncImage(
                                model = if (banner.startsWith("http")) banner else DeliHttp.BASE_URL + banner,
                                contentDescription = null,
                                modifier = Modifier.fillMaxWidth().height(96.dp),
                                contentScale = ContentScale.Crop,
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(96.dp)
                                    .background(
                                        Brush.linearGradient(
                                            listOf(
                                                DeliColors.Primary.copy(alpha = 0.5f),
                                                DeliColors.PrimaryContainer.copy(alpha = 0.3f),
                                                DeliColors.Surface,
                                            )
                                        )
                                    )
                            )
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .padding(bottom = 14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().pullUp(46.dp),
                                verticalAlignment = Alignment.Bottom,
                            ) {
                                // The ring has to follow the kit like the picture does:
                                // a square avatar under a round ring spilled out of it.
                                DeliAvatar(
                                    url = profile.avatarUrl,
                                    size = 86.dp,
                                    name = selfName,
                                    modifier = Modifier.border(
                                        3.dp,
                                        DeliColors.Surface,
                                        avatarShape(DeliColors.palette),
                                    )
                                )
                                Spacer(Modifier.weight(1f))
                                if (!isSelf && profile.isFollowing) {
                                    FollowingChip()
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "@${profile.name}",
                                    color = DeliColors.OnBackground,
                                    fontSize = 23.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                if (profile.operator) {
                                    Spacer(Modifier.width(7.dp))
                                    Icon(
                                        Icons.Filled.Verified,
                                        contentDescription = "Operatör",
                                        tint = DeliColors.Primary,
                                        modifier = Modifier.size(17.dp),
                                    )
                                }
                            }

                            profile.joinedAgo?.let {
                                Spacer(Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Filled.CalendarMonth,
                                        contentDescription = null,
                                        tint = DeliColors.OnSurfaceVariant,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    MutedText(tf("Katılım: {0}", it))
                                }
                            }

                            // Counters: both link to the list behind them.
                            Spacer(Modifier.height(8.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Counter(
                                    text = tf(
                                        "{0} {1}",
                                        profile.followers?.toString() ?: "0",
                                        followersLabel.lowercase(),
                                    ),
                                    onClick = if (profile.followers != null) {
                                        { onOpenRelations(profile.name, true) }
                                    } else null,
                                )
                                MutedText("·")
                                Counter(
                                    text = tf(
                                        "{0} {1}",
                                        profile.following?.toString() ?: "0",
                                        followingLabel.lowercase(),
                                    ),
                                    onClick = if (profile.following != null) {
                                        { onOpenRelations(profile.name, false) }
                                    } else null,
                                )
                                val coins = profile.stats.firstOrNull { it.key == "Coin" }?.value
                                    ?: profile.stats.firstOrNull { it.key.contains("Coin", true) }?.value
                                if (!coins.isNullOrBlank() && coins != "0") {
                                    MutedText("·")
                                    MutedText(tf("{0} {1}", coins, coinsLabel.lowercase()))
                                }
                            }

                            profile.bio?.takeIf { it.isNotBlank() }?.let { bioText ->
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = bioText,
                                    color = DeliColors.OnBackground,
                                    fontSize = 14.sp,
                                    lineHeight = 20.sp,
                                )
                            }

                            if (!isSelf) {
                                var blocked by remember(profile.name) { mutableStateOf(false) }
                                Spacer(Modifier.height(12.dp))
                                DeliButtonPrimary(
                                    text = t(if (profile.isFollowing) "Takip bırak" else "Takip et"),
                                    enabled = !busy,
                                ) { vm.follow(profile.name, !profile.isFollowing) }

                                if (profile.id.isNotBlank()) {
                                    Spacer(Modifier.height(8.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        DeliOutlinedButton(
                                            text = t(if (profile.muted) "Bildirimleri aç" else "Sessize al"),
                                            enabled = !busy,
                                            modifier = Modifier.weight(1f),
                                        ) { vm.mute(profile.name, profile.id, !profile.muted) }

                                        DeliOutlinedButton(
                                            text = t(if (blocked) "Engeli kaldır" else "Engelle"),
                                            enabled = !busy,
                                            modifier = Modifier.weight(1f),
                                        ) { vm.block(profile.name, !blocked) { ok -> if (ok) blocked = !blocked } }
                                    }
                                }
                            }

                            if (isSelf) {
                                Spacer(Modifier.height(12.dp))
                                Button(
                                    onClick = onEditProfile,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = DeliShapes.control,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = DeliColors.Primary,
                                        contentColor = Color.White,
                                    ),
                                ) {
                                    Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Profili Düzenle", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }

                            profile.id.takeIf { it.isNotBlank() }?.let { userId ->
                                Spacer(Modifier.height(10.dp))
                                MutedText(tf("Kullanıcı kimliği: {0}", userId))
                            }

                            profile.moderationHref?.let { href ->
                                Spacer(Modifier.height(6.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable { onOpenPath(href, moderationLabel) },
                                ) {
                                    Icon(
                                        Icons.Filled.History,
                                        contentDescription = null,
                                        tint = DeliColors.Blue,
                                        modifier = Modifier.size(15.dp),
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(moderationLabel, color = DeliColors.Blue, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }

            item(key = "tabs") {
                val postCount = profile.tabs.firstOrNull { it.href.endsWith("/posts") }?.count
                val commentCount = profile.tabs.firstOrNull { it.href.endsWith("/comments") }?.count
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ProfileTabChip(t("Duvar"), tab == "wall") { tab = "wall" }
                    ProfileTabChip(
                        t("Gönderiler") + (postCount?.let { " ($it)" } ?: ""),
                        tab == "posts",
                    ) { tab = "posts" }
                    ProfileTabChip(
                        t("Yorumlar") + (commentCount?.let { " ($it)" } ?: ""),
                        tab == "comments",
                    ) { tab = "comments" }
                }
            }

            if (tab == "wall") {
                val wall = profile.wallPosts
                if (wall.isEmpty()) {
                    item(key = "wall-empty") { EmptyBox(t("Henüz gönderi yok.")) }
                }
                items(wall, key = { "wall-" + it.id }) { post ->
                    PostCard(
                        post = post,
                        canVote = false,
                        onOpen = { onOpenPost(post.permalink) },
                        onOpenHole = { hole -> onOpenHole(hole) },
                        onOpenUser = onOpenUser,
                        onVote = {},
                        onShare = {},
                    )
                }
                if (profile.postDays.isNotEmpty()) {
                    item(key = "activity") {
                        DeliCard {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Filled.BarChart,
                                    contentDescription = null,
                                    tint = DeliColors.Primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    "Gönderi aktivitesi",
                                    color = DeliColors.OnBackground,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                            Spacer(Modifier.height(10.dp))

                            val max = profile.postDays.maxOf { it.count }.coerceAtLeast(1)
                            for (day in profile.postDays) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    MutedText(day.label, Modifier.width(92.dp))
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(9.dp)
                                            .clip(DeliShapes.small)
                                            .background(DeliColors.SurfaceVariant)
                                    ) {
                                        val fraction = (day.count.toFloat() / max).coerceIn(0.04f, 1f)
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth(fraction)
                                                .height(9.dp)
                                                .clip(DeliShapes.small)
                                                .background(
                                                    Brush.horizontalGradient(
                                                        listOf(DeliColors.Primary, DeliColors.Blue)
                                                    )
                                                )
                                        )
                                    }
                                    Spacer(Modifier.width(10.dp))
                                    Text(
                                        day.count.toString(),
                                        color = DeliColors.OnBackground,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.width(28.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                if (wall.isEmpty() && profile.bio.isNullOrBlank() && profile.postDays.isEmpty()) {
                    item(key = "empty") {
                        DeliCard {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 22.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Icon(
                                    Icons.Filled.Person,
                                    contentDescription = null,
                                    tint = DeliColors.OnSurfaceVariant,
                                    modifier = Modifier.size(34.dp)
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "Bu profil henüz ek bir ayrıntı paylaşmamış.",
                                    color = DeliColors.OnSurfaceVariant,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }

            if (tab == "posts") {
                val posts = postsByUser[name].orEmpty()
                if (listLoading && posts.isEmpty()) {
                    item(key = "posts-loading") { LoadingBox() }
                } else if (posts.isEmpty()) {
                    item(key = "posts-empty") { EmptyBox(t("Henüz gönderi yok.")) }
                } else {
                    items(posts, key = { "post-" + it.id }) { post ->
                        PostCard(
                            post = post,
                            canVote = false,
                            onOpen = { onOpenPost(post.permalink) },
                            onOpenHole = { hole -> onOpenHole(hole) },
                            onOpenUser = onOpenUser,
                            onVote = {},
                            onShare = {},
                        )
                    }
                }
            }

            if (tab == "comments") {
                val comments = commentsByUser[name].orEmpty()
                if (listLoading && comments.isEmpty()) {
                    item(key = "comments-loading") { LoadingBox() }
                } else if (comments.isEmpty()) {
                    item(key = "comments-empty") { EmptyBox(t("Henüz yorum yok.")) }
                } else {
                    items(comments, key = { "comment-" + it.id }) { comment ->
                        DeliCard {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    DeliAvatar(url = null, size = 20.dp, name = selfName)
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = comment.postLabel ?: t("Gönderi"),
                                        color = DeliColors.Blue,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.clickable(enabled = comment.postHref != null) {
                                            comment.postHref?.let(onOpenPost)
                                        },
                                    )
                                    Spacer(Modifier.weight(1f))
                                    MutedText(comment.ago)
                                }
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = comment.text,
                                    color = DeliColors.OnBackground,
                                    fontSize = 14.sp,
                                    lineHeight = 19.sp,
                                    maxLines = 8,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** One of the profile's three tabs: a selectable chip, not a link out of the app. */
@Composable
private fun ProfileTabChip(label: String, active: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(DeliShapes.control)
            .background(if (active) DeliColors.Primary else DeliColors.SurfaceVariant)
            .clickable(enabled = !active, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(
            text = label,
            color = if (active) Color.White else DeliColors.OnBackground,
            fontSize = 13.sp,
            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

/** A counter that is a link when the site has a page behind it. */
@Composable
private fun Counter(text: String, onClick: (() -> Unit)?) {
    Text(
        text = text,
        color = if (onClick != null) DeliColors.Blue else DeliColors.OnSurfaceVariant,
        fontSize = 13.sp,
        modifier = if (onClick != null) Modifier.clickable { onClick() } else Modifier,
    )
}

/** Only shown on somebody else's profile, when you already follow them. */
@Composable
private fun FollowingChip() {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(DeliColors.Primary.copy(alpha = 0.18f))
            .border(1.dp, DeliColors.Primary.copy(alpha = 0.4f), RoundedCornerShape(50))
            .padding(horizontal = 11.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Filled.Check,
            contentDescription = null,
            tint = DeliColors.Primary,
            modifier = Modifier.size(14.dp)
        )
        Spacer(Modifier.width(5.dp))
        Text("Takip Ediliyor", color = DeliColors.Primary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}
