package com.delidehset.mobile.ui.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.sin

/** How a kit draws the surfaces a list is made of. */
enum class CardStyle {
    /** A hairline around the fill — the app's original look. */
    OUTLINED,

    /** No line, lifted off the page with a shadow. */
    ELEVATED,

    /** No line and no shadow: flat panels on a flat background. */
    FLAT,
}

/** Round like every avatar, or squared off like a tile. */
enum class AvatarShape { CIRCLE, SQUARE }

/** How the kit paints its buttons. */
enum class ButtonStyle {
    /** Solid accent with white text. */
    FILLED,

    /** A tinted container with the normal text colour. */
    SOFT,

    /** An outline and accent text. */
    OUTLINED,
}

fun cardShape(kit: Palette): Shape = RoundedCornerShape(kit.cardRadius)

fun controlShape(kit: Palette): Shape = RoundedCornerShape(kit.controlRadius)

fun avatarShape(kit: Palette): Shape =
    if (kit.avatarShape == AvatarShape.CIRCLE) CircleShape else RoundedCornerShape(kit.controlRadius)

/** The padding a card's inside gets in this kit: the kit's own air, not a fixed 12 dp. */
fun cardPadding(kit: Palette): PaddingValues = PaddingValues(kit.spacing)

/** The padding a list row gets in this kit. */
fun rowPadding(kit: Palette): PaddingValues =
    PaddingValues(horizontal = kit.spacing, vertical = kit.spacing * 0.8f)

/**
 * Custom architectural canvas details for [KitCategory.DETAILED] kits:
 * HUD corner brackets & CRT scanlines for NEON, glowing energy strips & radial aura for PULSE,
 * topographic wave rings for RIPPLE, frosted glass specular beams for FLOAT, carbon/luxury
 * inset frames for SHIFT, bokeh orbs for BOUNCE, and ruled manuscript lines for editorial kits.
 */
