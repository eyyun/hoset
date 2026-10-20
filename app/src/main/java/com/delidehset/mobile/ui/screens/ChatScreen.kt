package com.delidehset.mobile.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.HeadsetOff
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import com.delidehset.mobile.ui.Text
import com.delidehset.mobile.ui.t
import com.delidehset.mobile.ui.tf
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.delidehset.mobile.AppContainer
import com.delidehset.mobile.data.ChatMessage
import com.delidehset.mobile.data.VoiceState
import com.delidehset.mobile.data.VoiceUser
import com.delidehset.mobile.ui.CallAudio
import com.delidehset.mobile.ui.components.DeliAvatar
import com.delidehset.mobile.ui.components.DeliButtonPrimary
import com.delidehset.mobile.ui.components.DeliHtml
import com.delidehset.mobile.ui.components.ErrorBox
import com.delidehset.mobile.ui.theme.DeliColors
import com.delidehset.mobile.ui.theme.nameColor
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.delidehset.mobile.ui.theme.parseCssColor
import com.delidehset.mobile.ui.theme.DeliShapes

/**
 * The chat room, drawn entirely natively: message list, avatars, reactions, online count,
 * typing indicator, composer and the voice panel are Compose. The websocket — and the
 * voice call with it — lives in a WebView laid out one pixel tall behind the list (see
 * [ChatBridge]); that page is the site's own client speaking its binary protocol, which is
 * what keeps this screen honest about being connected without re-implementing it here.
 */
