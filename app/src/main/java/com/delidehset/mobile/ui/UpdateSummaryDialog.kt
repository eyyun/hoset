package com.delidehset.mobile.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
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
import com.delidehset.mobile.data.WhatsNew
import com.delidehset.mobile.ui.theme.DeliColors

/**
 * What changed, shown once after an update.
 *
 * The app updates itself, and until now it did so silently: a fix arrived and the only way
 * to notice was to go looking for it. This is one small window naming the few things that
 * changed, shown the first time this version runs.
 *
 * A fresh install records its version without showing anything — there is no previous
 * version on the phone for it to have changed from, and a changelog for a version someone
 * never ran is noise.
 */
@Composable
fun UpdateSummaryDialog(version: String) {
    val context = LocalContext.current
    var lines by remember(version) { mutableStateOf<List<String>>(emptyList()) }

    LaunchedEffect(version) {
        val previous = HosetPrefs.lastVersion(context)
        if (previous != null && previous != version) lines = WhatsNew.forVersion(version)
        HosetPrefs.setLastVersion(context, version)
    }

    if (lines.isEmpty()) return

    AlertDialog(
        onDismissRequest = { lines = emptyList() },
        containerColor = DeliColors.Surface,
        title = {
            Text(
                t("Güncellendi") + " · $version",
                color = DeliColors.OnBackground,
                fontWeight = FontWeight.SemiBold,
            )
        },
        text = {
            Column {
                for (line in lines) {
                    Text(
                        "•  " + t(line),
                        color = DeliColors.OnSurfaceVariant,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(vertical = 3.dp),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { lines = emptyList() }) {
                Text(t("Tamam"), color = DeliColors.Primary, fontWeight = FontWeight.Bold)
            }
        },
    )
}