fun Modifier.kitDetailedDecoration(
    kit: Palette,
    glowAlpha: Float = 0.7f,
): Modifier {
    if (kit.category != KitCategory.DETAILED) return this
    val accent = if (kit.glowColor != Color.Transparent) kit.glowColor else kit.primary
    return this.drawWithContent {
        val w = size.width
        val h = size.height

        // 1. Background pattern layer (behind card content)
        when (kit.motion) {
            KitMotion.NEON -> {
                // CRT horizontal scanlines
                val step = 5.dp.toPx().coerceAtLeast(3f)
                var y = step
                val scanColor = accent.copy(alpha = 0.05f)
                while (y < h) {
                    drawLine(scanColor, Offset(0f, y), Offset(w, y), strokeWidth = 1f)
                    y += step
                }
                // Diagonal cyber accent glow in top-right
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(accent.copy(alpha = 0.16f * glowAlpha), Color.Transparent),
                        center = Offset(w * 0.9f, h * 0.15f),
                        radius = (w * 0.45f).coerceAtLeast(40f),
                    ),
                    radius = (w * 0.45f).coerceAtLeast(40f),
                    center = Offset(w * 0.9f, h * 0.15f),
                )
            }
            KitMotion.PULSE -> {
                // Radial spotlight aura from top-left
                val radius = (w * 0.55f).coerceAtLeast(60f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(accent.copy(alpha = 0.18f * glowAlpha), Color.Transparent),
                        center = Offset(w * 0.08f, h * 0.15f),
                        radius = radius,
                    ),
                    radius = radius,
                    center = Offset(w * 0.08f, h * 0.15f),
                )
            }
            KitMotion.RIPPLE -> {
                // Concentric topographic wave rings in top-right corner
                val ringColor = accent.copy(alpha = 0.08f)
                val center = Offset(w * 0.92f, h * 0.12f)
                for (i in 1..4) {
                    drawCircle(
                        color = ringColor,
                        radius = (18.dp.toPx() * i),
                        center = center,
                        style = Stroke(width = 1.2.dp.toPx()),
                    )
                }
                // Gentle wave curve near the bottom
                val wavePath = Path().apply {
                    val baseY = h - 8.dp.toPx()
                    moveTo(0f, baseY)
                    val segments = 24
                    for (i in 1..segments) {
                        val x = w * i / segments
                        val wy = baseY + sin(i * 0.6f) * 3.dp.toPx()
                        lineTo(x, wy)
                    }
                }
                drawPath(wavePath, color = accent.copy(alpha = 0.12f), style = Stroke(width = 1.2.dp.toPx()))
            }
            KitMotion.FLOAT -> {
                // Frosted glass diagonal specular sheen bands
                val sheenAlpha = if (kit.dark) 0.045f else 0.35f
                val beam = Path().apply {
                    moveTo(w * 0.18f, 0f)
                    lineTo(w * 0.42f, 0f)
                    lineTo(w * 0.24f, h)
                    lineTo(0f, h)
                    close()
                }
                drawPath(beam, color = Color.White.copy(alpha = sheenAlpha))
                val beam2 = Path().apply {
                    moveTo(w * 0.48f, 0f)
                    lineTo(w * 0.55f, 0f)
                    lineTo(w * 0.37f, h)
                    lineTo(w * 0.30f, h)
                    close()
                }
                drawPath(beam2, color = Color.White.copy(alpha = sheenAlpha * 0.7f))
                // Frost/star dots in top-right
                drawCircle(accent.copy(alpha = 0.22f), radius = 2.5.dp.toPx(), center = Offset(w - 18.dp.toPx(), 14.dp.toPx()))
                drawCircle(accent.copy(alpha = 0.14f), radius = 1.8.dp.toPx(), center = Offset(w - 30.dp.toPx(), 22.dp.toPx()))
            }
            KitMotion.SHIFT -> {
                if (kit.surfaceEffect == SurfaceEffect.METALLIC) {
                    // Brushed carbon diagonal hatch lines in the right corner
                    val hatchColor = accent.copy(alpha = 0.06f)
                    val step = 9.dp.toPx().coerceAtLeast(5f)
                    var d = -h
                    while (d < w) {
                        drawLine(
                            color = hatchColor,
                            start = Offset(d.coerceAtLeast(0f), (d - w * 0.5f).coerceAtLeast(0f)),
                            end = Offset((d + h).coerceAtMost(w), h),
                            strokeWidth = 1f,
                        )
                        d += step
                    }
                } else {
                    // Dual-orb gradient mesh glow
                    val r = (w * 0.45f).coerceAtLeast(50f)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(accent.copy(alpha = 0.14f), Color.Transparent),
                            center = Offset(w * 0.88f, h * 0.2f),
                            radius = r,
                        ),
                        radius = r,
                        center = Offset(w * 0.88f, h * 0.2f),
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(kit.upvote.copy(alpha = 0.09f), Color.Transparent),
                            center = Offset(w * 0.12f, h * 0.85f),
                            radius = r * 0.8f,
                        ),
                        radius = r * 0.8f,
                        center = Offset(w * 0.12f, h * 0.85f),
                    )
                }
            }
            KitMotion.BOUNCE -> {
                // Soft playful bokeh bubbles
                drawCircle(accent.copy(alpha = 0.08f), radius = 26.dp.toPx(), center = Offset(w - 24.dp.toPx(), 20.dp.toPx()))
                drawCircle(kit.blue.copy(alpha = 0.07f), radius = 16.dp.toPx(), center = Offset(w - 52.dp.toPx(), 34.dp.toPx()))
                drawCircle(kit.upvote.copy(alpha = 0.06f), radius = 20.dp.toPx(), center = Offset(28.dp.toPx(), h - 18.dp.toPx()))
            }
            KitMotion.NONE -> {
                // Editorial manuscript ruled lines + margin line (Kâğıt & Mürekkep)
                val lineStep = 20.dp.toPx().coerceAtLeast(12f)
                var y = lineStep
                val ruleColor = kit.border.copy(alpha = 0.38f)
                while (y < h) {
                    drawLine(ruleColor, Offset(0f, y), Offset(w, y), strokeWidth = 1f)
                    y += lineStep
                }
                val marginX = 6.dp.toPx()
                drawLine(kit.danger.copy(alpha = 0.22f), Offset(marginX, 0f), Offset(marginX, h), strokeWidth = 1.2.dp.toPx())
            }
        }

        // 2. Actual card content
        drawContent()

        // 3. Foreground architectural frame accents (at outer edges, outside padding)
        when (kit.motion) {
            KitMotion.NEON -> {
                // Cyber HUD L-brackets on corners
                val len = 14.dp.toPx()
                val stroke = 2.dp.toPx()
                val bracketColor = accent.copy(alpha = (0.85f * glowAlpha).coerceIn(0.4f, 1f))
                // Top-left
                drawLine(bracketColor, Offset(0f, 0f), Offset(len, 0f), strokeWidth = stroke)
                drawLine(bracketColor, Offset(0f, 0f), Offset(0f, len), strokeWidth = stroke)
                // Top-right
                drawLine(bracketColor, Offset(w - len, 0f), Offset(w, 0f), strokeWidth = stroke)
                drawLine(bracketColor, Offset(w, 0f), Offset(w, len), strokeWidth = stroke)
                // Bottom-left
                drawLine(bracketColor, Offset(0f, h - len), Offset(0f, h), strokeWidth = stroke)
                drawLine(bracketColor, Offset(0f, h), Offset(len, h), strokeWidth = stroke)
                // Bottom-right
                drawLine(bracketColor, Offset(w - len, h), Offset(w, h), strokeWidth = stroke)
                drawLine(bracketColor, Offset(w, h - len), Offset(w, h), strokeWidth = stroke)
            }
            KitMotion.PULSE -> {
                // Left glowing energy ribbon
                val barW = 3.5.dp.toPx()
                val insetY = (h * 0.14f).coerceAtMost(12.dp.toPx())
                if (h > insetY * 2f) {
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(accent.copy(alpha = 0.9f), kit.blue.copy(alpha = 0.7f), accent.copy(alpha = 0.9f)),
                            startY = insetY,
                            endY = h - insetY,
                        ),
                        topLeft = Offset(0f, insetY),
                        size = Size(barW, h - insetY * 2f),
                        cornerRadius = CornerRadius(barW, barW),
                    )
                }
            }
            KitMotion.FLOAT -> {
                // Top specular glass rim highlight
                drawLine(
                    brush = Brush.horizontalGradient(
                        listOf(Color.Transparent, Color.White.copy(alpha = 0.42f), accent.copy(alpha = 0.35f), Color.Transparent)
                    ),
                    start = Offset(w * 0.08f, 1.dp.toPx()),
                    end = Offset(w * 0.92f, 1.dp.toPx()),
                    strokeWidth = 1.5.dp.toPx(),
                )
            }
            KitMotion.SHIFT -> {
                if (kit.surfaceEffect == SurfaceEffect.METALLIC) {
                    // Inset metallic frame with corner rivets
                    val inset = 3.dp.toPx()
                    drawRect(
                        color = accent.copy(alpha = 0.22f),
                        topLeft = Offset(inset, inset),
                        size = Size((w - inset * 2).coerceAtLeast(0f), (h - inset * 2).coerceAtLeast(0f)),
                        style = Stroke(width = 1.dp.toPx()),
                    )
                    val r = 2.dp.toPx()
                    val rivetOffset = 6.dp.toPx()
                    val rivetColor = accent.copy(alpha = 0.65f)
                    drawCircle(rivetColor, r, Offset(rivetOffset, rivetOffset))
                    drawCircle(rivetColor, r, Offset(w - rivetOffset, rivetOffset))
                    drawCircle(rivetColor, r, Offset(rivetOffset, h - rivetOffset))
                    drawCircle(rivetColor, r, Offset(w - rivetOffset, h - rivetOffset))
                } else {
                    // Warm top gradient bar
                    drawLine(
                        brush = Brush.horizontalGradient(listOf(accent.copy(alpha = 0.7f), kit.upvote.copy(alpha = 0.5f), Color.Transparent)),
                        start = Offset(0f, 1.dp.toPx()),
                        end = Offset(w * 0.65f, 1.dp.toPx()),
                        strokeWidth = 2.dp.toPx(),
                    )
                }
            }
            KitMotion.BOUNCE -> {
                // Playful top pill accent indicator
                val pillW = (w * 0.22f).coerceIn(24.dp.toPx(), 56.dp.toPx())
                val pillH = 3.dp.toPx()
                drawRoundRect(
                    color = accent.copy(alpha = 0.6f),
                    topLeft = Offset((w - pillW) / 2f, 0f),
                    size = Size(pillW, pillH),
                    cornerRadius = CornerRadius(pillH, pillH),
                )
            }
            KitMotion.RIPPLE -> {
                // Left dual wave bar
                val barW = 2.5.dp.toPx()
                val insetY = (h * 0.18f).coerceAtMost(14.dp.toPx())
                if (h > insetY * 2f) {
                    drawRoundRect(
                        color = accent.copy(alpha = 0.65f),
                        topLeft = Offset(0f, insetY),
                        size = Size(barW, h - insetY * 2f),
                        cornerRadius = CornerRadius(barW, barW),
                    )
                }
            }
            KitMotion.NONE -> {
                // Editorial corner ticks
                val len = 8.dp.toPx()
                val c = kit.primary.copy(alpha = 0.45f)
                drawLine(c, Offset(0f, 0f), Offset(len, 0f), strokeWidth = 1.5.dp.toPx())
                drawLine(c, Offset(0f, 0f), Offset(0f, len), strokeWidth = 1.5.dp.toPx())
                drawLine(c, Offset(w - len, h), Offset(w, h), strokeWidth = 1.5.dp.toPx())
                drawLine(c, Offset(w, h - len), Offset(w, h), strokeWidth = 1.5.dp.toPx())
            }
        }
    }
}

