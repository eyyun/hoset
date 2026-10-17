package com.delidehset.mobile.ui.theme

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.filled.Hexagon
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Nightlife

/**
 * One look for the whole app: the fourteen colours every screen draws from.
 *
 * The first entry is the app's original look, read straight out of the website's own
 * stylesheets:
 *
 *  - themes/midnight.css  -> --bs-body-bg: #0d1117, --bs-body-secondary-bg: #161b22,
 *                            --bs-border-color: #262b32, --bs-body-color: #c9d1d9
 *  - <html style="--primary:#805ad5">  -> the favicon/purple accent
 *  - main.css             -> --downvote-color: #38b2ac
 *  - themes/midnight.css  -> --bs-orange: #fd7e14 (the upvote arrow colour)
 *
 * The rest are alternatives in the same shape — six dark, four light — so the app can be
 * something other than the site's own theme without any screen knowing which is on.
 */
data class Palette(
    val id: String,
    val name: String,

    /** The icon the kit wears in the store, and in the settings row once it is picked. */
    val icon: ImageVector,
    /** Light palettes need a light Material scheme under them; the dark ones do not. */
    val dark: Boolean,

    /**
     * How round the kit is. Three radii cover the app: cards and sheets, the controls on
     * them, and the small chips inside — so a kit can look soft or sharp without any
     * screen knowing which one is on.
     */
    val cardRadius: Dp,
    val controlRadius: Dp,
    val smallRadius: Dp,

    /** The weight this kit gives to titles. */
    val titleWeight: FontWeight,
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val border: Color,
    val onBackground: Color,
    val onSurfaceVariant: Color,
    val primary: Color,
    val primaryContainer: Color,
    val blue: Color,
    val upvote: Color,
    val downvote: Color,
    val success: Color,
    val warning: Color,
    val danger: Color,
)

/** The eleven looks the app ships with, in the order the picker shows them. */
object DeliPalettes {

