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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

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
    if (kit.cardStyle == CardStyle.ELEVATED) {
        modifier = modifier.shadow(
            elevation = 8.dp,
            shape = shape,
            ambientColor = Color.Black.copy(alpha = 0.55f),
            spotColor = Color.Black.copy(alpha = 0.55f),
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
                kit.surface.copy(alpha = if (kit.dark) 0.85f else 0.90f)
            )
        }
        else -> {
            modifier = modifier.clip(shape).background(kit.surface)
        }
    }

    when {
        kit.surfaceEffect == SurfaceEffect.NEON_BORDER -> {
            val tint = if (kit.glowColor != Color.Transparent) kit.glowColor else kit.primary
            modifier = modifier.border(1.5.dp, tint, shape)
        }
        kit.surfaceEffect == SurfaceEffect.GLOW && kit.glowColor != Color.Transparent -> {
            modifier = modifier.border(1.dp, kit.glowColor.copy(alpha = 0.45f), shape)
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