@Composable
fun ChatScreen(
    container: AppContainer,
    signedIn: Boolean,
    viewerName: String?,
    contentPadding: PaddingValues,
    onOpenLogin: () -> Unit,
    /** Tapping someone's picture or name in the room opens their profile. */
    onOpenUser: (String) -> Unit = {},
) {
    val context = LocalContext.current
    // The room belongs to the app, not to this screen: leaving Sohbet no longer tears the
    // socket down, so coming back shows the conversation at once instead of an empty room
    // that fills in over the next few seconds. `container.chat` is where that lives.
    val session = remember { container.chat }
    val room by session.state.collectAsStateWithLifecycle()
    val status by session.status.collectAsStateWithLifecycle()
    val chat = room
    var voiceOpen by remember { mutableStateOf(false) }
    var micDenied by remember { mutableStateOf(false) }

    // Tell the room which account this is, twice: the frame is usually still loading when
    // this screen opens, and the call reports its participants by name.
    LaunchedEffect(viewerName) {
        if (viewerName.isNullOrBlank()) return@LaunchedEffect
        session.hello(viewerName)
        delay(2500)
        session.hello(viewerName)
    }

    // The call's audio path: communication mode is what turns the platform's echo canceller
    // on, and unlike leaving it alone it is also what stops the microphone from playing the
    // loudspeaker — and the speaker from reaching the microphone — back at you.
    LaunchedEffect(chat.voice.joined) {
        if (chat.voice.joined) CallAudio.enter(context) else CallAudio.leave(context)
    }

    DisposableEffect(Unit) {
        session.onShown()
        onDispose {
            // Leaving the screen ends the call — a hot microphone behind a closed screen
            // would be the worst possible surprise — but not the room: the socket stays up
            // so the next visit is instant. The app closing is what ends it (see
            // MainActivity.onStop).
            if (session.state.value.voice.joined) session.voice(join = false)
            CallAudio.leave(context)
            session.onHidden()
        }
    }

    // Joining the call is what needs the microphone, so the permission is asked for there
    // and the join goes through only once it is granted.
    val askMic = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            micDenied = false
            session.voice(join = true)
        } else {
            micDenied = true
        }
    }

    fun joinVoice() {
        val granted = ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
        android.util.Log.d("DeliChat", "joinVoice: micPermission=$granted")
        if (granted) {
            micDenied = false
            session.voice(join = true)
        } else {
            askMic.launch(android.Manifest.permission.RECORD_AUDIO)
        }
    }

    if (!signedIn) {
        Column(
            modifier = Modifier.fillMaxSize().background(DeliColors.Background).padding(16.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            ErrorBox("Sohbet odasına girmek için oturum açmalısın.")
            Spacer(Modifier.height(10.dp))
            DeliButtonPrimary("Oturum aç") { onOpenLogin() }
        }
        return
    }

    val listState = rememberLazyListState()
    var draft by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    // Whether the reader is at the bottom of the room, which decides two things: whether
    // new lines should pull the list along, and whether the "back to the newest" button is
    // worth showing.
    //
    // "At the bottom" means the last line is actually on screen. The first version asked
    // whether the last *visible* index was within one of the end, which is false for a room
    // whose whole list fits on screen — so a short room, and any room on first open, looked
    // as though the reader had scrolled away and the list simply stopped following the new
    // lines. Nothing follows a list that thinks it is not at the end of itself.
    val atBottom by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            if (info.totalItemsCount == 0) return@derivedStateOf true
            val last = info.visibleItemsInfo.lastOrNull() ?: return@derivedStateOf true
            last.index >= info.totalItemsCount - 1
        }
    }

    // The room opens on its newest line, whatever the list thought before the first layout
    // had happened.
    var openedOnNewest by remember { mutableStateOf(false) }

    // New lines appear at the bottom, so the list follows them — but only once this room's
    // own list has been placed at the bottom, and after that only while the reader is
    // still there. Yanking someone out of the message they were reading is the classic way
    // to make a room feel hostile.
    LaunchedEffect(chat.messages.size) {
        if (chat.messages.isEmpty()) return@LaunchedEffect
        if (!openedOnNewest) {
            listState.scrollToItem(chat.messages.lastIndex)
            openedOnNewest = true
            return@LaunchedEffect
        }
        if (atBottom) listState.animateScrollToItem(chat.messages.lastIndex)
    }

    // Writing with the keyboard up means the newest line is the one behind it.
    LaunchedEffect(draft.isNotEmpty()) {
        if (draft.isNotEmpty() && chat.messages.isNotEmpty()) {
            listState.animateScrollToItem(chat.messages.lastIndex)
        }
    }

    // Opening the keyboard shrinks the room without adding a line, so nothing above moved
    // the list: the newest lines stayed behind the keyboard and the screen looked stuck
    // wherever it happened to be. Follow the bottom edge itself, not just new text, and
    // wait for the keyboard to finish sliding so the last line is measured where it lands.
    val keyboardUp = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    LaunchedEffect(keyboardUp) {
        if (keyboardUp && chat.messages.isNotEmpty()) {
            kotlinx.coroutines.delay(180)
            listState.animateScrollToItem(chat.messages.lastIndex)
        }
    }

    // The scaffold above already lifts the whole screen for the keyboard.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeliColors.Background),
    ) {
        // The transport itself: never visible, never touched by the user. The screen owns
        // an empty host and the session moves its WebView into it — so a room that was
        // closed in the background comes back as a working room instead of a blank one,
        // and the same view is never handed to two parents.
        Box(Modifier.height(1.dp).fillMaxWidth()) {
            AndroidView(
                modifier = Modifier.size(1.dp),
                factory = { hostContext ->
                    android.widget.FrameLayout(hostContext).also { host ->
                        session.attach(host, viewerName)
                    }
                },
                update = { host -> session.attach(host, viewerName) },
                onRelease = { host -> host.removeAllViews() },
            )
        }

        var showOnline by remember { mutableStateOf(false) }
        val stealthOnline by com.delidehset.mobile.data.HosetPrefs.stealthOnlineState
        val showHiddenInfo by com.delidehset.mobile.data.HosetPrefs.showHiddenInfoState

        LaunchedEffect(stealthOnline) {
            if (!viewerName.isNullOrBlank()) {
                session.hello(viewerName)
            }
        }

        RoomHeader(
            online = chat.online,
            chatCount = chat.chatUsers.size,
            siteCount = chat.siteUsers.size,
            stealth = stealthOnline,
            typing = chat.typing,
            status = status,
            voice = chat.voice,
            onJoinVoice = { joinVoice() },
            onOpenVoicePanel = { voiceOpen = !voiceOpen },
            onToggleMic = { session.voice(mic = !chat.voice.muted) },
            onLeaveVoice = { session.voice(join = false) },
            onOpenOnline = { showOnline = true },
        )

        if (showOnline) {
            val chatList = if (chat.chatUsers.isNotEmpty()) {
                chat.chatUsers
            } else {
                chat.online.map { com.delidehset.mobile.data.OnlineUser(name = it) }
            }
            val siteList = chat.siteUsers
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showOnline = false },
                containerColor = DeliColors.Surface,
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            t("Çevrimiçi Kişiler"),
                            color = DeliColors.OnBackground,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                        )
                    }
                },
                text = {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(DeliShapes.control)
                                    .background(DeliColors.Background)
                                    .clickable {
                                        com.delidehset.mobile.data.HosetPrefs.setStealthOnline(context, !stealthOnline)
                                    }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        t("Çevrimiçi durumunu gizle (Hayalet mod)"),
                                        color = DeliColors.OnBackground,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    Text(
                                        if (stealthOnline) t("Sohbette gizlisin") else t("Sohbette görünürsün"),
                                        color = if (stealthOnline) DeliColors.Success else DeliColors.OnSurfaceVariant,
                                        fontSize = 11.sp,
                                    )
                                }
                                androidx.compose.material3.Switch(
                                    checked = stealthOnline,
                                    onCheckedChange = {
                                        com.delidehset.mobile.data.HosetPrefs.setStealthOnline(context, it)
                                    },
                                )
                            }
                        }

                        item {
                            Spacer(Modifier.height(4.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Icon(
                                    Icons.Filled.Forum,
                                    contentDescription = null,
                                    tint = DeliColors.Success,
                                    modifier = Modifier.size(15.dp),
                                )
                                Text(
                                    tf("Sohbette ({0})", chatList.size),
                                    color = DeliColors.OnSurfaceVariant,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }

                        itemsIndexed(chatList) { _, u ->
                            OnlineUserRow(
                                user = u,
                                showPingMs = showHiddenInfo,
                                onClick = {
                                    showOnline = false
                                    onOpenUser(u.name)
                                },
                            )
                        }

                        if (siteList.isNotEmpty()) {
                            item {
                                Spacer(Modifier.height(8.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Icon(
                                        Icons.Filled.Public,
                                        contentDescription = null,
                                        tint = DeliColors.Blue,
                                        modifier = Modifier.size(15.dp),
                                    )
                                    Text(
                                        tf("Sitede Çevrimiçi ({0})", siteList.size),
                                        color = DeliColors.OnSurfaceVariant,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }

                            itemsIndexed(siteList) { _, u ->
                                OnlineUserRow(
                                    user = u,
                                    showPingMs = showHiddenInfo,
                                    onClick = {
                                        showOnline = false
                                        onOpenUser(u.name)
                                    },
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    androidx.compose.material3.TextButton(onClick = { showOnline = false }) {
                        Text(t("Kapat"), color = DeliColors.Primary)
                    }
                },
            )
        }

        AnimatedVisibility(
            visible = voiceOpen,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            // Loudspeaker or earpiece: the one setting that decides whether an open
            // microphone picks the room back up. Ear by default (see CallAudio); this is
            // here for anyone who would rather have the room on the loudspeaker.
            var speakerOn by remember { mutableStateOf(false) }
            VoicePanel(
                voice = chat.voice,
                micDenied = micDenied,
                speakerOn = speakerOn,
                onJoin = { joinVoice() },
                onToggleDeafen = { session.voice(deafen = !chat.voice.deafened) },
                onToggleSpeaker = {
                    speakerOn = !speakerOn
                    CallAudio.setSpeaker(context, speakerOn)
                },
                onOpenUser = onOpenUser,
            )
        }

        when {
            // Nothing has arrived yet: the room is still opening, so say so instead of
            // flashing an empty room that then fills in.
            chat.messages.isEmpty() && status != "Bağlı" -> Box(
                Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    androidx.compose.material3.CircularProgressIndicator(
                        color = DeliColors.Primary,
                        strokeWidth = 3.dp,
                    )
                    Spacer(Modifier.height(12.dp))
                    Text("Odaya bağlanılıyor…", color = DeliColors.OnSurfaceVariant, fontSize = 13.sp)
                }
            }

            chat.messages.isEmpty() -> Box(
                Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "Oda boş. İlk mesajı sen yaz.",
                    color = DeliColors.OnSurfaceVariant,
                    fontSize = 14.sp,
                )
            }

            else -> Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp),
                ) {
                    itemsIndexed(chat.messages, key = { _, message -> message.id }) { index, message ->
                        // Consecutive lines from one person read as one turn: the name and
                        // the picture are only worth repeating when the speaker changes.
                        val previous = chat.messages.getOrNull(index - 1)
                        ChatRow(
                            message = message,
                            own = viewerName != null && message.author.equals(viewerName, ignoreCase = true),
                            startsTurn = previous == null ||
                                !previous.author.equals(message.author, ignoreCase = true),
                            onOpenUser = onOpenUser,
                        )
                    }
                }

                // Reading back through the room should not mean scrolling a thousand
                // lines by hand to get back to now.
                if (!atBottom && chat.messages.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(12.dp)
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(DeliColors.Primary)
                            .clickable {
                                scope.launch { listState.animateScrollToItem(chat.messages.lastIndex) }
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Filled.KeyboardArrowDown,
                            contentDescription = com.delidehset.mobile.ui.t("En yeni mesaja in"),
                            tint = Color.White,
                            modifier = Modifier.size(21.dp),
                        )
                    }
                }
            }
        }

        // With the keyboard up the scaffold already ends the content at the keyboard's top
        // edge — and the bottom bar is behind the keyboard — so the composer asks for no
        // extra room of its own. Padding it again is what left a strip of empty space
        // between the composer and the keyboard.
        val density = LocalDensity.current
        val imeBottom = WindowInsets.ime.getBottom(density)
        Composer(
            draft = draft,
            onDraftChange = { draft = it },
            onSend = {
                val text = draft
                draft = ""
                session.send(text)
            },
            bottomInset = if (imeBottom > 0) 0.dp else contentPadding.calculateBottomPadding(),
        )
    }
}