/**
 * A surface in the kit's own idiom: filled, then either outlined, lifted or left bare,
 * then given that kit's padding.
 *
 * Every card in the app — a post, a settings section, the theme store itself — goes through
 * this, which is what makes a kit more than a palette: Grafit's flat panels with a line
 * around them and cuteeee's airy lifted ones are the same code with different answers.
 */
fun Modifier.kitSurface(
    kit: Palette = DeliColors.palette,
    shape: Shape = cardShape(kit),
    padding: Dp = kit.spacing,
): Modifier {
    var modifier = this
    if (kit.cardStyle == CardStyle.ELEVATED || (kit.category == KitCategory.DETAILED && (kit.surfaceEffect == SurfaceEffect.GLOW || kit.surfaceEffect == SurfaceEffect.NEON_BORDER))) {
        val spot = if (kit.category == KitCategory.DETAILED && kit.glowColor != Color.Transparent) {
            kit.glowColor.copy(alpha = 0.55f)
        } else {
            Color.Black.copy(alpha = 0.55f)
        }
        modifier = modifier.shadow(
            elevation = if (kit.category == KitCategory.DETAILED) 10.dp else 8.dp,
            shape = shape,
            ambientColor = spot,
            spotColor = spot,
        )
    }

    when (kit.surfaceEffect) {
        SurfaceEffect.GRADIENT -> {
            modifier = modifier.clip(shape).background(
                Brush.verticalGradient(listOf(kit.surface, kit.gradientEnd))
            )
        }
        SurfaceEffect.METALLIC -> {
            modifier = modifier.clip(shape).background(
                Brush.linearGradient(listOf(kit.surface, kit.surfaceVariant, kit.surface))
            )
        }
        SurfaceEffect.GLASS -> {
            modifier = modifier.clip(shape).background(
                Brush.verticalGradient(
                    listOf(
                        kit.surface.copy(alpha = if (kit.dark) 0.90f else 0.94f),
                        kit.surfaceVariant.copy(alpha = if (kit.dark) 0.82f else 0.88f),
                    )
                )
            )
        }
        else -> {
            modifier = modifier.clip(shape).background(kit.surface)
        }
    }

    modifier = modifier.kitDetailedDecoration(kit)

    when {
        kit.surfaceEffect == SurfaceEffect.NEON_BORDER -> {
            val tint = if (kit.glowColor != Color.Transparent) kit.glowColor else kit.primary
            modifier = modifier.border(1.5.dp, tint, shape)
        }
        kit.surfaceEffect == SurfaceEffect.GLOW && kit.glowColor != Color.Transparent -> {
            modifier = modifier.border(1.2.dp, kit.glowColor.copy(alpha = 0.55f), shape)
        }
        kit.cardStyle == CardStyle.OUTLINED -> {
            modifier = modifier.border(1.dp, kit.border, shape)
        }
    }

    return modifier.padding(padding)
}

