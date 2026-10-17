package com.delidehset.mobile.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * The Material scheme that matches the palette in force.
 *
 * The app draws almost everything from [DeliColors] directly, so this is the part that
 * matters to the widgets that read Material's own tokens — dialogs, text fields, ripples,
 * the snackbar. Light palettes get a light scheme underneath for the same reason.
 */
private fun schemeFor(palette: Palette) = if (palette.dark) {
    darkColorScheme(
        primary = palette.primary,
        onPrimary = Color.White,
        primaryContainer = palette.primaryContainer,
        onPrimaryContainer = Color.White,
        secondary = palette.blue,
        onSecondary = Color.White,
        tertiary = palette.downvote,
        onTertiary = Color.White,
        background = palette.background,
        onBackground = palette.onBackground,
        surface = palette.surface,
        onSurface = palette.onBackground,
        surfaceVariant = palette.surfaceVariant,
        onSurfaceVariant = palette.onSurfaceVariant,
        outline = palette.border,
        outlineVariant = palette.border,
        error = palette.danger,
        onError = Color.White,
        scrim = Color(0xCC000000),
    )
} else {
    lightColorScheme(
        primary = palette.primary,
        onPrimary = Color.White,
        primaryContainer = palette.primaryContainer,
        onPrimaryContainer = palette.onBackground,
        secondary = palette.blue,
        onSecondary = Color.White,
        tertiary = palette.downvote,
        onTertiary = Color.White,
        background = palette.background,
        onBackground = palette.onBackground,
        surface = palette.surface,
        onSurface = palette.onBackground,
        surfaceVariant = palette.surfaceVariant,
        onSurfaceVariant = palette.onSurfaceVariant,
        outline = palette.border,
        outlineVariant = palette.border,
        error = palette.danger,
        onError = Color.White,
        scrim = Color(0x66000000),
    )
}

/**
 * Material's own corner tokens, filled from the kit's radii.
 *
 * Everything the app draws itself goes through [DeliShapes]; this is for the widgets that
 * come with the framework — dialogs, menus, text fields, ripples — so they belong to the
 * same kit instead of keeping Material's default 28 dp sheet corners.
 */
private fun shapesFor(palette: Palette) = Shapes(
    extraSmall = RoundedCornerShape(palette.smallRadius),
    small = RoundedCornerShape(palette.controlRadius),
    medium = RoundedCornerShape(palette.cardRadius),
    large = RoundedCornerShape(palette.cardRadius),
    extraLarge = RoundedCornerShape(palette.cardRadius),
)

/**
 * The app's theme, following the kit picked in Ayarlar.
 *
 * Reading [DeliColors.palette] here is what makes a change of kit reach the whole tree:
 * the scheme, the shapes and the title weight are rebuilt, and every screen reading a
 * `DeliColors` or `DeliShapes` token repaints.
 */
@Composable
fun DeliTheme(content: @Composable () -> Unit) {
    val palette = DeliColors.palette
    val base = MaterialTheme.typography
    MaterialTheme(
        colorScheme = schemeFor(palette),
        shapes = shapesFor(palette),
        // Only the lines nobody styles by hand take this: the kit's voice on top of
        // colours and corners, without touching the type the screens set themselves.
        typography = base.copy(
            headlineSmall = base.headlineSmall.copy(fontWeight = palette.titleWeight),
            titleLarge = base.titleLarge.copy(fontWeight = palette.titleWeight),
            titleMedium = base.titleMedium.copy(fontWeight = palette.titleWeight),
        ),
        content = content,
    )
}