@Composable
private fun OnlineUserRow(
    user: com.delidehset.mobile.data.OnlineUser,
    showPingMs: Boolean,
    onClick: () -> Unit,
) {
    val userColor = remember(user.name, user.color) {
        parseCssColor(user.color)?.takeIf { it != DeliColors.Primary } ?: nameColor(user.name)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(DeliShapes.small)
            .clickable(onClick = onClick)
            .padding(vertical = 5.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DeliAvatar(url = user.avatarUrl, name = user.name, size = 30.dp, accent = userColor)
        Spacer(Modifier.width(10.dp))
        Text(
            "@${user.name}",
            color = userColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            modifier = Modifier.weight(1f),
        )
        if (!user.nameplate.isNullOrBlank()) {
            Text(
                user.nameplate,
                color = DeliColors.Primary,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .padding(end = 6.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(DeliColors.Primary.copy(alpha = 0.14f))
                    .padding(horizontal = 5.dp, vertical = 2.dp),
            )
        }
        if (showPingMs && user.rtt >= 0) {
            Text(
                "${user.rtt} ms",
                color = DeliColors.OnSurfaceVariant,
                fontSize = 10.sp,
                modifier = Modifier.padding(end = 6.dp),
            )
        }
        SignalBars(rtt = user.rtt)
    }
}

@Composable
private fun SignalBars(rtt: Int) {
    // Matches the site's 4-bar signal indicator in the chat sidebar
    val activeBars = when {
        rtt < 0 -> 3
        rtt < 90 -> 4
        rtt < 180 -> 3
        rtt < 320 -> 2
        else -> 1
    }
    val barColor = when (activeBars) {
        4, 3 -> Color(0xFF3FB950)
        2 -> Color(0xFFD29922)
        else -> Color(0xFFF85149)
    }
    val inactiveColor = DeliColors.OnSurfaceVariant.copy(alpha = 0.25f)
    val heights = listOf(4.dp, 7.dp, 10.dp, 13.dp)
    Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier.height(14.dp),
    ) {
        for (i in 0 until 4) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(heights[i])
                    .clip(RoundedCornerShape(1.dp))
                    .background(if (i < activeBars) barColor else inactiveColor),
            )
        }
    }
}

