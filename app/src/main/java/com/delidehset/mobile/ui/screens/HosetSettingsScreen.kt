package com.delidehset.mobile.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.compose.material.icons.filled.NotificationsActive
import com.delidehset.mobile.notify.HosetNotifications
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.delidehset.mobile.BuildConfig
import com.delidehset.mobile.R
import com.delidehset.mobile.ui.Text
import com.delidehset.mobile.ui.t
import com.delidehset.mobile.ui.tf
import com.delidehset.mobile.ui.theme.DeliColors
import com.delidehset.mobile.ui.theme.DeliPalettes
import com.delidehset.mobile.ui.theme.DeliShapes
import com.delidehset.mobile.ui.theme.Palette
import com.delidehset.mobile.ui.theme.kitSurface
import com.delidehset.mobile.vm.UpdateState

/**
 * Hoset's own settings: the things that belong to this app rather than to the account.
 *
 * The account settings next door change what the site stores (bio, password, language);
 * this page changes what the install is — its look, whether it checks for a new version,
 * and what it is and is not (an unofficial client, MIT licensed, source on GitHub).
 */
@Composable
fun HosetSettingsScreen(
    theme: Palette,
    updateState: UpdateState,
    contentPadding: PaddingValues,
    onOpenThemeStore: () -> Unit,
    onCheckUpdates: () -> Unit,
) {
    val context = LocalContext.current
    var enabled by remember { mutableStateOf(HosetNotifications.enabled(context)) }
    var permitted by remember { mutableStateOf(hasNotificationPermission(context)) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        permitted = granted
        if (granted) {
            HosetNotifications.setEnabled(context, true)
            enabled = true
        }
    }
    val open = { url: String ->
        runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeliColors.Background)
            .verticalScroll(rememberScrollState())
            .padding(
                start = 14.dp,
                end = 14.dp,
                top = 12.dp,
                bottom = contentPadding.calculateBottomPadding() + 24.dp,
            ),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // The app's own mark and version, the way an about screen opens.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .kitSurface()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(R.drawable.logo_hoset),
                contentDescription = null,
                modifier = Modifier.size(44.dp),
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text("Hoset", color = DeliColors.OnBackground, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(2.dp))
                Text(
                    text = tf("Sürüm {0} (yapı {1})", BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE),
                    color = DeliColors.OnSurfaceVariant,
                    fontSize = 12.sp,
                )
            }
        }

        SettingsGroupTitle(t("Uygulama"))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(DeliShapes.card)
                .kitSurface(padding = 0.dp),
        ) {
            HosetRow(
                icon = Icons.Filled.Palette,
                tint = theme.primary,
                title = t("Tema Mağazası"),
                subtitle = tf("{0} kit içinden {1}", DeliPalettes.all.size, t(theme.name)),
                onClick = onOpenThemeStore,
            )

            HorizontalDivider(color = DeliColors.Border)

            HosetRow(
                icon = Icons.Filled.Refresh,
                tint = DeliColors.Blue,
                title = t("Güncellemeyi denetle"),
                subtitle = when (val state = updateState) {
                    is UpdateState.Downloading -> tf("İndiriliyor… %{0}", (state.progress * 100).toInt())
                    is UpdateState.Available -> tf("Yeni sürüm {0} hazır.", state.info.version)
                    is UpdateState.UpToDate -> t("En son sürümü kullanıyorsun.")
                    is UpdateState.Checking -> t("Denetleniyor…")
                    is UpdateState.Ready -> t("Kurulum açıldı")
                    is UpdateState.Failed -> state.message
                    UpdateState.Idle -> t("Güncellemeler GitHub sürümlerinden gelir.")
                },
                onClick = onCheckUpdates,
            )
        }

        SettingsGroupTitle(t("Bildirimler"))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(DeliShapes.card)
                .kitSurface(padding = 0.dp),
        ) {
            HosetToggleRow(
                icon = Icons.Filled.NotificationsActive,
                tint = DeliColors.Warning,
                title = t("Yeni bildirimler"),
                subtitle = when {
                    !enabled -> t("Kapalı — hiçbir bildirim gösterilmez.")
                    !permitted -> t("İzin yok. Açmak için dokun.")
                    else -> t("Açık — yanıtlar ve bahsetmeler bildirim olarak gelir.")
                },
                checked = enabled && permitted,
                onToggle = { want ->
                    if (want && !permitted) permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    else {
                        HosetNotifications.setEnabled(context, want)
                        enabled = want
                    }
                },
            )
        }

        SettingsGroupTitle(t("Hakkında"))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(DeliShapes.card)
                .kitSurface(padding = 0.dp),
        ) {
            HosetRow(
                icon = Icons.Filled.Public,
                tint = DeliColors.Success,
                title = t("Delidehşet"),
                subtitle = "delidehset.com",
                onClick = { open("https://delidehset.com") },
            )

            HorizontalDivider(color = DeliColors.Border)

            HosetRow(
                icon = Icons.Filled.Code,
                tint = DeliColors.OnSurfaceVariant,
                title = t("Kaynak kodu"),
                subtitle = "github.com/eyyun/hoset",
                onClick = { open("https://github.com/eyyun/hoset") },
            )

            HorizontalDivider(color = DeliColors.Border)

            HosetRow(
                icon = Icons.Filled.History,
                tint = DeliColors.OnSurfaceVariant,
                title = t("Sürüm notları"),
                subtitle = "github.com/eyyun/hoset/releases",
                onClick = { open("https://github.com/eyyun/hoset/releases") },
            )

            HorizontalDivider(color = DeliColors.Border)

            HosetRow(
                icon = Icons.Filled.Shield,
                tint = DeliColors.Warning,
                title = t("MIT lisansı"),
                subtitle = t("Açık kaynak, garanti yok."),
                onClick = { open("https://github.com/eyyun/hoset/blob/main/LICENSE") },
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth().kitSurface().padding(14.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                Icons.Filled.Info,
                contentDescription = null,
                tint = DeliColors.OnSurfaceVariant,
                modifier = Modifier.size(17.dp),
            )
            Spacer(Modifier.width(10.dp))
            Text(
                // One literal, not two: the dictionary is keyed by the whole line, and a
                // sentence split across `+` is two lines it cannot find.
                text = t("Bu uygulama delidehset.com'un resmi istemcisi değildir; sitenin herkese açık sayfalarını okuyan, gönüllü yazılmış bir istemcidir."),
                color = DeliColors.OnSurfaceVariant,
                fontSize = 12.5.sp,
                lineHeight = 18.sp,
            )
        }
    }
}

