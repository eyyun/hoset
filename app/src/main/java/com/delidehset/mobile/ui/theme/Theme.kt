package com.delidehset.mobile.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DeliDarkScheme = darkColorScheme(
    primary = DeliColors.Primary,
    onPrimary = Color.White,
    primaryContainer = DeliColors.PrimaryContainer,
    onPrimaryContainer = Color.White,
    secondary = DeliColors.Blue,
    onSecondary = Color.White,
    tertiary = DeliColors.Downvote,
    onTertiary = Color.White,
    background = DeliColors.Background,
    onBackground = DeliColors.OnBackground,
    surface = DeliColors.Surface,
    onSurface = DeliColors.OnBackground,
    surfaceVariant = DeliColors.SurfaceVariant,
    onSurfaceVariant = DeliColors.OnSurfaceVariant,
    outline = DeliColors.Border,
    outlineVariant = DeliColors.Border,
    error = DeliColors.Danger,
    onError = Color.White,
    scrim = Color(0xCC000000),
)

/** The site ships a single dark theme (midnight), so the app does too. */
@Composable
fun DeliTheme(
    @Suppress("UNUSED_PARAMETER") darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = DeliDarkScheme,
        typography = MaterialTheme.typography,
        content = content,
    )
}