@Composable
private fun RoomHeader(
    online: List<String>,
    chatCount: Int = 0,
    siteCount: Int = 0,
    stealth: Boolean = false,
    typing: String,
    status: String,
    voice: VoiceState,
    onJoinVoice: () -> Unit,
    onOpenVoicePanel: () -> Unit,
    onToggleMic: () -> Unit,
    onLeaveVoice: () -> Unit,
    onOpenOnline: () -> Unit = {},
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DeliColors.Surface)
            .padding(start = 14.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .clickable { onOpenOnline() }
                .padding(vertical = 8.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    modifier = Modifier
                        .clip(DeliShapes.pill)
                        .background(DeliColors.Success.copy(alpha = 0.14f))
                        .padding(horizontal = 7.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Filled.Forum,
                        contentDescription = t("Sohbet"),
                        tint = DeliColors.Success,
                        modifier = Modifier.size(12.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = chatCount.toString(),
                        color = DeliColors.OnBackground,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }

                if (siteCount > 0) {
                    Row(
                        modifier = Modifier
                            .clip(DeliShapes.pill)
                            .background(DeliColors.Blue.copy(alpha = 0.14f))
                            .padding(horizontal = 7.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Filled.Public,
                            contentDescription = t("Çevrimiçi"),
                            tint = DeliColors.Blue,
                            modifier = Modifier.size(12.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = siteCount.toString(),
                            color = DeliColors.OnBackground,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }

                if (stealth) {
                    Row(
                        modifier = Modifier
                            .clip(DeliShapes.pill)
                            .background(DeliColors.Primary.copy(alpha = 0.16f))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Filled.VisibilityOff,
                            contentDescription = null,
                            tint = DeliColors.Primary,
                            modifier = Modifier.size(12.dp),
                        )
                    }
                }

                if (online.isNotEmpty()) {
                    Text(
                        text = online.joinToString(", "),
                        color = DeliColors.OnSurfaceVariant,
                        fontSize = 11.5.sp,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                }
            }
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                // While you are in the call this line is the answer to the only question
                // that matters — am I on air — said in the microphone's own words, in
                // the microphone's own colour. A dot and a count never answered it.
                if (voice.joined) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(if (voice.muted) DeliColors.Danger else DeliColors.Success)
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        if (voice.muted) "Seste • mikrofon kapalı" else "Seste • mikrofon açık",
                        color = if (voice.muted) DeliColors.Danger else DeliColors.Success,
                        fontSize = 11.sp,
                        maxLines = 1,
                    )
                } else {
                    if (voice.count > 0) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(DeliColors.Success)
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            tf("Seste {0} kişi", voice.count),
                            color = DeliColors.Success,
                            fontSize = 11.sp,
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(
                        text = if (typing.isBlank()) status else typing,
                        color = if (typing.isBlank()) DeliColors.OnSurfaceVariant else DeliColors.Primary,
                        fontSize = 11.sp,
                        maxLines = 1,
                    )
                }
            }
        }

        // The call is entered from one labelled button, and while you are in one the
        // microphone sits next to the call's own marker. A lone icon that both opened a
        // panel and muted the mic was exactly what made it impossible to tell whether you
        // were in the call at all.
        if (voice.joined) {
            // The microphone, then the state, then the way out — the three things a call
            // needs, in one row that never scrolls away. Everything else about the call
            // (who is in it, which speaker, deafen) is one tap behind the state pill.
            MicButton(muted = voice.muted, onClick = onToggleMic, size = 36.dp, iconSize = 19.dp)
            Spacer(Modifier.width(4.dp))
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(DeliColors.Success.copy(alpha = 0.18f))
                    .clickable { onOpenVoicePanel() }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(7.dp).clip(CircleShape).background(DeliColors.Success))
                Spacer(Modifier.width(6.dp))
                Text(
                    "Seste",
                    color = DeliColors.Success,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(Modifier.width(4.dp))
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(DeliColors.Danger.copy(alpha = 0.18f))
                    .clickable { onLeaveVoice() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.CallEnd,
                    contentDescription = com.delidehset.mobile.ui.t("Sesten ayrıl"),
                    tint = DeliColors.Danger,
                    modifier = Modifier.size(19.dp),
                )
            }
        } else {
            // There is a call running but you are not in it: say so, and let it be looked
            // at without joining it.
            if (voice.count > 0) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable { onOpenVoicePanel() }
                        .padding(horizontal = 8.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(7.dp).clip(CircleShape).background(DeliColors.Success))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        tf("Seste {0}", voice.count),
                        color = DeliColors.Success,
                        fontSize = 12.sp,
                    )
                }
            }
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .border(1.dp, DeliColors.Border, RoundedCornerShape(50))
                    .clickable { onJoinVoice() }
                    .padding(horizontal = 10.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Filled.Headset,
                    contentDescription = null,
                    tint = DeliColors.OnBackground,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "Sese katıl",
                    color = DeliColors.OnBackground,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
        Spacer(Modifier.width(4.dp))
    }
}

