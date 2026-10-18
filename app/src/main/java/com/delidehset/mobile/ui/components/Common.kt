package com.delidehset.mobile.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import com.delidehset.mobile.ui.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.ImageRequest
import com.delidehset.mobile.data.AvatarStore
import com.delidehset.mobile.data.DeliHttp
import com.delidehset.mobile.ui.theme.CardStyle
import com.delidehset.mobile.ui.theme.DeliColors
import com.delidehset.mobile.ui.theme.avatarShape
import com.delidehset.mobile.ui.theme.cardShape
import com.delidehset.mobile.ui.theme.controlShape
import com.delidehset.mobile.ui.theme.kitButtonColors
import com.delidehset.mobile.ui.theme.kitButtonOutline
import com.delidehset.mobile.ui.theme.kitSurface
import com.delidehset.mobile.ui.theme.nameColor

/**
 * A user avatar. When the account has no picture — which on this beta is more often than
 * not — it falls back to the name's first letter on a colour derived from that name, so
 * nobody ends up as the same grey silhouette in the same purple chat.
 *
 * Pass [name] wherever it is known; without it the shipped placeholder is used.
 */
@Composable
fun DeliAvatar(
    url: String?,
    size: Dp,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    name: String? = null,
    /** The colour the name already has on this screen, so the letter matches it. */
    accent: Color? = null,
) {
    val context = LocalContext.current
    // The kit decides whether a face is round or a tile; both the picture and the letter
    // fallback below follow it.
    val shape = avatarShape(DeliColors.palette)
    // The site serves a stock picture for accounts without one; showing it next to the
    // app's own placeholder is what made avatars look inconsistent between screens.
    val given = url?.takeIf { it.isNotBlank() && !it.contains("default-profile-pic") }

    // Where the site gave no picture but did give a name — comment rows, the user tables,
    // a post's author line — the account is looked up once. Without this the app showed a
    // bare initial on accounts the website shows a face for.
    var looked by remember(name, given) { mutableStateOf(AvatarStore.cached(name)) }
    LaunchedEffect(name, given) {
        if (given != null || name.isNullOrBlank() || isAnonymous(name)) return@LaunchedEffect
        val app = context.applicationContext as? com.delidehset.mobile.DeliApp ?: return@LaunchedEffect
        val repository = runCatching { app.container.repository }.getOrNull() ?: return@LaunchedEffect
        looked = AvatarStore.remember(repository, name)
    }

    val full = (given ?: looked)
        ?.takeIf { it.isNotBlank() && !it.contains("default-profile-pic") }
        ?.let { if (it.startsWith("http")) it else DeliHttp.BASE_URL + it }

    if (full == null) {
        if (isAnonymous(name)) {
            AnonymousAvatar(
                size = size,
                shape = shape,
                modifier = modifier,
                contentDescription = contentDescription,
            )
        } else {
            InitialAvatar(
                name = name,
                size = size,
                shape = shape,
                modifier = modifier,
                contentDescription = contentDescription,
                accent = accent,
            )
        }
        return
    }

    // Avatars sit in every list row: building a fresh request object on each composition
    // makes Coil re-resolve the same image over and over.
    val request = remember(full) {
        ImageRequest.Builder(context).data(full).crossfade(true).build()
    }
    SubcomposeAsyncImage(
        model = request,
        contentDescription = contentDescription,
        modifier = modifier.size(size).clip(shape),
        contentScale = ContentScale.Crop,
    ) {
        // A picture that fails to load lands here too, so a dead URL shows the same
        // initial every other account without a picture gets.
        if (painter.state is AsyncImagePainter.State.Success) {
            SubcomposeAsyncImageContent()
        } else if (isAnonymous(name)) {
            AnonymousAvatar(
                size = size,
                shape = shape,
                contentDescription = contentDescription,
            )
        } else {
            InitialAvatar(
                name = name,
                size = size,
                shape = shape,
                contentDescription = contentDescription,
                accent = accent,
            )
        }
    }
}

/** The site's anonymised author: a name that stands for nobody in particular. */
private fun isAnonymous(name: String?): Boolean =
    name != null && name.trim().equals("Anonymous", ignoreCase = true)