/**
 * Interactive touch feedback that expresses the kit's motion character.
 * When a bouncy kit is on, clicks feel springy and responsive.
 */
@Composable
fun Modifier.kitInteractive(
    kit: Palette = DeliColors.palette,
    onClick: () -> Unit,
): Modifier {
    if (kit.motion != KitMotion.BOUNCE) {
        return this.clickable(onClick = onClick)
    }
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "kit_bounce_scale",
    )
    return this
        .scale(scale)
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick,
        )
}

/** What a kit's buttons are painted with. */
@Composable
fun kitButtonColors(kit: Palette = DeliColors.palette): ButtonColors = when (kit.buttonStyle) {
    ButtonStyle.FILLED -> ButtonDefaults.buttonColors(
        containerColor = kit.primary,
        contentColor = Color.White,
    )
    ButtonStyle.SOFT -> ButtonDefaults.buttonColors(
        containerColor = kit.primaryContainer,
        contentColor = kit.onBackground,
    )
    ButtonStyle.OUTLINED -> ButtonDefaults.buttonColors(
        containerColor = Color.Transparent,
        contentColor = kit.primary,
    )
}

/** The outline a kit's buttons carry when the button style asks for one. */
@Composable
fun Modifier.kitButtonOutline(kit: Palette = DeliColors.palette): Modifier =
    if (kit.buttonStyle == ButtonStyle.OUTLINED) {
        val shape = controlShape(kit)
        this.clip(shape).border(1.dp, kit.primary, shape)
    } else {
        this
    }