/** The microphone, in one place: same icon, same colour, wherever it is offered. */
@Composable
private fun MicButton(
    muted: Boolean,
    onClick: () -> Unit,
    size: Dp,
    iconSize: Dp,
    label: String? = null,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(if (muted) DeliColors.Danger.copy(alpha = 0.18f) else DeliColors.SurfaceVariant.copy(alpha = 0.5f))
            .clickable { onClick() }
            .padding(horizontal = if (label == null) 0.dp else 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.size(size), contentAlignment = Alignment.Center) {
            Icon(
                if (muted) Icons.Filled.MicOff else Icons.Filled.Mic,
                contentDescription = if (muted) com.delidehset.mobile.ui.t("Mikrofonu aç") else com.delidehset.mobile.ui.t("Mikrofonu kapat"),
                tint = if (muted) DeliColors.Danger else DeliColors.Success,
                modifier = Modifier.size(iconSize),
            )
        }
        if (label != null) {
            Text(
                label,
                color = if (muted) DeliColors.Danger else DeliColors.OnBackground,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                modifier = Modifier.padding(end = 14.dp),
            )
        }
    }
}

/**
 * The voice channel, native. Joining, muting, deafening and leaving are Compose buttons;
 * the audio itself runs in the room's own page behind the message list, so nothing here
 * pretends to re-implement the codec.
 */