/**
 * The letter a nameless account is drawn with.
 *
 * The first *code point*, not the first UTF-16 unit: a name that starts with an emoji —
 * and people do use them — was cut in half by `take(1)`, which left a replacement glyph
 * where a letter should be. Null for a blank name, so callers can fall back to an icon.
 */
fun initialOf(name: String?): String? {
    val trimmed = name?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    val first = Character.codePointAt(trimmed, 0)
    return String(Character.toChars(first)).uppercase()
}

/**
 * What an anonymous author gets instead of a letter.
 *
 * There is nobody behind the name to give a colour to, and a lone "A" on a tinted square
 * read as a picture that failed to load rather than as a nameless account.
 */
@Composable
private fun AnonymousAvatar(
    size: Dp,
    shape: Shape,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(DeliColors.SurfaceVariant)
            .border(1.dp, DeliColors.Border, shape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Filled.PersonOff,
            contentDescription = contentDescription,
            tint = DeliColors.OnSurfaceVariant,
            modifier = Modifier.size(size * 0.6f),
        )
    }
}

/**
 * The fallback avatar: first letter of the name on that name's colour.
 *
 * Most accounts here have no picture, so this is what the room actually looks like. A flat
 * 28 % tint with a matching letter read as a washed-out dot; the name's colour is used as
 * a gradient instead, and the letter sits on top in the app's text colour, which is what
 * makes two people with the same first letter still easy to tell apart.
 */
@Composable
private fun InitialAvatar(
    name: String?,
    size: Dp,
    shape: Shape,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    accent: Color? = null,
) {
    val label = name?.trim()?.takeIf { it.isNotEmpty() }
    val tint = accent ?: label?.let { nameColor(it) }
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(
                if (tint != null) {
                    Brush.linearGradient(
                        listOf(tint.copy(alpha = 0.45f), tint.copy(alpha = 0.18f))
                    )
                } else {
                    Brush.linearGradient(listOf(DeliColors.SurfaceVariant, DeliColors.Surface))
                }
            )
            .border(1.dp, (tint ?: DeliColors.Border).copy(alpha = if (tint != null) 0.6f else 1f), shape),
        contentAlignment = Alignment.Center,
    ) {
        val initial = initialOf(label)
        if (initial != null) {
            // `propagateMinConstraints` used to hand the letter a box the size of the
            // whole avatar and start it at that box's top-left corner — which is why the
            // initial sat high and left instead of in the middle. The font padding is
            // gone too: with a glyph as tall as the line, it was what pushed the letter
            // down by a few more pixels.
            val letterSize = (size.value * 0.42f).sp
            Text(
                text = initial,
                color = DeliColors.OnBackground,
                fontSize = letterSize,
                lineHeight = letterSize,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                style = androidx.compose.ui.text.TextStyle(
                    platformStyle = androidx.compose.ui.text.PlatformTextStyle(includeFontPadding = false),
                ),
            )
        } else {
            Icon(
                Icons.Filled.Person,
                contentDescription = contentDescription,
                tint = DeliColors.OnSurfaceVariant,
                modifier = Modifier.size(size * 0.6f),
            )
        }
    }
}

