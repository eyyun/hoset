package com.delidehset.mobile.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Colour for a name, the way every chat client has it.
 *
 * Accounts here have no picture more often than not, so an avatar falls back to the
 * first letter of the name — and without a colour of its own the whole room ends up one
 * purple. Deriving the colour from the name makes it stable across screens and sessions:
 * @cronus is the same colour in the room, in the drawer and on the voice panel.
 */
fun nameColor(name: String): Color {
    if (name.isBlank()) return NamePalette[0]
    // A plain rolling hash: stable for the same name on every device and every run, which
    // is the whole point — a random or per-process colour would flicker between screens.
    var hash = 0
    for (ch in name.lowercase()) {
        hash = (hash * 31 + ch.code) and 0x7fffffff
    }
    return NamePalette[hash % NamePalette.size]
}

/** Readable on the app's dark background, and distinct enough to tell people apart. */
private val NamePalette = listOf(
    Color(0xFFA78BFA), // violet
    Color(0xFF58A6FF), // blue
    Color(0xFF3FB950), // green
    Color(0xFFE3B341), // amber
    Color(0xFFF778BA), // pink
    Color(0xFF56D4DD), // cyan
    Color(0xFFFF8A65), // orange
    Color(0xFF9BE15D), // lime
    Color(0xFFD2A8FF), // lavender
    Color(0xFF7EE787), // mint
)

/**
 * A colour the room itself sent for a user — `#rrggbb`, `#rgb` or `rgb(...)` — or null when
 * there is nothing usable. The room's own paint is preferred over [nameColor] so a user who
 * picked a colour on the site keeps it here.
 */
fun parseCssColor(raw: String?): Color? {
    val value = raw?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    if (value.startsWith("#")) {
        val hex = value.substring(1)
        val expanded = when (hex.length) {
            3 -> hex.map { "$it$it" }.joinToString("")
            6 -> hex
            else -> return null
        }
        val parsed = expanded.toLongOrNull(16) ?: return null
        return Color(0xFF000000L or parsed)
    }
    if (value.startsWith("rgb", ignoreCase = true)) {
        val inside = value.substringAfter('(', "").substringBefore(')')
        val parts = inside.split(',').map { it.trim().toFloatOrNull() }
        if (parts.size < 3 || parts.take(3).any { it == null }) return null
        val alpha = parts.getOrNull(3)?.coerceIn(0f, 1f) ?: 1f
        return Color(
            red = (parts[0]!! / 255f).coerceIn(0f, 1f),
            green = (parts[1]!! / 255f).coerceIn(0f, 1f),
            blue = (parts[2]!! / 255f).coerceIn(0f, 1f),
            alpha = alpha,
        )
    }
    return null
}
