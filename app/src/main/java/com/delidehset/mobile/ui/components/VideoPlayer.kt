package com.delidehset.mobile.ui.components

import android.graphics.Color as AndroidColor
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.delidehset.mobile.ui.Text
import com.delidehset.mobile.ui.theme.DeliColors
import com.delidehset.mobile.ui.theme.DeliShapes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext

/** File endings the site actually uploads; anything else is treated as a picture. */
fun isVideoUrl(url: String?): Boolean {
    if (url.isNullOrBlank()) return false
    val path = url.substringBefore('?').lowercase()
    return listOf(".mp4", ".webm", ".mov", ".m4v", ".mkv", ".ogv").any { path.endsWith(it) }
}

/**
 * The app's own video player.
 *
 * The site embeds plain `<video src="…webm">` tags, which the app used to drop, so a video
 * post looked like an empty post with a broken thumbnail. This draws the stream with
 * ExoPlayer and puts a control layer over it in the app's colours: tap to play or pause,
 * a thin seek bar you can tap anywhere on, a mute switch, and full screen.
 */
@OptIn(UnstableApi::class)
@Composable
fun DeliVideoPlayer(
    url: String,
    modifier: Modifier = Modifier,
    /** Preview players on a feed start paused and without controls until tapped. */
    compact: Boolean = false,
) {
    val context = LocalContext.current
    val full = remember(url) { if (url.startsWith("http")) url else "https://delidehset.com$url" }

    val player = remember(full) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(full))
            repeatMode = Player.REPEAT_MODE_OFF
            playWhenReady = false
            prepare()
        }
    }
    DisposableEffect(player) { onDispose { player.release() } }

    var playing by remember(player) { mutableStateOf(false) }
    var buffering by remember(player) { mutableStateOf(true) }
    var position by remember(player) { mutableLongStateOf(0L) }
    var duration by remember(player) { mutableLongStateOf(0L) }
    var muted by remember(player) { mutableStateOf(false) }
    var fullscreen by remember(player) { mutableStateOf(false) }

    LaunchedEffect(player) {
        while (isActive) {
            playing = player.isPlaying
            buffering = player.playbackState == Player.STATE_BUFFERING
            position = player.currentPosition.coerceAtLeast(0L)
            duration = player.duration.coerceAtLeast(0L)
            delay(200)
        }
    }

    fun seekTo(fraction: Float) {
        val total = player.duration
        if (total > 0) player.seekTo((total * fraction.coerceIn(0f, 1f)).toLong())
    }

    // Which way the last skip went, so the player can say "+5 sn" where the finger was. It
    // clears itself a moment later; nothing here needs to be remembered.
    var skipHint by remember { mutableStateOf(0) }
    LaunchedEffect(skipHint) {
        if (skipHint != 0) {
            delay(700)
            skipHint = 0
        }
    }
    fun skipByTouch(forward: Boolean) {
        val total = player.duration.coerceAtLeast(0L)
        val step = if (forward) 5_000L else -5_000L
        val target = (player.currentPosition + step)
            .coerceIn(0L, if (total > 0) total else Long.MAX_VALUE)
        player.seekTo(target)
        skipHint = if (forward) 1 else -1
    }

    fun toggle() {
        if (player.isPlaying) player.pause() else player.play()
    }

    /**
     * Five seconds either way, the way every video app does it: a double tap on the right
     * half goes forward, on the left half back. It seeks from the position on screen rather
     * than from the last known one, and stops at either end instead of wrapping.
     */
    fun skip(forward: Boolean): Long {
        val total = player.duration.coerceAtLeast(0L)
        val step = if (forward) 5_000L else -5_000L
        val target = (player.currentPosition + step).coerceIn(0L, if (total > 0) total else Long.MAX_VALUE)
        player.seekTo(target)
        return target
    }

    var showControls by remember { mutableStateOf(!compact) }

    LaunchedEffect(showControls, playing) {
        if (showControls && playing) {
            delay(3500)
            showControls = false
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .clip(DeliShapes.control)
            .background(Color.Black),
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    setShutterBackgroundColor(AndroidColor.TRANSPARENT)
                    setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                    this.player = null
                }
            },
            update = { view -> view.player = if (fullscreen) null else player },
            modifier = Modifier.fillMaxSize(),
        )

        // Overlay: tap anywhere to show/hide controls or play/pause
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (showControls || !playing) {
                        Modifier.background(
                            Brush.verticalGradient(
                                listOf(Color.Black.copy(alpha = 0.35f), Color.Transparent, Color.Black.copy(alpha = 0.65f))
                            )
                        )
                    } else Modifier
                )
                .pointerInput(player) {
                    detectTapGestures(
                        onTap = {
                            if (!compact) showControls = !showControls
                            else toggle()
                        },
                        onDoubleTap = { offset ->
                            skipByTouch(forward = offset.x > size.width / 2f)
                        },
                    )
                },
        )

        // Skip hint (+5 sn / -5 sn)
        if (skipHint != 0) {
            Box(
                modifier = Modifier
                    .align(if (skipHint > 0) Alignment.CenterEnd else Alignment.CenterStart)
                    .padding(horizontal = 14.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.65f))
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (skipHint > 0) "5 sn  \u00bb" else "\u00ab  5 sn",
                    color = Color.White,
                    fontSize = 13.sp,
                )
            }
        }

        if (buffering && !playing) {
            Box(Modifier.align(Alignment.Center)) {
                CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(32.dp))
            }
        } else if (!playing || (showControls && !compact)) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.55f))
                    .clickable { toggle() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = com.delidehset.mobile.ui.t(if (playing) "Kapat" else "Oynat"),
                    tint = Color.White,
                    modifier = Modifier.size(32.dp),
                )
            }
        }

        if (!fullscreen && (showControls || !playing)) {
            Column(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
                if (!compact) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(20.dp)
                            .pointerInput(player, duration) {
                                detectTapGestures { offset ->
                                    if (duration > 0) seekTo(offset.x / size.width.toFloat())
                                }
                            },
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.35f))
                        )
                        val progress = if (duration > 0) position.toFloat() / duration else 0f
                        Box(
                            Modifier
                                .fillMaxWidth(progress.coerceIn(0f, 1f))
                                .height(4.dp)
                                .clip(CircleShape)
                                .background(DeliColors.Primary)
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!compact) {
                        Text(timeLabel(position), color = Color.White, fontSize = 11.sp)
                        Text(" / ", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                        Text(timeLabel(duration), color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                        Spacer(Modifier.weight(1f))
                        Icon(
                            imageVector = if (muted) Icons.Filled.VolumeOff else Icons.Filled.VolumeUp,
                            contentDescription = if (muted) com.delidehset.mobile.ui.t("Sesi aç") else com.delidehset.mobile.ui.t("Sessize al"),
                            tint = Color.White,
                            modifier = Modifier
                                .size(20.dp)
                                .clickable {
                                    muted = !muted
                                    player.volume = if (muted) 0f else 1f
                                },
                        )
                        Spacer(Modifier.width(16.dp))
                        Icon(
                            Icons.Filled.Fullscreen,
                            contentDescription = com.delidehset.mobile.ui.t("Tam ekran"),
                            tint = Color.White,
                            modifier = Modifier.size(22.dp).clickable { fullscreen = true },
                        )
                    } else {
                        Spacer(Modifier.weight(1f))
                        Icon(
                            Icons.Filled.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }
    }

    if (fullscreen) {
        Dialog(
            onDismissRequest = { fullscreen = false },
            properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
        ) {
            var fullShowControls by remember { mutableStateOf(true) }
            LaunchedEffect(fullShowControls, playing) {
                if (fullShowControls && playing) {
                    delay(3500)
                    fullShowControls = false
                }
            }

            Box(Modifier.fillMaxSize().background(Color.Black)) {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            useController = false
                            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                            setShutterBackgroundColor(AndroidColor.TRANSPARENT)
                            setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                        }
                    },
                    update = { view -> view.player = player },
                    modifier = Modifier.fillMaxSize(),
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(
                            if (fullShowControls || !playing) {
                                Modifier.background(
                                    Brush.verticalGradient(
                                        listOf(Color.Black.copy(alpha = 0.5f), Color.Transparent, Color.Black.copy(alpha = 0.7f))
                                    )
                                )
                            } else Modifier
                        )
                        .pointerInput(player) {
                            detectTapGestures(
                                onTap = { fullShowControls = !fullShowControls },
                                onDoubleTap = { offset -> skipByTouch(forward = offset.x > size.width / 2f) },
                            )
                        },
                )

                // Skip hint in fullscreen
                if (skipHint != 0) {
                    Box(
                        modifier = Modifier
                            .align(if (skipHint > 0) Alignment.CenterEnd else Alignment.CenterStart)
                            .padding(horizontal = 24.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.65f))
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = if (skipHint > 0) "5 sn  \u00bb" else "\u00ab  5 sn",
                            color = Color.White,
                            fontSize = 15.sp,
                        )
                    }
                }

                if (fullShowControls) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Filled.FullscreenExit,
                            contentDescription = com.delidehset.mobile.ui.t("Küçült"),
                            tint = Color.White,
                            modifier = Modifier
                                .size(30.dp)
                                .clickable { fullscreen = false },
                        )
                    }
                }

                if (!playing || fullShowControls) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(68.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.55f))
                            .clickable { toggle() },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = com.delidehset.mobile.ui.t(if (playing) "Kapat" else "Oynat"),
                            tint = Color.White,
                            modifier = Modifier.size(40.dp),
                        )
                    }
                }

                if (fullShowControls) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(24.dp)
                                .pointerInput(player, duration) {
                                    detectTapGestures { offset ->
                                        if (duration > 0) seekTo(offset.x / size.width.toFloat())
                                    }
                                },
                            contentAlignment = Alignment.CenterStart,
                        ) {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.35f))
                            )
                            val progress = if (duration > 0) position.toFloat() / duration else 0f
                            Box(
                                Modifier
                                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                                    .height(4.dp)
                                    .clip(CircleShape)
                                    .background(DeliColors.Primary)
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(timeLabel(position), color = Color.White, fontSize = 12.sp)
                            Text(" / ", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                            Text(timeLabel(duration), color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                            Spacer(Modifier.weight(1f))
                            Icon(
                                imageVector = if (muted) Icons.Filled.VolumeOff else Icons.Filled.VolumeUp,
                                contentDescription = if (muted) com.delidehset.mobile.ui.t("Sesi aç") else com.delidehset.mobile.ui.t("Sessize al"),
                                tint = Color.White,
                                modifier = Modifier
                                    .size(24.dp)
                                    .clickable {
                                        muted = !muted
                                        player.volume = if (muted) 0f else 1f
                                    },
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * A frame of the clip with the play badge, which is what a feed card shows instead of
 * loading the video.
 *
 * The site's own stand-in is a link icon, not a frame, so a video post used to reach the
 * feed as a plain black rectangle with a triangle in it. When there is no usable picture
 * the first frame is taken out of the clip itself once per URL and kept in memory: the
 * card then shows the picture the site shows — the video's own opening shot.
 */
@Composable
fun VideoThumb(url: String?, videoUrl: String? = null, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val still = url?.takeIf { it.isNotBlank() && !it.contains("default_thumb") }
    val absolute = videoUrl?.takeIf { it.isNotBlank() }?.let { if (it.startsWith("http")) it else "https://delidehset.com$it" }

    var frame by remember(absolute) { mutableStateOf(absolute?.let { frames[it] }) }
    LaunchedEffect(absolute, still) {
        if (still != null || absolute == null || frame != null) return@LaunchedEffect
        frame = withContext(Dispatchers.IO) {
            frames[absolute] ?: runCatching { firstFrame(context, absolute) }
                .getOrNull()
                ?.also { frames[absolute] = it }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .clip(DeliShapes.control)
            .background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        val shot = frame
        if (shot != null) {
            androidx.compose.foundation.Image(
                bitmap = shot.asImageBitmap(),
                contentDescription = null,
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            // The badge sits on the picture, so the picture is darkened where it does.
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.25f)),
            )
        } else if (still != null) {
            androidx.compose.foundation.Image(
                painter = coil.compose.rememberAsyncImagePainter(
                    model = if (still.startsWith("http")) still else "https://delidehset.com$still"
                ),
                contentDescription = null,
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.55f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = com.delidehset.mobile.ui.t("Videoyu oynat"),
                tint = Color.White,
                modifier = Modifier.size(28.dp),
            )
        }
    }
}

/** The frames taken out of clips in this session, so scrolling does not re-decode them. */
private val frames = java.util.concurrent.ConcurrentHashMap<String, android.graphics.Bitmap>()

/**
 * The clip's first frame, or the one a second in for clips that open on black.
 *
 * `MediaMetadataRetriever` reads the container itself, so this costs one network read per
 * URL and then lives in [frames].
 */
private fun firstFrame(context: android.content.Context, url: String): android.graphics.Bitmap? {
    val retriever = android.media.MediaMetadataRetriever()
    return try {
        retriever.setDataSource(url, emptyMap<String, String>())
        retriever.getFrameAtTime(1_000_000L, android.media.MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            ?: retriever.frameAtTime
    } catch (t: Throwable) {
        null
    } finally {
        runCatching { retriever.release() }
    }
}

/** `1:05` / `1:02:05`, the way every player writes a position. */
private fun timeLabel(ms: Long): String {
    if (ms <= 0) return "0:00"
    val totalSeconds = ms / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
    }
}