/**
 * Card surface used by every list on the site (`.card` / `.feed-card`).
 *
 * The fill, the hairline, the lift and the padding all come from the kit in force, so a
 * list is drawn in that kit's idiom rather than in one fixed one.
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun DeliCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onDoubleClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val clickMod = when {
        onDoubleClick != null || onLongClick != null ->
            Modifier.combinedClickable(
                onClick = { onClick?.invoke() },
                onDoubleClick = onDoubleClick,
                onLongClick = onLongClick,
            )
        onClick != null -> Modifier.clickable { onClick() }
        else -> Modifier
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .kitSurface()
            .then(clickMod)
    ) {
        Column { content() }
    }
}

@Composable
fun LoadingBox(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = DeliColors.Primary, strokeWidth = 3.dp)
    }
}

@Composable
fun ErrorBox(
    message: String,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = message,
            color = DeliColors.Danger,
            textAlign = TextAlign.Center,
            fontSize = 14.sp,
        )
        if (onRetry != null) {
            TextButton(onClick = onRetry) {
                Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Tekrar dene", color = DeliColors.Blue)
            }
        }
    }
}

@Composable
fun EmptyBox(text: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(text, color = DeliColors.OnSurfaceVariant, fontSize = 14.sp, textAlign = TextAlign.Center)
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    val kit = DeliColors.palette
    Text(
        text = text,
        modifier = modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = kit.spacing * 0.8f),
        color = DeliColors.OnBackground,
        fontSize = 18.sp,
        fontWeight = kit.titleWeight,
        letterSpacing = kit.letterSpacing,
    )
}

/**
 * The up/down arrow pair plus score, exactly like the `.feed-actions` row on a card.
 *
 * A vote the viewer has cast is drawn as a *filled* circle behind that arrow, not just a
 * different tint. The tint alone made an upvote read as "yellow, so it worked" while a
 * downvote — teal on the site's palette, only a shade away from the muted grey of an
 * unvoted arrow — looked like nothing had happened at all. The score follows the vote and
 * carries the same colour, so the row says which way it was cast even at a glance.
 *
 * When the viewer cannot vote the arrows are dimmed instead of hidden, matching the way
 * the site links the arrows to /signup for guests.
 */
@Composable
fun VoteArrows(
    score: Int?,
    viewerVote: Int,
    canVote: Boolean,
    onVote: (Int) -> Unit,
    compact: Boolean = false,
) {
    val size = if (compact) 20.dp else 25.dp
    Row(verticalAlignment = Alignment.CenterVertically) {
        VoteArrow(
            up = true,
            active = viewerVote > 0,
            canVote = canVote,
            size = size,
            onClick = { onVote(if (viewerVote == 1) 0 else 1) },
        )
        Text(
            text = score?.toString() ?: "–",
            color = when {
                viewerVote > 0 -> DeliColors.Upvote
                viewerVote < 0 -> DeliColors.Downvote
                else -> DeliColors.OnSurfaceVariant
            },
            fontSize = if (compact) 12.sp else 14.sp,
            fontWeight = if (viewerVote != 0) FontWeight.ExtraBold else FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        VoteArrow(
            up = false,
            active = viewerVote < 0,
            canVote = canVote,
            size = size,
            onClick = { onVote(if (viewerVote == -1) 0 else -1) },
        )
    }
}

/** One arrow of [VoteArrows]: solid colour and slightly thicker stroke when active, no circle background. */
@Composable
private fun VoteArrow(
    up: Boolean,
    active: Boolean,
    canVote: Boolean,
    size: Dp,
    onClick: () -> Unit,
) {
    val accent = if (up) DeliColors.Upvote else DeliColors.Downvote
    val tint = when {
        active -> accent
        canVote -> DeliColors.OnSurfaceVariant
        else -> DeliColors.Border
    }
    Box(
        modifier = Modifier
            .size(size + 6.dp)
            .then(if (canVote) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.foundation.Canvas(modifier = Modifier.size(size)) {
            val w = this.size.width
            val h = this.size.height
            val dx = w * 0.24f
            val dy = h * 0.14f
            val cy = h * 0.50f
            val topY = if (up) cy - dy else cy + dy
            val sideY = if (up) cy + dy else cy - dy
            val strokePx = (if (active) 2.75.dp else 1.85.dp).toPx()
            val path = androidx.compose.ui.graphics.Path().apply {
                moveTo(w * 0.5f - dx, sideY)
                lineTo(w * 0.5f, topY)
                lineTo(w * 0.5f + dx, sideY)
            }
            drawPath(
                path = path,
                color = tint,
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = strokePx,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round,
                    join = androidx.compose.ui.graphics.StrokeJoin.Round,
                ),
            )
        }
    }
}

/**
 * The pull the person made, as opposed to the load a screen starts by itself.
 *
 * `PullToRefreshBox` draws its spinner whenever `isRefreshing` is true, so keying that on
 * "this screen is loading" meant the indicator dropped down every time a page was opened,
 * a tab was switched or a cached list refreshed itself — noise for something nobody asked
 * for. Only a real pull arms it; the next time the screen settles, it disarms itself.
 */
class ManualRefresh internal constructor() {
    internal var armed by androidx.compose.runtime.mutableStateOf(false)

    val active: Boolean get() = armed

    fun start() {
        armed = true
    }
}

/** Arms a [ManualRefresh] and clears it as soon as the screen has finished loading. */
@Composable
fun rememberManualRefresh(loading: Boolean): ManualRefresh {
    val pull = remember { ManualRefresh() }
    LaunchedEffect(loading, pull.active) {
        if (!loading && pull.active) {
            kotlinx.coroutines.delay(180)
            if (!loading) pull.armed = false
        }
    }
    return pull
}

/**
 * Pulls an element up so it overlaps whatever is above it.
 *
 * The profile heroes on the site let the avatar ride over the banner, which in CSS is a
 * negative margin — but Compose *rejects* negative padding (`IllegalArgumentException:
 * Padding must be non-negative`), so the same effect has to be a layout modifier: the
 * element keeps its size, is placed [amount] higher, and reports a correspondingly
 * shorter height so the content below it does not move down.
 */
fun Modifier.pullUp(amount: Dp): Modifier = this.layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    val shift = amount.roundToPx().coerceAtMost(placeable.height)
    layout(placeable.width, placeable.height - shift) {
        placeable.place(0, -shift)
    }
}