/**
 * The short words the store puts on a kit, so its structure is readable at a glance next
 * to the preview: `çerçeveli · yuvarlak · dolu · ferah`.
 */
fun kitTraits(kit: Palette): List<String> = buildList {
    if (kit.category == KitCategory.DETAILED) {
        when (kit.motion) {
            KitMotion.PULSE -> add("ışıma")
            KitMotion.SHIFT -> add("gradyan")
            KitMotion.BOUNCE -> add("yaylanan")
            KitMotion.FLOAT -> add("süzülen")
            KitMotion.RIPPLE -> add("dalgalanan")
            KitMotion.NEON -> add("neon")
            KitMotion.NONE -> {}
        }
        when (kit.surfaceEffect) {
            SurfaceEffect.GLASS -> add("buzlu cam")
            SurfaceEffect.METALLIC -> add("metalik")
            else -> {}
        }
    }
    add(
        when (kit.cardStyle) {
            CardStyle.OUTLINED -> "çerçeveli"
            CardStyle.ELEVATED -> "gölgeli"
            CardStyle.FLAT -> "düz"
        }
    )
    add(if (kit.avatarShape == AvatarShape.CIRCLE) "yuvarlak" else "köşeli")
    add(
        when (kit.buttonStyle) {
            ButtonStyle.FILLED -> "dolu"
            ButtonStyle.SOFT -> "yumuşak"
            ButtonStyle.OUTLINED -> "çerçeveli"
        }
    )
    add(
        when {
            kit.spacing >= 16.dp -> "ferah"
            kit.spacing <= 10.dp -> "sık"
            else -> "normal"
        }
    )
}.distinct()

