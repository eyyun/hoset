package com.delidehset.mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.delidehset.mobile.data.HosetPrefs
import com.delidehset.mobile.ui.Text
import com.delidehset.mobile.ui.components.DeliCard
import com.delidehset.mobile.ui.components.LoadingBox
import com.delidehset.mobile.ui.components.MutedText
import com.delidehset.mobile.ui.deliContainer
import com.delidehset.mobile.ui.t
import com.delidehset.mobile.ui.theme.DeliColors

/**
 * The site's rules, drawn from the site's own `/rules` page.
 *
 * Reached from the sidebar's Kurallar entry, and drawn by the app itself rather than sent
 * to the web frame: it is a page of plain text, and the frame is no way to read one. The
 * text is cached on the device as soon as it is read, so opening it again — or opening it
 * offline — still has something to show.
 */
@Composable
fun RulesScreen(
    contentPadding: PaddingValues,
) {
    val context = LocalContext.current
    val repo = deliContainer().repository

    val cached = remember { HosetPrefs.rulesText(context) }
    var blocks by remember { mutableStateOf(cached?.split('\n')?.filter { it.isNotBlank() }.orEmpty()) }
    var loading by remember { mutableStateOf(blocks.isEmpty()) }

    LaunchedEffect(Unit) {
        val fresh = runCatching { repo.rules() }.getOrNull()
        if (fresh != null && fresh.blocks.isNotEmpty()) {
            blocks = fresh.blocks
            HosetPrefs.setRulesText(context, fresh.blocks.joinToString("\n"))
        }
        loading = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeliColors.Background)
            .verticalScroll(rememberScrollState())
            .padding(
                start = 16.dp,
                end = 16.dp,
                top = 12.dp,
                bottom = contentPadding.calculateBottomPadding() + 28.dp,
            ),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            t("Kurallar"),
            color = DeliColors.OnBackground,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
        )
        MutedText(t("Delidehşet'te geçerli kurallar."))

        if (loading) {
            LoadingBox(Modifier.fillMaxWidth())
        } else if (blocks.isEmpty()) {
            DeliCard {
                Text(
                    t("Kurallar şu an yüklenemedi. Menüden tekrar açabilirsin."),
                    color = DeliColors.OnSurfaceVariant,
                    fontSize = 14.sp,
                )
            }
        } else {
            DeliCard {
                blocks.forEachIndexed { index, line ->
                    val heading = line.endsWith(":")
                    Text(
                        text = line,
                        color = if (heading) DeliColors.OnBackground else DeliColors.OnSurfaceVariant,
                        fontSize = if (heading) 15.sp else 14.sp,
                        fontWeight = if (heading) FontWeight.SemiBold else FontWeight.Normal,
                    )
                    if (index != blocks.lastIndex) Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}
