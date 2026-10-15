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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
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
import com.delidehset.mobile.ui.components.EmptyBox
import com.delidehset.mobile.ui.components.ErrorBox
import com.delidehset.mobile.ui.components.LoadingBox
import com.delidehset.mobile.ui.components.MutedText
import com.delidehset.mobile.ui.components.ThinDivider
import com.delidehset.mobile.ui.components.VoteArrows
import com.delidehset.mobile.ui.theme.DeliColors
import com.delidehset.mobile.vm.PostViewModel

@Composable
fun PostScreen(
    vm: PostViewModel,
    permalink: String,
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

    LaunchedEffect(permalink) { vm.load(permalink) }
    LaunchedEffect(canVote) { vm.setVoting(canVote) }
    LaunchedEffect(state.message) {
        state.message?.let { onMessage(it); vm.consumeMessage() }
    }

    val flatComments = remember(state.comments) { flatten(state.comments) }

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
                                DeliChip("/h/$it", onClick = { onOpenHole(it) }, accent = true)
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
                            DeliAvatar(url = post.authorAvatar, size = 20.dp)
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
                                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.FillWidth,
                            )
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
                            contentDescription = "Şikâyet",
                            tint = DeliColors.OnSurfaceVariant,
                            modifier = Modifier.size(18.dp).clickable { showReport = true },
                        )
                        if (signedIn && viewerName != null && viewerName == post.authorName) {
                            Spacer(Modifier.width(14.dp))
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = "Sil",
                                tint = DeliColors.Danger,
                                modifier = Modifier.size(18.dp).clickable { showDelete = true },
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    ThinDivider()
                }

                // ---- composer ----
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        if (state.replyTo != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                MutedText(tf("@{0} kişisine yanıt yazıyorsun", state.replyTo?.authorName ?: "?"))
                                Spacer(Modifier.weight(1f))
                                Text(
                                    "İptal",
                                    color = DeliColors.Blue,
                                    fontSize = 12.sp,
                                    modifier = Modifier.clickable { vm.cancelReply() },
                                )
                            }
                        }
                        OutlinedTextField(
                            value = replyText,
                            onValueChange = { replyText = it },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2,
                            maxLines = 8,
                            placeholder = { Text("Bir yanıt yaz…", color = DeliColors.OnSurfaceVariant) },
                            shape = RoundedCornerShape(8.dp),
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
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (state.sending) DeliColors.SurfaceVariant else DeliColors.Primary)
                                        .clickable(enabled = !state.sending && replyText.isNotBlank()) {
                                            vm.sendComment(replyText)
                                            replyText = ""
                                        }
                                        .padding(horizontal = 18.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        if (state.sending) "Gönderiliyor…" else "Yanıtla",
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
                        CommentRow(
                            comment = comment,
                            depth = depth,
                            viewerVote = state.commentVotes[comment.id] ?: comment.viewerVote,
                            canVote = canVote,
                            onVote = { vm.voteComment(comment, it) },
                            onReply = { vm.startReply(comment) },
                            onOpenUser = onOpenUser,
                        )
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

@Composable
private fun CommentRow(
    comment: CommentNode,
    depth: Int,
    viewerVote: Int,
    canVote: Boolean,
    onVote: (Int) -> Unit,
    onReply: () -> Unit,
    onOpenUser: (String) -> Unit,
) {
    val rendered = remember(comment.bodyHtml) { DeliHtml.render(comment.bodyHtml) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (depth.coerceAtMost(6) * 14).dp)
            .clip(RoundedCornerShape(8.dp))
            .background(DeliColors.Surface)
            .padding(10.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                DeliAvatar(url = comment.authorAvatar, size = 18.dp)
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "@${comment.authorName ?: "anonim"}",
                    color = DeliColors.Blue,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.clickable { comment.authorName?.let(onOpenUser) },
                )
                comment.ago?.let { MutedText("  ·  $it") }
            }
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
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)),
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

/** Depth-first flattening so comments can live in a plain LazyColumn. */
private fun flatten(nodes: List<CommentNode>): List<Pair<CommentNode, Int>> {
    val out = mutableListOf<Pair<CommentNode, Int>>()
    fun walk(list: List<CommentNode>, depth: Int) {
        for (n in list) {
            out += n to depth
            walk(n.children, depth + 1)
        }
    }
    walk(nodes, 0)
    return out
}
