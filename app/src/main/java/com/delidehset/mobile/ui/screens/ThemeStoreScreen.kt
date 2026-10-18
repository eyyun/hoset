package com.delidehset.mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.delidehset.mobile.data.CustomThemeSpec
import com.delidehset.mobile.ui.Text
import com.delidehset.mobile.ui.components.DeliButtonPrimary
import com.delidehset.mobile.ui.t
import com.delidehset.mobile.ui.tf
import com.delidehset.mobile.ui.theme.AvatarShape
import com.delidehset.mobile.ui.theme.ButtonStyle
import com.delidehset.mobile.ui.theme.CardStyle
import com.delidehset.mobile.ui.theme.DeliColors
import com.delidehset.mobile.ui.theme.DeliPalettes
import com.delidehset.mobile.ui.theme.DeliShapes
import com.delidehset.mobile.ui.theme.Palette
import com.delidehset.mobile.ui.theme.avatarShape
import com.delidehset.mobile.ui.theme.controlShape
import com.delidehset.mobile.ui.theme.kitTraits

/**
 * The theme store: every kit the app ships with, plus custom user-created kits.
 *
 * Each entry carries a preview built out of the things a kit actually changes — a top bar,
 * a post with its votes, a couple of buttons and the bottom bar — with *that* kit's colours,
 * corners, chrome, avatar shape and air.
 */
@Composable
fun ThemeStoreScreen(
    current: Palette,
    onApply: (Palette) -> Unit,
    onSaveCustom: (CustomThemeSpec) -> Unit = {},
    onDeleteCustom: (String) -> Unit = {},
    contentPadding: PaddingValues,
) {
    var showBuilder by remember { mutableStateOf(false) }
    val allKits = DeliPalettes.available

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(DeliColors.Background),
        contentPadding = PaddingValues(
            start = 14.dp,
            end = 14.dp,
            top = 12.dp,
            bottom = contentPadding.calculateBottomPadding() + 28.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = t("Tema Mağazası"),
                        color = DeliColors.OnBackground,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = tf(
                            "{0} kit var. Beğendiğine dokun, uygulama baştan aşağı o renklere ve köşelere geçer.",
                            allKits.size,
                        ),
                        color = DeliColors.OnSurfaceVariant,
                        fontSize = 12.5.sp,
                    )
                }
                Spacer(Modifier.width(10.dp))
                Box(
                    modifier = Modifier
                        .clip(DeliShapes.control)
                        .background(DeliColors.Primary)
                        .clickable { showBuilder = true }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(t("Tema Oluştur"), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        items(allKits, key = { it.id }) { kit ->
            val isCustom = kit.id.startsWith("custom_")
            KitCard(
                kit = kit,
                active = kit.id == current.id,
                onApply = { onApply(kit) },
                onDelete = if (isCustom) { { onDeleteCustom(kit.id) } } else null,
            )
        }
    }

    if (showBuilder) {
        CustomThemeBuilderDialog(
            onDismiss = { showBuilder = false },
            onSave = { spec ->
                onSaveCustom(spec)
                showBuilder = false
            },
        )
    }
}

private val ACCENT_PRESETS = listOf(
    0xFF805AD5L, 0xFF3B82F6L, 0xFF10B981L, 0xFFF43F5EL,
    0xFFF59E0BL, 0xFFEC4899L, 0xFF06B6D4L, 0xFFA855F7L,
)

private val BG_DARK_PRESETS = listOf(
    0xFF0D1117L to 0xFF161B22L,
    0xFF05060AL to 0xFF0B0D14L,
    0xFF0F172AL to 0xFF1E293BL,
    0xFF12101BL to 0xFF1C1929L,
    0xFF0A1410L to 0xFF12211BL,
)

private val BG_LIGHT_PRESETS = listOf(
    0xFFF8FAFCL to 0xFFFFFFFFL,
    0xFFFFF5F7L to 0xFFFFFFFFL,
    0xFFF5F3FFL to 0xFFFFFFFFL,
    0xFFFEFCE8L to 0xFFFFFFFFL,
)