/** Small rounded chip used for holes and flairs. */
@Composable
fun DeliChip(text: String, onClick: (() -> Unit)? = null, accent: Boolean = false) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (accent) DeliColors.PrimaryContainer else DeliColors.SurfaceVariant)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            color = if (accent) DeliColors.OnBackground else DeliColors.OnSurfaceVariant,
            fontSize = 12.sp,
        )
    }
}

@Composable
fun ThinDivider(modifier: Modifier = Modifier) {
    // Kits that separate their rows with air instead of a line simply give the space.
    val kit = DeliColors.palette
    if (!kit.dividers) {
        Spacer(modifier.fillMaxWidth().height(kit.spacing * 0.5f))
        return
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(DeliColors.Border)
    )
}

@Composable
fun HorizontalButtonRow(content: @Composable () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) { content() }
}

@Composable
fun FieldLabel(text: String) {
    Text(
        text = text,
        color = DeliColors.OnSurfaceVariant,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
    )
}

@Composable
fun BodyText(text: String, modifier: Modifier = Modifier) {
    Text(text = text, color = DeliColors.OnBackground, fontSize = 14.sp, modifier = modifier)
}

@Composable
fun MutedText(text: String, modifier: Modifier = Modifier) {
    Text(text = text, color = DeliColors.OnSurfaceVariant, fontSize = 12.sp, modifier = modifier)
}

@Composable
fun DeliButtonPrimary(
    text: String,
    enabled: Boolean = true,
    modifier: Modifier = Modifier.fillMaxWidth(),
    onClick: () -> Unit,
) {
    val kit = DeliColors.palette
    androidx.compose.material3.Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.kitButtonOutline(kit),
        shape = controlShape(kit),
        colors = kitButtonColors(kit),
    ) { Text(text, fontWeight = kit.titleWeight, letterSpacing = kit.letterSpacing) }
}

@Composable
fun DeliOutlinedButton(
    text: String,
    enabled: Boolean = true,
    modifier: Modifier = Modifier.fillMaxWidth(),
    onClick: () -> Unit,
) {
    val kit = DeliColors.palette
    androidx.compose.material3.OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        shape = controlShape(kit),
        colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
            contentColor = DeliColors.OnBackground,
        ),
    ) { Text(text) }
}

@Composable
fun screenPadding() = Modifier.padding(horizontal = 12.dp)

@Composable
fun Spacer8() = Spacer(Modifier.height(8.dp))

@Composable
fun ColorSwatch(color: androidx.compose.ui.graphics.Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(14.dp).background(color))
        Spacer(Modifier.width(8.dp))
        Text(label, color = MaterialTheme.colorScheme.onBackground, fontSize = 12.sp)
    }
}
