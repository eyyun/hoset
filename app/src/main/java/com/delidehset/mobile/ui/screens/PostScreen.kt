package com.delidehset.mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Reply
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import com.delidehset.mobile.ui.Text
import com.delidehset.mobile.ui.tf
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.delidehset.mobile.data.CommentNode
import com.delidehset.mobile.data.DeliHttp
import com.delidehset.mobile.ui.components.DeliAvatar
import com.delidehset.mobile.ui.components.DeliChip
import com.delidehset.mobile.ui.components.DeliHtml
import com.delidehset.mobile.ui.components.DeliVideoPlayer
import com.delidehset.mobile.ui.components.EmptyBox
import com.delidehset.mobile.ui.components.ErrorBox
import com.delidehset.mobile.ui.components.LoadingBox
import com.delidehset.mobile.ui.components.MutedText
import com.delidehset.mobile.ui.components.ThinDivider
import com.delidehset.mobile.ui.components.VoteArrows
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.border
import kotlinx.coroutines.delay
import com.delidehset.mobile.ui.theme.DeliColors
import com.delidehset.mobile.vm.PostViewModel
import com.delidehset.mobile.ui.theme.DeliShapes

@Composable
fun PostScreen(
    vm: PostViewModel,
    permalink: String,
    /** The comment to land on, when the post was opened from a profile's comment row. */
    highlightComment: String? = null,
    canVote: Boolean,
    signedIn: Boolean,
    viewerName: String?,
    contentPadding: PaddingValues,
    onOpenHole: (String) -> Unit,
    onOpenUser: (String) -> Unit,
    onOpenLogin: () -> Unit,
    onMessage: (String) -> Unit,
    onBack: () -> Unit,
) {
    val state by vm.state.collectAsStateWithLifecycle()
    var replyText by remember { mutableStateOf("") }
    var showReport by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }
    var zoomedImage by remember { mutableStateOf<String?>(null) }
    var collapsedIds by remember { mutableStateOf(setOf<String>()) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val clipboard = androidx.compose.ui.platform.LocalClipboardManager.current
    val copyLongPress = com.delidehset.mobile.data.HosetPrefs.copyOnLongPressState.value
    val showDownload = com.delidehset.mobile.data.HosetPrefs.mediaDownloadState.value
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()

    LaunchedEffect(permalink) {
        com.delidehset.mobile.data.HosetPrefs.markPostSeen(context, permalink)
        vm.load(permalink)
    }
    LaunchedEffect(canVote) { vm.setVoting(canVote) }
    LaunchedEffect(state.message) {
        state.message?.let { onMessage(it); vm.consumeMessage() }
    }

    // The thread on screen, plus the comment that was just sent if the page has not
    // caught up with it yet. Without the second part the app looked as if the comment
    // had gone nowhere: it is added at the end of a long thread, far below the box the
    // person had just typed into.
    val flatComments = remember(state.comments, state.posted, collapsedIds) {
        val real = flatten(state.comments, collapsedIds)
        val pending = state.posted
        if (pending == null || real.any { it.first.id == pending.id }) real
        else if (pending.parentId == null) real + (pending to 0)
        else {
            val parentAt = real.indexOfFirst { it.first.id == pending.parentId }
            if (parentAt < 0) {
                real + (pending to pending.depth)
            } else {
                // Straight after the subtree of the comment it answers.
                var end = parentAt + 1
                while (end < real.size && real[end].second > real[parentAt].second) end++
                real.toMutableList().apply { add(end, pending to pending.depth) }
            }
        }
    }

    // The first four rows are the header, the body, the actions and the composer; every
    // comment after them is one row of its own.
    LaunchedEffect(state.posted?.id) {
        val id = state.posted?.id ?: return@LaunchedEffect
        val index = flatComments.indexOfFirst { it.first.id == id }
        if (index >= 0) {
            runCatching { listState.animateScrollToItem(4 + index) }
        }
    }

    // Landing on a comment means finding it: the thread can be long, and a silent jump
    // somewhere in the middle of it reads as nothing having happened at all. The list goes
    // to the comment and marks it, and the mark comes off by itself a moment later.
    var lit by remember(highlightComment) { mutableStateOf(highlightComment) }
    LaunchedEffect(highlightComment, flatComments.size) {
        val wanted = highlightComment ?: return@LaunchedEffect
        val index = flatComments.indexOfFirst { it.first.id == wanted }
        if (index < 0) return@LaunchedEffect
        // The first four rows are the header, the body, the actions and the composer.
        runCatching { listState.animateScrollToItem(4 + index) }
        delay(2400)
        lit = null
    }

    when {
        state.loading && state.post == null -> LoadingBox(Modifier.fillMaxSize())

        state.post == null -> ErrorBox(
            state.error ?: "Gönderi yüklenemedi.",
            Modifier.fillMaxSize(),
            onRetry = { vm.load(permalink, force = true) },
        )

        else -> {
            val post = state.post!!
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().background(DeliColors.Background),
                contentPadding = PaddingValues(
                    start = 12.dp,
                    end = 12.dp,
                    top = 10.dp,
                    bottom = contentPadding.calculateBottomPadding() + 24.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            post.hole?.let {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable { onOpenHole(it) },
                                ) {
                                    HoleLogo(url = null, name = it, size = 22.dp)
                                    Spacer(Modifier.width(6.dp))
                                    val label = com.delidehset.mobile.data.HoleStore.cachedLabel(it)
                                        ?.let { l -> "$l · /h/$it" } ?: "/h/$it"
                                    DeliChip(label, onClick = { onOpenHole(it) }, accent = true)
                                }
                                Spacer(Modifier.width(8.dp))
                            }
                            post.flair?.let { DeliChip(it) }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = post.title,
                            color = DeliColors.OnBackground,
                            fontSize = 21.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            DeliAvatar(url = post.authorAvatar, size = 20.dp, name = post.authorName)
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "@${post.authorName ?: "anonim"}",
                                color = DeliColors.Blue,
                                fontSize = 13.sp,
                                modifier = Modifier.clickable { post.authorName?.let(onOpenUser) },
                            )
                            post.ago?.let {
                                MutedText("  ·  $it")
                            }
                            post.editedAgo?.let { MutedText("  ·  $it") }
                        }
                    }
                }

                // ---- body ----
                item {
                    val rendered = remember(post.bodyHtml) { DeliHtml.render(post.bodyHtml) }
                    Column {
                        Text(text = rendered.text, color = DeliColors.OnBackground, fontSize = 15.sp)
                        for (img in rendered.images) {
                            Spacer(Modifier.height(8.dp))
                            AsyncImage(
                                model = img,
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(DeliShapes.control)
                                    .clickable { zoomedImage = img },
                                contentScale = ContentScale.FillWidth,
                            )
                        }
                        for (video in rendered.videos) {
                            Spacer(Modifier.height(8.dp))
                            DeliVideoPlayer(url = video)
                            if (com.delidehset.mobile.data.HosetPrefs.mediaDownloadState.value) {
                                val vCtx = androidx.compose.ui.platform.LocalContext.current
                                Spacer(Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier
                                        .clip(DeliShapes.small)
                                        .background(DeliColors.SurfaceVariant)
                                        .clickable {
                                            val ok = com.delidehset.mobile.data.HosetPrefs.downloadMedia(vCtx, video)
                                            android.widget.Toast.makeText(
                                                vCtx,
                                                if (ok) com.delidehset.mobile.ui.tr("İndirme başlatıldı.") else com.delidehset.mobile.ui.tr("İndirilemedi."),
                                                android.widget.Toast.LENGTH_SHORT,
                                            ).show()
                                        }
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(com.delidehset.mobile.ui.t("İndir"), color = DeliColors.Primary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }

                // ---- post actions ----
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        VoteArrows(
                            score = post.score,
                            viewerVote = post.viewerVote,
                            canVote = canVote,
                            onVote = { vm.votePost(it) },
                        )
                        Spacer(Modifier.width(16.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.Reply,
                                contentDescription = null,
                                tint = DeliColors.OnSurfaceVariant,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(tf("{0} yorum", post.commentCount), color = DeliColors.OnSurfaceVariant, fontSize = 12.sp)
                        }
                        Spacer(Modifier.weight(1f))
                        Icon(
                            Icons.Filled.Flag,
                            contentDescription = com.delidehset.mobile.ui.t("Şikâyet"),
                            tint = DeliColors.OnSurfaceVariant,
                            modifier = Modifier.size(18.dp).clickable { showReport = true },
                        )
                        if (signedIn && viewerName != null && viewerName == post.authorName) {
                            Spacer(Modifier.width(14.dp))
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = com.delidehset.mobile.ui.t("Sil"),
                                tint = DeliColors.Danger,
                                modifier = Modifier.size(18.dp).clickable { showDelete = true },
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    ThinDivider()
                }

                // ---- composer ----
                // This box is the post's own comment box, always. A reply started under a
                // comment gets its own small box there, so the line written here never
                // silently becomes an answer to whoever was tapped last.
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = replyText,
                            onValueChange = { replyText = it },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2,
                            maxLines = 8,
                            placeholder = { Text("Bir yorum yaz…", color = DeliColors.OnSurfaceVariant) },
                            shape = DeliShapes.control,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = DeliColors.Surface,
                                unfocusedContainerColor = DeliColors.Surface,
                                focusedTextColor = DeliColors.OnBackground,
                                unfocusedTextColor = DeliColors.OnBackground,
                                cursorColor = DeliColors.Primary,
                                focusedIndicatorColor = DeliColors.Primary,
                                unfocusedIndicatorColor = DeliColors.Border,
                            ),
                        )
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                            if (!signedIn) {
                                Text(
                                    "Yorum yazmak için oturum aç",
                                    color = DeliColors.Warning,
                                    fontSize = 12.sp,
                                    modifier = Modifier.clickable { onOpenLogin() },
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .clip(DeliShapes.small)
                                        .background(if (state.sending) DeliColors.SurfaceVariant else DeliColors.Primary)
                                        .clickable(enabled = !state.sending && replyText.isNotBlank()) {
                                            vm.sendComment(replyText, viewerName, topLevel = true)
                                            replyText = ""
                                        }
                                        .padding(horizontal = 18.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        if (state.sending) "Gönderiliyor…" else "Yorum yap",
                                        color = androidx.compose.ui.graphics.Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }
                            }
                        }
                    }
                }

                if (flatComments.isEmpty()) {
                    item { EmptyBox("Henüz yorum yok. İlk yorumu sen yaz.") }
                } else {
                    items(flatComments, key = { it.first.id }) { (comment, depth) ->
                        val collapsed = comment.id in collapsedIds
                        Column {
                            CommentRow(
                                comment = comment,
                                depth = depth,
                                collapsed = collapsed,
                                highlighted = lit != null && comment.id == lit,
                                viewerVote = state.commentVotes[comment.id] ?: comment.viewerVote,
                                canVote = canVote,
                                onVote = { vm.voteComment(comment, it) },
                                onReply = {
                                    replyText = ""
                                    vm.startReply(comment)
                                },
                                onToggleCollapse = {
                                    collapsedIds = if (collapsed) collapsedIds - comment.id else collapsedIds + comment.id
                                },
                                onCopy = { text ->
                                    clipboard.setText(androidx.compose.ui.text.AnnotatedString(text))
                                    onMessage(com.delidehset.mobile.ui.tr("Yorum kopyalandı."))
                                },
                                onOpenImage = { zoomedImage = it },
                                onOpenUser = onOpenUser,
                            )
                            // The answer is written where it will appear: directly under
                            // the comment it answers, not in the box at the top of the
                            // thread where it could be mistaken for a new comment.
                            if (!collapsed && state.replyTo?.id == comment.id) {
                                InlineReplyBox(
                                    name = comment.authorName ?: "?",
                                    text = replyText,
                                    onText = { replyText = it },
                                    sending = state.sending,
                                    signedIn = signedIn,
                                    onCancel = {
                                        replyText = ""
                                        vm.cancelReply()
                                    },
                                    onSend = {
                                        vm.sendComment(replyText, viewerName)
                                        replyText = ""
                                    },
                                    onOpenLogin = onOpenLogin,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    zoomedImage?.let { imgUrl ->
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { zoomedImage = null },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.94f))
                    .clickable { zoomedImage = null },
                contentAlignment = Alignment.Center,
            ) {
                AsyncImage(
                    model = imgUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    contentScale = ContentScale.Fit,
                )

                // Top bar with close and download buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.6f))
                            .clickable { zoomedImage = null },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = com.delidehset.mobile.ui.t("Kapat"),
                            tint = Color.White,
                            modifier = Modifier.size(22.dp),
                        )
                    }

                    if (showDownload) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(DeliColors.Primary)
                                .clickable {
                                    val ok = com.delidehset.mobile.data.HosetPrefs.downloadMedia(context, imgUrl)
                                    onMessage(com.delidehset.mobile.ui.tr(if (ok) "İndirme başlatıldı." else "İndirilemedi."))
                                }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Filled.Download,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = com.delidehset.mobile.ui.t("İndir"),
                                color = Color.White,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }
        }
    }

    if (showReport) {
        var reason by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showReport = false },
            containerColor = DeliColors.Surface,
            title = { Text("Gönderiyi şikâyet et", color = DeliColors.OnBackground) },
            text = {
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    placeholder = { Text("Sebep", color = DeliColors.OnSurfaceVariant) },
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = DeliColors.OnBackground,
                        unfocusedTextColor = DeliColors.OnBackground,
                        focusedContainerColor = DeliColors.Background,
                        unfocusedContainerColor = DeliColors.Background,
                    ),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.reportPost(reason.ifBlank { "kural ihlali" }, platform = false)
                    showReport = false
                }) { Text("Hole moderatörlerine", color = DeliColors.Blue) }
            },
            dismissButton = {
                TextButton(onClick = { showReport = false }) { Text("Vazgeç", color = DeliColors.OnSurfaceVariant) }
            },
        )
    }

    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            containerColor = DeliColors.Surface,
            title = { Text("Gönderi silinsin mi?", color = DeliColors.OnBackground) },
            confirmButton = {
                TextButton(onClick = { vm.deletePost(); showDelete = false; onBack() }) {
                    Text("Sil", color = DeliColors.Danger)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDelete = false }) { Text("Vazgeç", color = DeliColors.OnSurfaceVariant) }
            },
        )
    }
}