@Composable
private fun VoicePanel(
    voice: VoiceState,
    micDenied: Boolean,
    speakerOn: Boolean,
    onJoin: () -> Unit,
    onToggleDeafen: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onOpenUser: (String) -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(DeliColors.SurfaceVariant.copy(alpha = 0.45f))
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Sesli sohbet",
                color = DeliColors.OnBackground,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.weight(1f))
            Text(
                when {
                    !voice.joined -> "Seste değilsin"
                    voice.muted -> "Sestesin • mikrofon kapalı"
                    else -> "Sestesin • mikrofon açık"
                },
                color = when {
                    !voice.joined -> DeliColors.OnSurfaceVariant
                    voice.muted -> DeliColors.Danger
                    else -> DeliColors.Success
                },
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
            )
        }

        if (micDenied) {
            Spacer(Modifier.height(8.dp))
            Text(
                "Mikrofon izni verilmedi. Ayarlardan izin verip tekrar dene.",
                color = DeliColors.Danger,
                fontSize = 12.sp,
            )
        }

        if (!voice.joined) {
            Spacer(Modifier.height(6.dp))
            Text(
                "Sese katılınca mikrofonun odaya açılır.",
                color = DeliColors.OnSurfaceVariant,
                fontSize = 12.sp,
            )
            Spacer(Modifier.height(10.dp))
            DeliButtonPrimary("Sese katıl") { onJoin() }
            return@Column
        }

        Spacer(Modifier.height(10.dp))

        if (voice.users.isEmpty()) {
            Text(
                "Seste kimse yok — ilk gelen sen ol.",
                color = DeliColors.OnSurfaceVariant,
                fontSize = 12.sp,
            )
        } else {
            voice.users.forEach { user: VoiceUser ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(DeliShapes.small)
                        .clickable { onOpenUser(user.name) }
                        .padding(vertical = 4.dp, horizontal = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .border(
                                if (user.speaking) 2.5.dp else 1.dp,
                                if (user.speaking) DeliColors.Success else nameColor(user.name).copy(alpha = 0.5f),
                                CircleShape,
                            )
                            .padding(2.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        DeliAvatar(url = null, name = user.name, size = 27.dp)
                    }
                    Spacer(Modifier.width(9.dp))
                    Text(
                        "@${user.name}",
                        color = nameColor(user.name),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        modifier = Modifier.weight(1f),
                    )
                    if (user.speaking) {
                        Icon(
                            Icons.Filled.GraphicEq,
                            contentDescription = null,
                            tint = DeliColors.Success,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Two settings, and only two: the microphone and the way out live in the bar at
        // the top of the room, always on screen, so offering them again here was the same
        // button twice. What is left is the route (how this phone plays the room) and
        // deafen — headphones, told apart from the speaker by its own icon.
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            VoiceControl(
                label = if (speakerOn) "Hoparlör" else "Kulaklık",
                active = speakerOn,
                icon = Icons.AutoMirrored.Filled.VolumeUp,
                onClick = onToggleSpeaker,
                modifier = Modifier.weight(1f),
            )
            VoiceControl(
                label = if (voice.deafened) "Sesi geri aç" else "Sağırlaştır",
                active = voice.deafened,
                icon = if (voice.deafened) Icons.Filled.HeadsetOff else Icons.Filled.Headset,
                onClick = onToggleDeafen,
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(Modifier.height(8.dp))

        Text(
            "Mikrofonu kapatmak ve ayrılmak için üstteki çubuğu kullan.",
            color = DeliColors.OnSurfaceVariant,
            fontSize = 11.sp,
        )
    }
}

@Composable
private fun VoiceControl(
    label: String,
    active: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(DeliShapes.control)
            .background(if (active) DeliColors.Primary.copy(alpha = 0.22f) else DeliColors.Surface)
            .border(1.dp, if (active) DeliColors.Primary.copy(alpha = 0.5f) else DeliColors.Border, DeliShapes.control)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (active) DeliColors.Primary else DeliColors.OnBackground,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            label,
            color = if (active) DeliColors.Primary else DeliColors.OnBackground,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
        )
    }
}

/**
 * One line of the room. Your own lines sit on the right in a tinted bubble; everyone
 * else's sit on the left under their name, in the colour that name owns.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChatRow(
    message: ChatMessage,
    own: Boolean,
    startsTurn: Boolean,
    onOpenUser: (String) -> Unit = {},
) {
    val rendered = remember(message.id, message.bodyHtml) { DeliHtml.render(message.bodyHtml) }

    // The room's own colour for this account when it has one, otherwise the colour the
    // name earns. The site's default link purple counts as "no colour" — that is exactly
    // the case where every name in the room looks the same.
    val authorColor = remember(message.author, message.authorColor) {
        parseCssColor(message.authorColor)?.takeIf { it != DeliColors.Primary } ?: nameColor(message.author)
    }

    val context = LocalContext.current

    // Copying goes through the platform clipboard: it behaves the same on every Android
    // version, which is more than can be said for the Compose wrapper's signatures.
    val copy: (CharSequence) -> Unit = { text ->
        val clipboard = context.getSystemService(android.content.ClipboardManager::class.java)
        clipboard?.setPrimaryClip(android.content.ClipData.newPlainText("mesaj", text))
        android.widget.Toast.makeText(context, com.delidehset.mobile.ui.tr("Mesaj kopyalandı"), android.widget.Toast.LENGTH_SHORT).show()
    }

    // Both sides get a bubble: a line of text floating on the background reads as a
    // comment thread, not as a conversation. Yours carries the app's own colour, theirs
    // sits on the surface colour so the two are told apart at a glance.
    val body: @Composable () -> Unit = {
        Column(
            modifier = Modifier
                .clip(
                    RoundedCornerShape(
                        topStart = 14.dp,
                        topEnd = 14.dp,
                        bottomStart = if (own) 14.dp else 4.dp,
                        bottomEnd = if (own) 4.dp else 14.dp,
                    )
                )
                .background(
                    if (own) DeliColors.Primary.copy(alpha = 0.22f)
                    else DeliColors.SurfaceVariant.copy(alpha = 0.5f)
                )
                // Long-press copies the line: a room's worth of text you cannot get out of
                // the app is the one thing every phone chat has and this one did not.
                .then(
                    if (rendered.text.isNotBlank()) {
                        Modifier.combinedClickable(
                            onClick = {},
                            onLongClick = { copy(rendered.text.toString()) },
                        )
                    } else {
                        Modifier
                    }
                )
                .border(
                    1.dp,
                    if (own) DeliColors.Primary.copy(alpha = 0.35f) else DeliColors.Border,
                    RoundedCornerShape(
                        topStart = 14.dp,
                        topEnd = 14.dp,
                        bottomStart = if (own) 14.dp else 4.dp,
                        bottomEnd = if (own) 4.dp else 14.dp,
                    ),
                )
                .padding(horizontal = 10.dp, vertical = 7.dp),
            horizontalAlignment = if (own) Alignment.End else Alignment.Start,
        ) {
            if (startsTurn) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!own) {
                        Text(
                            "@${message.author}",
                            color = authorColor,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clickable {
                                message.author.takeIf { it.isNotBlank() }?.let(onOpenUser)
                            },
                        )
                        Spacer(Modifier.width(6.dp))
                    }
                    if (message.timeText.isNotBlank()) {
                        Text(message.timeText, color = DeliColors.OnSurfaceVariant, fontSize = 10.5.sp)
                    }
                }
                Spacer(Modifier.height(1.dp))
            }
            if (rendered.text.isNotBlank()) {
                Text(
                    text = rendered.text,
                    color = DeliColors.OnBackground,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                )
            }
            rendered.images.forEach { url ->
                Spacer(Modifier.height(6.dp))
                androidx.compose.foundation.Image(
                    painter = coil.compose.rememberAsyncImagePainter(url),
                    contentDescription = null,
                    modifier = Modifier
                        .width(180.dp)
                        .clip(DeliShapes.small),
                )
            }
            if (message.reactions.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    message.reactions.forEach { reaction ->
                        Text(
                            reaction,
                            color = DeliColors.OnSurfaceVariant,
                            fontSize = 11.sp,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(DeliColors.SurfaceVariant.copy(alpha = 0.6f))
                                .padding(horizontal = 7.dp, vertical = 2.dp),
                        )
                    }
                }
            }
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = if (startsTurn) 5.dp else 1.dp),
        horizontalArrangement = if (own) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top,
    ) {
        if (own) {
            // Your own lines run all the way to the right edge and carry no picture: you
            // already know who you are, and the row of your own face down the side of a
            // conversation is noise.
            Box(modifier = Modifier.weight(1f, fill = false)) { body() }
        } else {
            if (startsTurn) {
                // The picture (or its letter) carries the colour the name already has, so
                // a person is one colour in the room instead of two.
                DeliAvatar(
                    url = message.avatarUrl,
                    size = 28.dp,
                    name = message.author,
                    accent = authorColor,
                    modifier = Modifier.clickable {
                        message.author.takeIf { it.isNotBlank() }?.let(onOpenUser)
                    },
                )
                Spacer(Modifier.width(9.dp))
            } else {
                // Keep the follow-up lines aligned with the first one instead of letting
                // them drift back under the picture.
                Spacer(Modifier.width(37.dp))
            }
            Box(modifier = Modifier.weight(1f)) { body() }
        }
    }
}

@Composable
private fun Composer(
    draft: String,
    onDraftChange: (String) -> Unit,
    onSend: () -> Unit,
    bottomInset: androidx.compose.ui.unit.Dp,
) {
    val canSend = draft.isNotBlank()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DeliColors.Surface)
            .padding(
                start = 12.dp,
                end = 10.dp,
                top = 8.dp,
                bottom = bottomInset + 8.dp,
            ),
        verticalAlignment = Alignment.Bottom,
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(DeliShapes.card)
                .background(DeliColors.Background)
                .border(1.dp, DeliColors.Border, DeliShapes.card)
                .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            if (draft.isEmpty()) {
                Text("Mesaj yaz…", color = DeliColors.OnSurfaceVariant, fontSize = 14.sp)
            }
            BasicTextField(
                value = draft,
                onValueChange = onDraftChange,
                textStyle = TextStyle(color = DeliColors.OnBackground, fontSize = 14.sp),
                cursorBrush = SolidColor(DeliColors.Primary),
                maxLines = 3,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { onSend() }),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Spacer(Modifier.width(8.dp))
        // A filled button while there is something to send, a quiet one while there is
        // not: the send action should never look available when it would do nothing.
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(if (canSend) DeliColors.Primary else DeliColors.SurfaceVariant)
                .clickable(enabled = canSend) { onSend() },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.AutoMirrored.Filled.Send,
                contentDescription = com.delidehset.mobile.ui.t("Gönder"),
                tint = if (canSend) androidx.compose.ui.graphics.Color.White else DeliColors.OnSurfaceVariant,
                modifier = Modifier.size(19.dp),
            )
        }
    }
}