@Composable
private fun CustomThemeBuilderDialog(
    onDismiss: () -> Unit,
    onSave: (CustomThemeSpec) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var dark by remember { mutableStateOf(true) }
    var primaryHex by remember { mutableLongStateOf(ACCENT_PRESETS.first()) }
    var bgPair by remember { mutableStateOf(BG_DARK_PRESETS.first()) }
    var cardStyle by remember { mutableStateOf("OUTLINED") }
    var avatarShape by remember { mutableStateOf("CIRCLE") }
    var buttonStyle by remember { mutableStateOf("FILLED") }
    var radiusStep by remember { mutableIntStateOf(12) }
    var spacingStep by remember { mutableIntStateOf(12) }
    var dividers by remember { mutableStateOf(true) }

    val previewSpec = remember(name, dark, primaryHex, bgPair, cardStyle, avatarShape, buttonStyle, radiusStep, spacingStep, dividers) {
        CustomThemeSpec(
            id = "custom_preview",
            name = name.ifBlank { "Özel Tema" },
            dark = dark,
            cardRadiusDp = radiusStep,
            controlRadiusDp = (radiusStep * 2 / 3).coerceAtLeast(2),
            smallRadiusDp = (radiusStep / 2).coerceAtLeast(2),
            cardStyle = cardStyle,
            avatarShape = avatarShape,
            buttonStyle = buttonStyle,
            dividers = dividers,
            spacingDp = spacingStep,
            backgroundHex = bgPair.first,
            surfaceHex = bgPair.second,
            primaryHex = primaryHex,
            blueHex = primaryHex,
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DeliColors.Surface,
        title = {
            Text(t("Tema Oluştur"), color = DeliColors.OnBackground, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(28) },
                    singleLine = true,
                    placeholder = { Text(t("Tema adı"), color = DeliColors.OnSurfaceVariant) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = DeliColors.Background,
                        unfocusedContainerColor = DeliColors.Background,
                        focusedTextColor = DeliColors.OnBackground,
                        unfocusedTextColor = DeliColors.OnBackground,
                    ),
                )

                // Mode: Dark / Light
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OptionChip(t("Koyu"), dark) {
                        dark = true
                        bgPair = BG_DARK_PRESETS.first()
                    }
                    OptionChip(t("Açık"), !dark) {
                        dark = false
                        bgPair = BG_LIGHT_PRESETS.first()
                    }
                }

                // Accent presets
                Text(t("Vurgu rengi"), color = DeliColors.OnSurfaceVariant, fontSize = 12.sp)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    for (hex in ACCENT_PRESETS) {
                        val selected = primaryHex == hex
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(Color(hex))
                                .border(
                                    width = if (selected) 2.5.dp else 1.dp,
                                    color = if (selected) Color.White else DeliColors.Border,
                                    shape = CircleShape,
                                )
                                .clickable { primaryHex = hex },
                        )
                    }
                }

                // Background presets
                Text(t("Zemin"), color = DeliColors.OnSurfaceVariant, fontSize = 12.sp)
                val bgOptions = if (dark) BG_DARK_PRESETS else BG_LIGHT_PRESETS
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    for (pair in bgOptions) {
                        val selected = bgPair == pair
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(DeliShapes.control)
                                .background(Color(pair.first))
                                .border(
                                    width = if (selected) 2.dp else 1.dp,
                                    color = if (selected) Color(primaryHex) else DeliColors.Border,
                                    shape = DeliShapes.control,
                                )
                                .clickable { bgPair = pair },
                            contentAlignment = Alignment.Center,
                        ) {
                            Box(Modifier.size(16.dp).clip(CircleShape).background(Color(pair.second)))
                        }
                    }
                }

                // Card style
                Text(t("Kart yapısı"), color = DeliColors.OnSurfaceVariant, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OptionChip(t("çerçeveli"), cardStyle == "OUTLINED") { cardStyle = "OUTLINED" }
                    OptionChip(t("gölgeli"), cardStyle == "ELEVATED") { cardStyle = "ELEVATED" }
                    OptionChip(t("düz"), cardStyle == "FLAT") { cardStyle = "FLAT" }
                }

                // Avatar & button style
                Text(t("Profil & Düğme"), color = DeliColors.OnSurfaceVariant, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OptionChip(t("yuvarlak"), avatarShape == "CIRCLE") { avatarShape = "CIRCLE" }
                    OptionChip(t("köşeli"), avatarShape == "SQUARE") { avatarShape = "SQUARE" }
                    OptionChip(t("dolu"), buttonStyle == "FILLED") { buttonStyle = "FILLED" }
                    OptionChip(t("yumuşak"), buttonStyle == "SOFT") { buttonStyle = "SOFT" }
                }

                // Corner radius & spacing
                Text(t("Köşe & Boşluk"), color = DeliColors.OnSurfaceVariant, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OptionChip("2 dp", radiusStep == 2) { radiusStep = 2 }
                    OptionChip("12 dp", radiusStep == 12) { radiusStep = 12 }
                    OptionChip("22 dp", radiusStep == 22) { radiusStep = 22 }
                    OptionChip(t("sık"), spacingStep == 10) { spacingStep = 10 }
                    OptionChip(t("ferah"), spacingStep == 16) { spacingStep = 16 }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(t("Ayırıcı çizgiler"), color = DeliColors.OnBackground, fontSize = 13.sp, modifier = Modifier.weight(1f))
                    Switch(
                        checked = dividers,
                        onCheckedChange = { dividers = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(primaryHex)),
                    )
                }

                KitPreview(previewSpec.toPalette())
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val id = "custom_" + System.currentTimeMillis()
                    onSave(previewSpec.copy(id = id, name = name.trim().ifBlank { "Özel Tema" }))
                }
            ) {
                Text(t("Kaydet"), color = DeliColors.Primary, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(t("Vazgeç"), color = DeliColors.OnSurfaceVariant)
            }
        },
    )
}