/** `805ad5` or `#805ad5` as the site writes it, or null when it wrote nothing usable. */
private fun siteColor(raw: String?): Color? {
    val hex = raw?.trim()?.removePrefix("#")?.takeIf { it.length == 6 || it.length == 8 } ?: return null
    val value = hex.toLongOrNull(16) ?: return null
    return Color(if (hex.length == 6) value or 0xFF000000L else value)
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun CommentRow(
    comment: CommentNode,
    depth: Int,
    collapsed: Boolean = false,
    /** The comment the screen was opened for: its card is outlined until the moment passes. */
    highlighted: Boolean = false,
    viewerVote: Int,
    canVote: Boolean,
    onVote: (Int) -> Unit,
    onReply: () -> Unit,
    onToggleCollapse: () -> Unit = {},
    onCopy: (String) -> Unit = {},
    onOpenImage: (String) -> Unit = {},
    onOpenUser: (String) -> Unit,
) {
    val rendered = remember(comment.bodyHtml) { DeliHtml.render(comment.bodyHtml) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (depth.coerceAtMost(6) * 14).dp)
            .clip(DeliShapes.control)
            .then(
                if (highlighted) Modifier.border(1.5.dp, DeliColors.Primary, DeliShapes.control)
                else Modifier
            )
            .background(if (highlighted) DeliColors.Primary.copy(alpha = 0.12f) else DeliColors.Surface)
            .combinedClickable(
                onClick = { if (collapsed) onToggleCollapse() },
                onLongClick = { onCopy(rendered.text.text) },
            )
            .padding(10.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                DeliAvatar(
                    url = comment.authorAvatar,
                    size = 20.dp,
                    name = comment.authorName,
                    // The site keeps no picture for a comment's author, so the app's own
                    // initial stands in — in the colour the site already gives that name.
                    accent = siteColor(comment.authorColor),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "@${comment.authorName ?: "anonim"}",
                    color = DeliColors.Blue,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.clickable { comment.authorName?.let(onOpenUser) },
                )
                comment.ago?.let { MutedText("  ·  $it") }
                Spacer(Modifier.weight(1f))
                Text(
                    text = if (collapsed) "[+${1 + comment.children.size}]" else "[−]",
                    color = DeliColors.OnSurfaceVariant,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .clip(DeliShapes.small)
                        .clickable { onToggleCollapse() }
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
            if (!collapsed) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = rendered.text,
                    color = if (comment.isDeleted) DeliColors.OnSurfaceVariant else DeliColors.OnBackground,
                    fontSize = 14.sp,
                )
                for (img in rendered.images) {
                    Spacer(Modifier.height(6.dp))
                    AsyncImage(
                        model = img,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(DeliShapes.small)
                            .clickable { onOpenImage(img) },
                        contentScale = ContentScale.FillWidth,
                    )
                }
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    VoteArrows(
                        score = comment.score,
                        viewerVote = viewerVote,
                        canVote = canVote,
                        onVote = onVote,
                        compact = true,
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Yanıtla",
                        color = DeliColors.OnSurfaceVariant,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.clickable { onReply() },
                    )
                }
            }
        }
    }
}

