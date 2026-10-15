package com.delidehset.mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import com.delidehset.mobile.ui.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
import com.delidehset.mobile.data.DeliHttp
import com.delidehset.mobile.ui.theme.DeliColors
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
) {
    val context = LocalContext.current
    // The site serves a stock picture for accounts without one; showing it next to the
    // app's own placeholder is what made avatars look inconsistent between screens.
    val full = url
        ?.takeIf { it.isNotBlank() && !it.contains("default-profile-pic") }
        ?.let { if (it.startsWith("http")) it else DeliHttp.BASE_URL + it }

    if (full == null) {
        InitialAvatar(name = name, size = size, modifier = modifier, contentDescription = contentDescription)
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
        modifier = modifier.size(size).clip(CircleShape),
        contentScale = ContentScale.Crop,
    ) {
        // A picture that fails to load lands here too, so a dead URL shows the same
        // initial every other account without a picture gets.
        if (painter.state is AsyncImagePainter.State.Success) {
            SubcomposeAsyncImageContent()
        } else {
            InitialAvatar(name = name, size = size, contentDescription = contentDescription)
        }
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
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    val label = name?.trim()?.takeIf { it.isNotEmpty() }
    val tint = label?.let { nameColor(it) }
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(
                if (tint != null) {
                    Brush.linearGradient(
                        listOf(tint.copy(alpha = 0.45f), tint.copy(alpha = 0.18f))
                    )
                } else {
                    Brush.linearGradient(listOf(DeliColors.SurfaceVariant, DeliColors.Surface))
                }
            )
            .border(1.dp, (tint ?: DeliColors.Border).copy(alpha = if (tint != null) 0.6f else 1f), CircleShape),
        contentAlignment = Alignment.Center,
        propagateMinConstraints = true,
    ) {
        if (label != null) {
            Text(
                text = label.first().uppercase(),
                color = DeliColors.OnBackground,
                fontSize = (size.value * 0.44f).sp,
                fontWeight = FontWeight.Bold,
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

/** Card surface used by every list on the site (`.card` / `.feed-card`). */
@Composable
fun DeliCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(DeliColors.Surface)
            .border(1.dp, DeliColors.Border, RoundedCornerShape(8.dp))
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
    ) {
        Column(modifier = Modifier.padding(12.dp)) { content() }
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
    Text(
        text = text,
        modifier = modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 10.dp),
        color = DeliColors.OnBackground,
        fontSize = 18.sp,
        fontWeight = FontWeight.SemiBold,
    )
}

/**
 * The up/down arrow pair plus score, exactly like the `.feed-actions` row on a card.
 * When the viewer cannot vote the arrows are dimmed instead of hidden, matching the
 * way the site links the arrows to /signup for guests.
 */
@Composable
fun VoteArrows(
    score: Int?,
    viewerVote: Int,
    canVote: Boolean,
    onVote: (Int) -> Unit,
    compact: Boolean = false,
) {
    val size = if (compact) 20.dp else 26.dp
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Filled.KeyboardArrowUp,
            contentDescription = "Upvote",
            tint = when {
                viewerVote > 0 -> DeliColors.Upvote
                canVote -> DeliColors.OnSurfaceVariant
                else -> DeliColors.Border
            },
            modifier = Modifier
                .size(size)
                .then(if (canVote) Modifier.clickable { onVote(if (viewerVote == 1) 0 else 1) } else Modifier),
        )
        Text(
            text = score?.toString() ?: "–",
            color = when (viewerVote) {
                1 -> DeliColors.Upvote
                -1 -> DeliColors.Downvote
                else -> DeliColors.OnSurfaceVariant
            },
            fontSize = if (compact) 12.sp else 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        Icon(
            imageVector = Icons.Filled.KeyboardArrowDown,
            contentDescription = "Downvote",
            tint = when {
                viewerVote < 0 -> DeliColors.Downvote
                canVote -> DeliColors.OnSurfaceVariant
                else -> DeliColors.Border
            },
            modifier = Modifier
                .size(size)
                .then(if (canVote) Modifier.clickable { onVote(if (viewerVote == -1) 0 else -1) } else Modifier),
        )
    }
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
fun DeliButtonPrimary(text: String, enabled: Boolean = true, onClick: () -> Unit) {
    androidx.compose.material3.Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(6.dp),
        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
            containerColor = DeliColors.Primary,
            contentColor = androidx.compose.ui.graphics.Color.White,
        ),
    ) { Text(text, fontWeight = FontWeight.SemiBold) }
}

@Composable
fun DeliOutlinedButton(text: String, enabled: Boolean = true, onClick: () -> Unit) {
    androidx.compose.material3.OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(6.dp),
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