@Composable
private fun OptionChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(DeliShapes.small)
            .background(if (selected) DeliColors.Primary else DeliColors.SurfaceVariant)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Text(
            text = label,
            color = if (selected) Color.White else DeliColors.OnBackground,
            fontSize = 11.5.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

@Composable
private fun KitCard(
    kit: Palette,
    active: Boolean,
    onApply: () -> Unit,
    onDelete: (() -> Unit)? = null,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(DeliShapes.card)
            .background(DeliColors.Surface)
            .border(
                width = if (active) 1.5.dp else 1.dp,
                color = if (active) DeliColors.Primary else DeliColors.Border,
                shape = DeliShapes.card,
            )
            .padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(kit.controlRadius))
                    .background(kit.primary.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = kit.icon,
                    contentDescription = null,
                    tint = kit.primary,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = t(kit.name),
                        color = DeliColors.OnBackground,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (active) {
                        Spacer(Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(DeliShapes.pill)
                                .background(DeliColors.PrimaryContainer)
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                        ) {
                            Text(
                                text = t("Seçili"),
                                color = DeliColors.OnBackground,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (trait in kitTraits(kit)) {
                        Box(
                            modifier = Modifier
                                .clip(DeliShapes.pill)
                                .background(DeliColors.SurfaceVariant)
                                .padding(horizontal = 6.dp, vertical = 1.dp),
                        ) {
                            Text(t(trait), color = DeliColors.OnSurfaceVariant, fontSize = 9.5.sp)
                        }
                    }
                }
            }
            if (onDelete != null) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = t("Sil"),
                    tint = DeliColors.Danger,
                    modifier = Modifier
                        .size(20.dp)
                        .clickable { onDelete() },
                )
                Spacer(Modifier.width(6.dp))
            }
            if (!active) {
                TextButton(onClick = onApply) {
                    Text(t("Uygula"), color = DeliColors.Blue, fontSize = 13.sp)
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        KitPreview(kit)
    }
}

/**
 * A miniature of the app in one kit: its top bar, a post with its votes, a button and the
 * bottom bar — drawn from the palette and radii handed in rather than from the kit in
 * force, which is the whole point of a preview.
 */
@Composable
private fun KitPreview(kit: Palette) {
    val shape = RoundedCornerShape(kit.cardRadius)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(kit.background)
            .border(1.dp, kit.border, shape),
    ) {
        // Top bar.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(kit.surface)
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.Menu, contentDescription = null, tint = kit.onBackground, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(8.dp))
            Text("Hoset", color = kit.onBackground, fontSize = 13.sp, fontWeight = kit.titleWeight)
            Spacer(Modifier.weight(1f))
            Icon(Icons.Filled.Search, contentDescription = null, tint = kit.onSurfaceVariant, modifier = Modifier.size(14.dp))
        }
        if (kit.cardStyle == CardStyle.OUTLINED) {
            Box(Modifier.fillMaxWidth().height(1.dp).background(kit.border))
        }

        // The post itself, in the kit's own surface idiom.
        Column(modifier = Modifier.padding(kit.spacing * 0.6f)) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(previewSurface(kit, RoundedCornerShape(kit.cardRadius)))
                    .padding(kit.spacing * 0.7f),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PreviewAvatar(kit, size = 22.dp)
                    Spacer(Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("@hoset", color = kit.onBackground, fontSize = 11.sp, fontWeight = kit.titleWeight, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("general · 2 sa", color = kit.onSurfaceVariant, fontSize = 9.sp, maxLines = 1)
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(kit.smallRadius))
                            .background(kit.primaryContainer)
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    ) {
                        Text("12", color = if (kit.dark) Color.White else kit.onBackground, fontSize = 9.sp)
                    }
                }
                Spacer(Modifier.height(7.dp))
                Text(
                    text = t("Örnek gönderi"),
                    color = kit.onBackground,
                    fontSize = 13.sp,
                    fontWeight = kit.titleWeight,
                    letterSpacing = kit.letterSpacing,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = t("Tema önizlemesi"),
                    color = kit.onSurfaceVariant,
                    fontSize = 10.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.KeyboardArrowUp, contentDescription = null, tint = kit.upvote, modifier = Modifier.size(15.dp))
                    Text("12", color = kit.upvote, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 3.dp))
                    Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null, tint = kit.downvote, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(t("Yorumlar"), color = kit.onSurfaceVariant, fontSize = 10.sp)
                    Text(" 3", color = kit.onSurfaceVariant, fontSize = 10.sp)
                    Spacer(Modifier.weight(1f))
                    Box(
                        modifier = Modifier
                            .then(previewButton(kit))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    ) {
                        Text(t("Uygula"), color = previewButtonText(kit), fontSize = 9.5.sp, fontWeight = kit.titleWeight)
                    }
                }
            }

            Spacer(Modifier.height(kit.spacing * 0.5f))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(RoundedCornerShape(kit.smallRadius))
                        .background(kit.primary.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(kit.primary))
                }
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(t("Uygulama Teması"), color = kit.onBackground, fontSize = 10.5.sp, maxLines = 1)
                    Text(t(kit.name), color = kit.onSurfaceVariant, fontSize = 9.sp, maxLines = 1)
                }
                Text("›", color = kit.onSurfaceVariant, fontSize = 12.sp)
            }
        }

        if (kit.cardStyle == CardStyle.OUTLINED) {
            Box(Modifier.fillMaxWidth().height(1.dp).background(kit.border))
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(kit.surface)
                .padding(vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            PreviewTab(kit, Icons.Filled.Home, t("Feed"), active = true)
            PreviewTab(kit, Icons.Filled.Groups, t("Topluluklar"), active = false)
            PreviewTab(kit, Icons.Filled.Forum, t("Sohbet"), active = false)
            PreviewTab(kit, Icons.Filled.Person, t("Profil"), active = false)
        }
    }
}