/**
 * The reply box that opens under the comment it answers.
 *
 * It is deliberately small and attached to the row: the box at the top of the thread
 * stays a post-level comment box, and a reply can never be sent to the wrong parent
 * because the parent is the row this box was opened on.
 */
@Composable
private fun InlineReplyBox(
    name: String,
    text: String,
    onText: (String) -> Unit,
    sending: Boolean,
    signedIn: Boolean,
    onCancel: () -> Unit,
    onSend: () -> Unit,
    onOpenLogin: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp, bottom = 2.dp)
            .clip(DeliShapes.control)
            .background(DeliColors.Surface.copy(alpha = 0.65f))
            .padding(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            MutedText(tf("@{0} kişisine yanıt", name))
            Spacer(Modifier.weight(1f))
            Text(
                "İptal",
                color = DeliColors.Blue,
                fontSize = 12.sp,
                modifier = Modifier.clickable { onCancel() },
            )
        }
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value = text,
            onValueChange = onText,
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            maxLines = 6,
            placeholder = { Text("Bir yanıt yaz…", color = DeliColors.OnSurfaceVariant) },
            shape = DeliShapes.control,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = DeliColors.Background,
                unfocusedContainerColor = DeliColors.Background,
                focusedTextColor = DeliColors.OnBackground,
                unfocusedTextColor = DeliColors.OnBackground,
                cursorColor = DeliColors.Primary,
                focusedIndicatorColor = DeliColors.Primary,
                unfocusedIndicatorColor = DeliColors.Border,
            ),
        )
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
            if (!signedIn) {
                Text(
                    "Yorum yazmak için oturum aç",
                    color = DeliColors.Warning,
                    fontSize = 12.sp,
                    modifier = Modifier.clickable { onOpenLogin() },
                )
            } else {
                Box(
                    modifier = Modifier
                        .clip(DeliShapes.small)
                        .background(if (sending) DeliColors.SurfaceVariant else DeliColors.Primary)
                        .clickable(enabled = !sending && text.isNotBlank()) { onSend() }
                        .padding(horizontal = 14.dp, vertical = 7.dp),
                ) {
                    Text(
                        if (sending) "Gönderiliyor…" else "Yanıtla",
                        color = androidx.compose.ui.graphics.Color.White,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

/** Depth-first flattening so comments can live in a plain LazyColumn. */
private fun flatten(
    nodes: List<CommentNode>,
    collapsedIds: Set<String> = emptySet(),
): List<Pair<CommentNode, Int>> {
    val out = mutableListOf<Pair<CommentNode, Int>>()
    fun walk(list: List<CommentNode>, depth: Int) {
        for (n in list) {
            out += n to depth
            if (n.id !in collapsedIds) {
                walk(n.children, depth + 1)
            }
        }
    }
    walk(nodes, 0)
    return out
}