    val all: List<Palette> = listOf(
        Palette(
            id = "hoset",
            name = "Hoset",
            icon = Icons.Filled.Hexagon,
            dark = true,
            cardRadius = 12.dp,
            controlRadius = 8.dp,
            smallRadius = 6.dp,
            titleWeight = FontWeight.SemiBold,
            background = Color(0xFF0D1117),
            surface = Color(0xFF161B22),
            surfaceVariant = Color(0xFF21262D),
            border = Color(0xFF262B32),
            onBackground = Color(0xFFC9D1D9),
            onSurfaceVariant = Color(0xFF8B949E),
            primary = Color(0xFF805AD5),
            primaryContainer = Color(0xFF3B2A63),
            blue = Color(0xFF58A6FF),
            upvote = Color(0xFFFD7E14),
            downvote = Color(0xFF38B2AC),
            success = Color(0xFF3FB950),
            warning = Color(0xFFD29922),
            danger = Color(0xFFF85149),
        ),
        Palette(
            id = "gece-mavisi",
            name = "Mavi Saat",
            icon = Icons.Filled.DarkMode,
            dark = true,
            cardRadius = 12.dp,
            controlRadius = 10.dp,
            smallRadius = 6.dp,
            titleWeight = FontWeight.Medium,
            background = Color(0xFF0A1220),
            surface = Color(0xFF111C2E),
            surfaceVariant = Color(0xFF16233A),
            border = Color(0xFF1E2E4A),
            onBackground = Color(0xFFC7D3E3),
            onSurfaceVariant = Color(0xFF8298B5),
            primary = Color(0xFF3B82F6),
            primaryContainer = Color(0xFF1E3A8A),
            blue = Color(0xFF60A5FA),
            upvote = Color(0xFFF97316),
            downvote = Color(0xFF22D3EE),
            success = Color(0xFF34D399),
            warning = Color(0xFFFBBF24),
            danger = Color(0xFFF87171),
        ),
        Palette(
            id = "orman",
            name = "Çamlık",
            icon = Icons.Filled.Park,
            dark = true,
            cardRadius = 16.dp,
            controlRadius = 12.dp,
            smallRadius = 8.dp,
            titleWeight = FontWeight.SemiBold,
            background = Color(0xFF0B1410),
            surface = Color(0xFF12201A),
            surfaceVariant = Color(0xFF182B22),
            border = Color(0xFF20382D),
            onBackground = Color(0xFFC6D8CC),
            onSurfaceVariant = Color(0xFF8AA796),
            primary = Color(0xFF22C55E),
            primaryContainer = Color(0xFF14532D),
            blue = Color(0xFF4ADE80),
            upvote = Color(0xFFFB923C),
            downvote = Color(0xFF2DD4BF),
            success = Color(0xFF4ADE80),
            warning = Color(0xFFEAB308),
            danger = Color(0xFFEF4444),
        ),
        Palette(
            id = "gun-batimi",
            name = "Kızıl Ufuk",
            icon = Icons.Filled.WbTwilight,
            dark = true,
            cardRadius = 18.dp,
            controlRadius = 12.dp,
            smallRadius = 8.dp,
            titleWeight = FontWeight.SemiBold,
            background = Color(0xFF1A0F0C),
            surface = Color(0xFF241712),
            surfaceVariant = Color(0xFF2E1D16),
            border = Color(0xFF3A251C),
            onBackground = Color(0xFFE8D3C6),
            onSurfaceVariant = Color(0xFFB08F7E),
            primary = Color(0xFFF97316),
            primaryContainer = Color(0xFF7C2D12),
            blue = Color(0xFFFDBA74),
            upvote = Color(0xFFFB7185),
            downvote = Color(0xFF38BDF8),
            success = Color(0xFF84CC16),
            warning = Color(0xFFFACC15),
            danger = Color(0xFFEF4444),
        ),
        Palette(
            id = "lavanta",
            name = "Lila",
            icon = Icons.Filled.Spa,
            dark = false,
            cardRadius = 20.dp,
            controlRadius = 14.dp,
            smallRadius = 10.dp,
            titleWeight = FontWeight.Medium,
            background = Color(0xFFF7F4FD),
            surface = Color(0xFFFFFFFF),
            surfaceVariant = Color(0xFFEFE9FA),
            border = Color(0xFFDDD3F0),
            onBackground = Color(0xFF241C36),
            onSurfaceVariant = Color(0xFF6B5E87),
            primary = Color(0xFF7C3AED),
            primaryContainer = Color(0xFFE9DDFF),
            blue = Color(0xFF2563EB),
            upvote = Color(0xFFEA580C),
            downvote = Color(0xFF0D9488),
            success = Color(0xFF16A34A),
            warning = Color(0xFFB45309),
            danger = Color(0xFFDC2626),
        ),
        Palette(
            id = "kar",
            name = "Beyaz Sayfa",
            icon = Icons.Filled.AcUnit,
            dark = false,
            cardRadius = 10.dp,
            controlRadius = 8.dp,
            smallRadius = 6.dp,
            titleWeight = FontWeight.Medium,
            background = Color(0xFFF6F8FA),
            surface = Color(0xFFFFFFFF),
            surfaceVariant = Color(0xFFEDF1F5),
            border = Color(0xFFD8DEE4),
            onBackground = Color(0xFF1F2328),
            onSurfaceVariant = Color(0xFF59636E),
            primary = Color(0xFF0969DA),
            primaryContainer = Color(0xFFDDF4FF),
            blue = Color(0xFF0969DA),
            upvote = Color(0xFFBC4C00),
            downvote = Color(0xFF1B7C83),
            success = Color(0xFF1A7F37),
            warning = Color(0xFF9A6700),
            danger = Color(0xFFCF222E),
        ),
        Palette(
            id = "grafit",
            name = "Grafit",
            icon = Icons.Filled.Bolt,
            dark = true,
            cardRadius = 4.dp,
            controlRadius = 4.dp,
            smallRadius = 2.dp,
            titleWeight = FontWeight.Bold,
            background = Color(0xFF111111),
            surface = Color(0xFF191919),
            surfaceVariant = Color(0xFF212121),
            border = Color(0xFF2A2A2A),
            onBackground = Color(0xFFD4D4D4),
            onSurfaceVariant = Color(0xFF8F8F8F),
            primary = Color(0xFFEF4444),
            primaryContainer = Color(0xFF7F1D1D),
            blue = Color(0xFFA1A1AA),
            upvote = Color(0xFFF59E0B),
            downvote = Color(0xFF71717A),
            success = Color(0xFF22C55E),
            warning = Color(0xFFEAB308),
            danger = Color(0xFFF87171),
        ),
        Palette(
            id = "deniz",
            name = "Derin Sular",
            icon = Icons.Filled.Waves,
            dark = true,
            cardRadius = 14.dp,
            controlRadius = 10.dp,
            smallRadius = 8.dp,
            titleWeight = FontWeight.SemiBold,
            background = Color(0xFF08191C),
            surface = Color(0xFF0F2629),
            surfaceVariant = Color(0xFF143034),
            border = Color(0xFF1D4247),
            onBackground = Color(0xFFC4DDE0),
            onSurfaceVariant = Color(0xFF7FA8AE),
            primary = Color(0xFF14B8A6),
            primaryContainer = Color(0xFF115E59),
            blue = Color(0xFF38BDF8),
            upvote = Color(0xFFFB923C),
            downvote = Color(0xFF67E8F9),
            success = Color(0xFF2DD4BF),
            warning = Color(0xFFFCD34D),
            danger = Color(0xFFFB7185),
        ),
        Palette(
            id = "seker",
            name = "Pamuk Şeker",
            icon = Icons.Filled.Cake,
            dark = false,
            cardRadius = 22.dp,
            controlRadius = 14.dp,
            smallRadius = 10.dp,
            titleWeight = FontWeight.SemiBold,
            background = Color(0xFFFFF5F7),
            surface = Color(0xFFFFFFFF),
            surfaceVariant = Color(0xFFFDE7EC),
            border = Color(0xFFF6CFD8),
            onBackground = Color(0xFF33161E),
            onSurfaceVariant = Color(0xFF8A5C67),
            primary = Color(0xFFE11D48),
            primaryContainer = Color(0xFFFFE4E6),
            blue = Color(0xFF2563EB),
            upvote = Color(0xFFF97316),
            downvote = Color(0xFF0EA5E9),
            success = Color(0xFF16A34A),
            warning = Color(0xFFC2410C),
            danger = Color(0xFFBE123C),
        ),
        // Pastels on purpose: the one palette that is meant to look soft rather than
        // serious, which is what its name is asking for.
        Palette(
            id = "cuteeee",
            name = "cuteeee",
            icon = Icons.Filled.Favorite,
            dark = false,
            cardRadius = 24.dp,
            controlRadius = 16.dp,
            smallRadius = 12.dp,
            titleWeight = FontWeight.Bold,
            background = Color(0xFFFFF6FB),
            surface = Color(0xFFFFFFFF),
            surfaceVariant = Color(0xFFFFE9F4),
            border = Color(0xFFF7C9E2),
            onBackground = Color(0xFF5A3A4E),
            onSurfaceVariant = Color(0xFF9B7791),
            primary = Color(0xFFF48FB1),
            primaryContainer = Color(0xFFFFD9EA),
            blue = Color(0xFF9CC7F0),
            upvote = Color(0xFFFFB07C),
            downvote = Color(0xFF9AE6D0),
            success = Color(0xFF9FD8A8),
            warning = Color(0xFFF7D08A),
            danger = Color(0xFFF4A0A8),
        ),
        Palette(
            id = "neon",
            name = "Neon Gece",
            icon = Icons.Filled.Nightlife,
            dark = true,
            cardRadius = 2.dp,
            controlRadius = 2.dp,
            smallRadius = 2.dp,
            titleWeight = FontWeight.Bold,
            background = Color(0xFF05060A),
            surface = Color(0xFF0B0D14),
            surfaceVariant = Color(0xFF12141F),
            border = Color(0xFF1D2030),
            onBackground = Color(0xFFE6E9F5),
            onSurfaceVariant = Color(0xFF8B90A8),
            primary = Color(0xFFE879F9),
            primaryContainer = Color(0xFF4A1D5C),
            blue = Color(0xFF22D3EE),
            upvote = Color(0xFFFDE047),
            downvote = Color(0xFF22D3EE),
            success = Color(0xFF4ADE80),
            warning = Color(0xFFFDE047),
            danger = Color(0xFFFB7185),
        ),
    )