@Composable
private fun PreviewTab(kit: Palette, icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, active: Boolean) {
    val tint = if (active) kit.primary else kit.onSurfaceVariant
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(14.dp))
        Spacer(Modifier.height(2.dp))
        Text(label, color = tint, fontSize = 8.5.sp)
    }
}

/** A card's chrome in the kit's idiom: filled, then outlined, lifted or left bare. */
private fun previewSurface(kit: Palette, shape: Shape): Modifier {
    var modifier: Modifier = Modifier
    if (kit.cardStyle == CardStyle.ELEVATED) {
        modifier = modifier.shadow(
            elevation = 8.dp,
            shape = shape,
            ambientColor = Color.Black.copy(alpha = 0.55f),
            spotColor = Color.Black.copy(alpha = 0.55f),
        )
    }
    modifier = modifier.clip(shape).background(kit.surface)
    if (kit.cardStyle == CardStyle.OUTLINED) modifier = modifier.border(1.dp, kit.border, shape)
    return modifier
}

private fun previewButton(kit: Palette): Modifier = when (kit.buttonStyle) {
    ButtonStyle.FILLED -> Modifier.clip(controlShape(kit)).background(kit.primary)
    ButtonStyle.SOFT -> Modifier.clip(controlShape(kit)).background(kit.primaryContainer)
    ButtonStyle.OUTLINED -> Modifier.clip(controlShape(kit)).border(1.dp, kit.primary, controlShape(kit))
}

private fun previewButtonText(kit: Palette): Color = when (kit.buttonStyle) {
    ButtonStyle.FILLED -> Color.White
    ButtonStyle.SOFT -> kit.onBackground
    ButtonStyle.OUTLINED -> kit.primary
}

/** The avatar the kit asks for: round, or a tile. */
@Composable
private fun PreviewAvatar(kit: Palette, size: Dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(avatarShape(kit))
            .background(kit.primary.copy(alpha = 0.35f))
            .border(1.dp, kit.primary.copy(alpha = 0.5f), avatarShape(kit)),
        contentAlignment = Alignment.Center,
    ) {
        Text("H", color = if (kit.dark) Color.White else kit.onBackground, fontSize = (size.value * 0.5f).sp, fontWeight = FontWeight.Bold)
    }
}