/** A row with a switch: the on/off kind of setting, as opposed to one that opens a page. */
@Composable
private fun HosetToggleRow(
    icon: ImageVector,
    tint: Color,
    title: String,
    subtitle: String,
    checked: Boolean,
    onToggle: (Boolean) -> Unit,
    enabled: Boolean = true,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onToggle(!checked) }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(DeliShapes.control)
                .background(tint.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(19.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = DeliColors.OnBackground, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(subtitle, color = DeliColors.OnSurfaceVariant, fontSize = 12.sp, maxLines = 2)
        }
        Spacer(Modifier.width(10.dp))
        Switch(
            checked = checked,
            onCheckedChange = onToggle,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = tint,
            ),
        )
    }
}

/** Android 13 and up gate every notification behind a runtime permission. */
private fun hasNotificationPermission(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED

@Composable
private fun SettingsGroupTitle(text: String) {
    Text(
        text = text.uppercase(),
        color = DeliColors.OnSurfaceVariant,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = DeliColors.palette.letterSpacing,
        modifier = Modifier.padding(start = 4.dp, top = 2.dp),
    )
}

@Composable
private fun HosetRow(
    icon: ImageVector,
    tint: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(DeliShapes.control)
                .background(tint.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(19.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = DeliColors.OnBackground, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(subtitle, color = DeliColors.OnSurfaceVariant, fontSize = 12.sp, maxLines = 2)
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = DeliColors.OnSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
    }
}