    val fallback: Palette = all.first()

    fun of(id: String?): Palette = all.firstOrNull { it.id == id } ?: fallback
}

/**
 * The colours the screens read, which follow whichever palette is picked.
 *
 * These stay plain properties — `DeliColors.Background` — because every screen in the app
 * is written against them. What makes them swappable is that they now read a snapshot
 * state: a screen that draws `DeliColors.Surface` is subscribed to the palette, so picking
 * a different one repaints the whole app without any screen being aware of the choice.
 */
object DeliColors {

    private val current = mutableStateOf(DeliPalettes.fallback)

    val palette: Palette get() = current.value

    fun apply(id: String?) {
        current.value = DeliPalettes.of(id)
    }

    val Background: Color get() = current.value.background
    val Surface: Color get() = current.value.surface
    val SurfaceVariant: Color get() = current.value.surfaceVariant
    val Border: Color get() = current.value.border
    val OnBackground: Color get() = current.value.onBackground
    val OnSurfaceVariant: Color get() = current.value.onSurfaceVariant
    val Primary: Color get() = current.value.primary
    val PrimaryContainer: Color get() = current.value.primaryContainer
    val Blue: Color get() = current.value.blue
    val Upvote: Color get() = current.value.upvote
    val Downvote: Color get() = current.value.downvote
    val Success: Color get() = current.value.success
    val Warning: Color get() = current.value.warning
    val Danger: Color get() = current.value.danger
}
