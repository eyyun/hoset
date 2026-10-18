package com.delidehset.mobile.data

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Hexagon
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.delidehset.mobile.ui.theme.AvatarShape
import com.delidehset.mobile.ui.theme.ButtonStyle
import com.delidehset.mobile.ui.theme.CardStyle
import com.delidehset.mobile.ui.theme.DeliPalettes
import com.delidehset.mobile.ui.theme.KitCategory
import com.delidehset.mobile.ui.theme.KitMotion
import com.delidehset.mobile.ui.theme.Palette
import com.delidehset.mobile.ui.theme.SurfaceEffect
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.themeDataStore by preferencesDataStore(name = "delidehset_theme")

@Serializable
data class CustomThemeSpec(
    val id: String,
    val name: String,
    val dark: Boolean = true,
    val cardRadiusDp: Int = 12,
    val controlRadiusDp: Int = 8,
    val smallRadiusDp: Int = 6,
    val cardStyle: String = "OUTLINED",
    val avatarShape: String = "CIRCLE",
    val buttonStyle: String = "FILLED",
    val dividers: Boolean = true,
    val spacingDp: Int = 12,
    val backgroundHex: Long = 0xFF0D1117,
    val surfaceHex: Long = 0xFF161B22,
    val primaryHex: Long = 0xFF805AD5,
    val blueHex: Long = 0xFF58A6FF,
    val category: String = "SIMPLE",
    val motion: String = "NONE",
    val surfaceEffect: String = "SOLID",
) {
    fun toPalette(): Palette {
        val bg = Color(backgroundHex)
        val surf = Color(surfaceHex)
        val prim = Color(primaryHex)
        val bl = Color(blueHex)
        val surfVar = if (dark) blendColor(surf, Color.White, 0.06f) else blendColor(surf, Color.Black, 0.06f)
        val bord = if (dark) blendColor(surf, Color.White, 0.12f) else blendColor(surf, Color.Black, 0.12f)
        val onBg = if (dark) Color(0xFFC9D1D9) else Color(0xFF1C2026)
        val onSurfVar = if (dark) Color(0xFF8B949E) else Color(0xFF606875)
        val primCont = blendColor(prim, bg, 0.65f)
        val base = DeliPalettes.fallback
        val cat = runCatching { KitCategory.valueOf(category) }.getOrDefault(KitCategory.SIMPLE)
        val mot = runCatching { KitMotion.valueOf(motion) }.getOrDefault(KitMotion.NONE)
        val eff = runCatching { SurfaceEffect.valueOf(surfaceEffect) }.getOrDefault(SurfaceEffect.SOLID)
        return Palette(
            id = id,
            name = name,
            icon = Icons.Filled.Hexagon,
            dark = dark,
            cardRadius = cardRadiusDp.dp,
            controlRadius = controlRadiusDp.dp,
            smallRadius = smallRadiusDp.dp,
            titleWeight = FontWeight.SemiBold,
            cardStyle = runCatching { CardStyle.valueOf(cardStyle) }.getOrDefault(CardStyle.OUTLINED),
            avatarShape = runCatching { AvatarShape.valueOf(avatarShape) }.getOrDefault(AvatarShape.CIRCLE),
            buttonStyle = runCatching { ButtonStyle.valueOf(buttonStyle) }.getOrDefault(ButtonStyle.FILLED),
            dividers = dividers,
            spacing = spacingDp.dp,
            letterSpacing = 0.sp,
            background = bg,
            surface = surf,
            surfaceVariant = surfVar,
            border = bord,
            onBackground = onBg,
            onSurfaceVariant = onSurfVar,
            primary = prim,
            primaryContainer = primCont,
            blue = bl,
            upvote = base.upvote,
            downvote = base.downvote,
            success = base.success,
            warning = base.warning,
            danger = base.danger,
            category = cat,
            motion = mot,
            surfaceEffect = eff,
            glowColor = if (eff == SurfaceEffect.GLOW || eff == SurfaceEffect.NEON_BORDER) prim else Color.Transparent,
            gradientEnd = surfVar,
        )
    }
}

private fun blendColor(a: Color, b: Color, ratio: Float): Color {
    val r = ratio.coerceIn(0f, 1f)
    return Color(
        red = a.red * (1f - r) + b.red * r,
        green = a.green * (1f - r) + b.green * r,
        blue = a.blue * (1f - r) + b.blue * r,
        alpha = 1f,
    )
}

fun Color.toHexLong(): Long = toArgb().toUInt().toLong()

/**
 * The look of the app itself, plus user-built custom kits.
 */
class ThemeStore(private val context: Context) {

    private val key = stringPreferencesKey("app_theme")
    private val customKey = stringPreferencesKey("custom_themes_json")
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    /** The chosen palette id, or null while the user has never picked one. */
    val theme: Flow<String?> = context.themeDataStore.data.map { it[key] }

    /** User-created custom kits. */
    val customSpecs: Flow<List<CustomThemeSpec>> = context.themeDataStore.data.map { prefs ->
        val raw = prefs[customKey] ?: return@map emptyList()
        runCatching { json.decodeFromString<List<CustomThemeSpec>>(raw) }.getOrDefault(emptyList())
    }

    suspend fun save(id: String) {
        context.themeDataStore.edit { it[key] = id }
    }

    suspend fun saveCustom(spec: CustomThemeSpec) {
        context.themeDataStore.edit { prefs ->
            val current = prefs[customKey]
                ?.let { runCatching { json.decodeFromString<List<CustomThemeSpec>>(it) }.getOrNull() }
                .orEmpty()
                .filterNot { it.id == spec.id }
            val next = current + spec
            prefs[customKey] = json.encodeToString(next)
            prefs[key] = spec.id
        }
    }

    suspend fun deleteCustom(id: String) {
        context.themeDataStore.edit { prefs ->
            val current = prefs[customKey]
                ?.let { runCatching { json.decodeFromString<List<CustomThemeSpec>>(it) }.getOrNull() }
                .orEmpty()
                .filterNot { it.id == id }
            prefs[customKey] = json.encodeToString(current)
            if (prefs[key] == id) {
                prefs[key] = DeliPalettes.fallback.id
            }
        }
    }
}
