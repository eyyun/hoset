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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.delidehset.mobile.data.Hole
import com.delidehset.mobile.data.HoleStore
import com.delidehset.mobile.ui.Text
import com.delidehset.mobile.ui.components.DeliCard
import com.delidehset.mobile.ui.components.EmptyBox
import com.delidehset.mobile.ui.components.ErrorBox
import com.delidehset.mobile.ui.components.LoadingBox
import com.delidehset.mobile.ui.components.MutedText
import com.delidehset.mobile.ui.components.rememberManualRefresh
import com.delidehset.mobile.ui.t
import com.delidehset.mobile.ui.tf
import com.delidehset.mobile.ui.theme.DeliColors
import com.delidehset.mobile.ui.theme.DeliShapes
import com.delidehset.mobile.vm.HolesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HolesScreen(
    vm: HolesViewModel,
    contentPadding: PaddingValues,
    onOpenHole: (String) -> Unit,
    onMessage: (String) -> Unit = {},
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val creating by vm.creating.collectAsStateWithLifecycle()
    val vmMessage by vm.message.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }
    var showCreate by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { vm.load() }
    LaunchedEffect(vmMessage) {
        vmMessage?.let {
            onMessage(it)
            vm.consumeMessage()
        }
    }

    // The spinner belongs to the person's own pull, not to the load this screen starts.
    val pull = rememberManualRefresh(state.loading)

    val filtered = remember(state.items, query) {
        val q = query.trim().removePrefix("/h/").removePrefix("h/")
        if (q.isEmpty()) state.items
        else state.items.filter { h ->
            h.name.contains(q, ignoreCase = true) ||
                (h.displayName ?: HoleStore.cachedLabel(h.name)).orEmpty().contains(q, ignoreCase = true)
        }
    }

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
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                "Topluluklar",
                                color = DeliColors.OnBackground,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f),
                            )
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(DeliColors.Primary)
                                    .clickable { showCreate = true }
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    t("Topluluk oluştur"),
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }
                    }
                    item {
                        OutlinedTextField(
                            value = query,
                            onValueChange = { query = it },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                            singleLine = true,
                            placeholder = { Text(t("Topluluk ara…"), color = DeliColors.OnSurfaceVariant) },
                            leadingIcon = {
                                Icon(Icons.Default.Search, contentDescription = null, tint = DeliColors.OnSurfaceVariant)
                            },
                            trailingIcon = {
                                if (query.isNotEmpty()) {
                                    IconButton(onClick = { query = "" }) {
                                        Icon(Icons.Default.Close, contentDescription = null, tint = DeliColors.OnSurfaceVariant)
                                    }
                                }
                            },
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
                    }
                    items(filtered, key = { it.name }) { hole: Hole ->
                        val label = hole.displayName ?: HoleStore.cachedLabel(hole.name)
                        DeliCard(onClick = { onOpenHole(hole.name) }) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                HoleMark(name = hole.name, logo = hole.logo)
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    ) {
                                        Text(
                                            text = label?.takeIf { it.isNotBlank() } ?: "/h/${hole.name}",
                                            color = DeliColors.Blue,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        if (!label.isNullOrBlank()) {
                                            Text(
                                                text = "/h/${hole.name}",
                                                color = DeliColors.OnSurfaceVariant,
                                                fontSize = 12.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                        }
                                        if (hole.inviteOnly) {
                                            Row(
                                                modifier = Modifier
                                                    .clip(DeliShapes.small)
                                                    .background(DeliColors.SurfaceVariant)
                                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                            ) {
                                                Icon(
                                                    Icons.Default.Lock,
                                                    contentDescription = null,
                                                    tint = DeliColors.OnSurfaceVariant,
                                                    modifier = Modifier.size(11.dp),
                                                )
                                                Spacer(Modifier.width(3.dp))
                                                Text(
                                                    t("Davetli"),
                                                    color = DeliColors.OnSurfaceVariant,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Medium,
                                                )
                                            }
                                        }
                                    }
                                    Spacer(Modifier.height(2.dp))
                                    Row {
                                        MutedText(tf("{0} gönderi", hole.postCount))
                                        hole.createdOn?.let { MutedText("  ·  $it") }
                                    }
                                }
                            }
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

    if (showCreate) {
        var holeName by remember { mutableStateOf("") }
        var holeTitle by remember { mutableStateOf("") }
        var holeDesc by remember { mutableStateOf("") }
        val cleanName = holeName.trim().removePrefix("/h/").removePrefix("h/")
        val validName = cleanName.length in 3..25 && cleanName.all { it.isLetterOrDigit() || it == '_' || it == '-' }

        AlertDialog(
            onDismissRequest = { if (!creating) showCreate = false },
            containerColor = DeliColors.Surface,
            title = { Text(t("Topluluk oluştur"), color = DeliColors.OnBackground, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = holeName,
                        onValueChange = { if (it.length <= 25) holeName = it },
                        singleLine = true,
                        label = { Text(t("Topluluk kodu (ör. yavsarlar)")) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = holeTitle,
                        onValueChange = { if (it.length <= 50) holeTitle = it },
                        singleLine = true,
                        label = { Text(t("Görünen ad")) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = holeDesc,
                        onValueChange = { if (it.length <= 200) holeDesc = it },
                        maxLines = 3,
                        label = { Text(t("Kısa açıklama")) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = validName && !creating,
                    onClick = {
                        vm.createHole(cleanName, holeTitle, holeDesc) { created ->
                            showCreate = false
                            onOpenHole(created)
                        }
                    },
                ) {
                    Text(
                        t(if (creating) "Kaydediliyor…" else "Oluştur"),
                        color = if (validName && !creating) DeliColors.Primary else DeliColors.OnSurfaceVariant,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            },
            dismissButton = {
                TextButton(enabled = !creating, onClick = { showCreate = false }) {
                    Text(t("Vazgeç"), color = DeliColors.OnSurfaceVariant)
                }
            },
        )
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
    HoleLogo(url = logo, name = name, size = 40.dp)
}

