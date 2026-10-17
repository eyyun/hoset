package com.delidehset.mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.delidehset.mobile.ui.Text
import com.delidehset.mobile.ui.t
import com.delidehset.mobile.ui.tf
import com.delidehset.mobile.ui.theme.DeliColors
import com.delidehset.mobile.ui.theme.DeliPalettes
import com.delidehset.mobile.ui.theme.DeliShapes
import com.delidehset.mobile.ui.theme.Palette

/**
 * The theme store: every kit the app ships with, each one drawn as itself.
 *
 * A row of colour dots says almost nothing about a kit — two kits can share a palette and
 * still feel different, because the corners are half of it. So each entry carries a small
 * mock of the app rendered with *that* kit's own values, colours and radii both, and the
 * choice is made by looking at it rather than by reading a name.
 */
@Composable
fun ThemeStoreScreen(
    current: Palette,
    onApply: (Palette) -> Unit,
    contentPadding: PaddingValues,
) {
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
            Column {
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
                        DeliPalettes.all.size,
                    ),
                    color = DeliColors.OnSurfaceVariant,
                    fontSize = 12.5.sp,
                )
            }
        }

        items(DeliPalettes.all, key = { it.id }) { kit ->
            KitCard(
                kit = kit,
                active = kit.id == current.id,
                onApply = { onApply(kit) },
            )
        }
    }
}

@Composable
private fun KitCard(kit: Palette, active: Boolean, onApply: () -> Unit) {
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
            // The kit's own icon, in the kit's own colour: the first thing you see of it.
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(kit.controlRadius))
                    .background(kit.primary.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) {
                androidx.compose.material3.Icon(
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
                Spacer(Modifier.height(2.dp))
                Text(
                    text = tf("köşe {0} dp", kit.cardRadius.value.toInt()),
                    color = DeliColors.OnSurfaceVariant,
                    fontSize = 12.sp,
                )
            }
            if (!active) {
                TextButton(onClick = onApply) {
                    Text(t("Uygula"), color = DeliColors.Blue, fontSize = 13.sp)
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        KitPreview(kit)

        Spacer(Modifier.height(10.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Swatch(kit.background)
            Swatch(kit.surface)
            Swatch(kit.surfaceVariant)
            Swatch(kit.primary)
            Swatch(kit.blue)
            Swatch(kit.upvote)
            Swatch(kit.downvote)
            Swatch(kit.success)
            Swatch(kit.warning)
            Swatch(kit.danger)
            Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun Swatch(color: Color) {
    Box(
        modifier = Modifier
            .size(16.dp)
            .clip(CircleShape)
            .background(color)
            .border(1.dp, DeliColors.Border, CircleShape),
    )
}

/**
 * A miniature of the app in one kit: a bar, a post, a row of chips and the bottom bar.
 *
 * Drawn from the palette it is handed rather than from the colours in force, which is the
 * whole point — the preview has to show the kit, not the screen showing the kit.
 */
@Composable
private fun KitPreview(kit: Palette) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(170.dp)
            .clip(RoundedCornerShape(kit.cardRadius))
            .background(kit.background)
            .border(1.dp, kit.border, RoundedCornerShape(kit.cardRadius)),
    ) {
        // Top bar: the mark, a title, and the two actions every screen has.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(30.dp)
                .background(kit.surface)
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(14.dp).clip(CircleShape).background(kit.primary))
            Spacer(Modifier.width(7.dp))
            Bar(width = 46.dp, height = 6.dp, color = kit.onBackground, alpha = 0.85f)
            Spacer(Modifier.weight(1f))
            Box(Modifier.size(10.dp).clip(CircleShape).background(kit.surfaceVariant))
            Spacer(Modifier.width(7.dp))
            Box(Modifier.size(10.dp).clip(CircleShape).background(kit.surfaceVariant))
        }

        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp)) {
            // A post: who wrote it, what they wrote, and what you can do about it.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(kit.controlRadius))
                    .background(kit.surface)
                    .padding(9.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(16.dp).clip(CircleShape).background(kit.primary))
                    Spacer(Modifier.width(7.dp))
                    Column {
                        Bar(width = 62.dp, height = 6.dp, color = kit.onBackground, alpha = 0.9f)
                        Spacer(Modifier.height(4.dp))
                        Bar(width = 38.dp, height = 5.dp, color = kit.onSurfaceVariant, alpha = 0.9f)
                    }
                }
                Spacer(Modifier.height(9.dp))
                Bar(Modifier.fillMaxWidth(0.92f), 6.dp, kit.onBackground, 0.55f)
                Spacer(Modifier.height(5.dp))
                Bar(Modifier.fillMaxWidth(0.68f), 6.dp, kit.onBackground, 0.55f)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Chip(kit.upvote, kit.controlRadius)
                    Chip(kit.downvote, kit.controlRadius)
                    Chip(kit.primary, kit.controlRadius)
                }
            }

            Spacer(Modifier.height(9.dp))

            // The small chips a list screen is made of.
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                SoftChip(kit.surfaceVariant, kit.smallRadius)
                SoftChip(kit.surfaceVariant, kit.smallRadius)
                SoftChip(kit.primary.copy(alpha = 0.25f), kit.smallRadius)
            }
        }

        Spacer(Modifier.weight(1f))

        // The bottom bar, where the app's four tabs live.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(26.dp)
                .background(kit.surface)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Box(Modifier.size(11.dp).clip(CircleShape).background(kit.primary))
            Box(Modifier.size(11.dp).clip(CircleShape).background(kit.onSurfaceVariant.copy(alpha = 0.5f)))
            Box(Modifier.size(11.dp).clip(CircleShape).background(kit.onSurfaceVariant.copy(alpha = 0.5f)))
            Box(Modifier.size(11.dp).clip(CircleShape).background(kit.onSurfaceVariant.copy(alpha = 0.5f)))
        }
    }
}

@Composable
private fun Bar(
    width: Dp,
    height: Dp,
    color: Color,
    alpha: Float,
) = Box(
    modifier = Modifier
        .width(width)
        .height(height)
        .clip(RoundedCornerShape(50))
        .background(color.copy(alpha = alpha)),
)

@Composable
private fun Bar(modifier: Modifier, height: Dp, color: Color, alpha: Float) =
    Box(modifier = modifier.height(height).clip(RoundedCornerShape(50)).background(color.copy(alpha = alpha)))

@Composable
private fun Chip(color: Color, radius: Dp) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(radius))
            .background(color.copy(alpha = 0.22f))
            .padding(horizontal = 9.dp, vertical = 4.dp),
    ) {
        Bar(width = 16.dp, height = 5.dp, color = color, alpha = 1f)
    }
}

@Composable
private fun SoftChip(color: Color, radius: Dp) {
    Box(
        modifier = Modifier
            .width(38.dp)
            .height(14.dp)
            .clip(RoundedCornerShape(radius))
            .background(color),
    )
}
