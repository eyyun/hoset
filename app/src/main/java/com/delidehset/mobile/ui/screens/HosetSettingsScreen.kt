package com.delidehset.mobile.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AppShortcut
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.core.content.ContextCompat
import com.delidehset.mobile.BuildConfig
import com.delidehset.mobile.R
import com.delidehset.mobile.data.FeedSort
import com.delidehset.mobile.data.HosetPrefs
import com.delidehset.mobile.data.LauncherVariant
import com.delidehset.mobile.notify.HosetNotifications
import com.delidehset.mobile.ui.Text
import com.delidehset.mobile.ui.deliContainer
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
    val container = deliContainer()
    var enabled by remember { mutableStateOf(HosetNotifications.enabled(context)) }
    var permitted by remember { mutableStateOf(hasNotificationPermission(context)) }
    var appTitle by remember { mutableStateOf(HosetPrefs.appTitle(context)) }
    var showNameDialog by remember { mutableStateOf(false) }
    var launcher by remember { mutableStateOf(HosetPrefs.launcherVariant(context)) }
    var showIconDialog by remember { mutableStateOf(false) }
    var fontScale by remember { mutableFloatStateOf(HosetPrefs.fontScale(context)) }
    var showFontDialog by remember { mutableStateOf(false) }
    var defaultSort by remember { mutableStateOf(HosetPrefs.defaultSort(context)) }
    var showSortDialog by remember { mutableStateOf(false) }
    var hideNsfw by remember { mutableStateOf(HosetPrefs.hideNsfwMedia(context)) }
    var compactFeed by remember { mutableStateOf(HosetPrefs.compactFeed(context)) }
    var autoUpdate by remember { mutableStateOf(HosetPrefs.autoUpdate(context)) }
    var cacheCleared by remember { mutableStateOf(false) }

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
                Text(appTitle, color = DeliColors.OnBackground, fontSize = 17.sp, fontWeight = FontWeight.Bold)
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
                subtitle = tf("{0} kit içinden {1}", DeliPalettes.available.size, t(theme.name)),
                onClick = onOpenThemeStore,
            )

            HorizontalDivider(color = DeliColors.Border)

            HosetRow(
                icon = Icons.Filled.Edit,
                tint = DeliColors.Primary,
                title = t("Uygulama adı"),
                subtitle = appTitle,
                onClick = { showNameDialog = true },
            )

            HorizontalDivider(color = DeliColors.Border)

            HosetRow(
                icon = Icons.Filled.AppShortcut,
                tint = DeliColors.Blue,
                title = t("Simge ve kısayol"),
                subtitle = t(launcher.label) + " · " + t(launcher.subtitle),
                onClick = { showIconDialog = true },
            )

            HorizontalDivider(color = DeliColors.Border)

            HosetRow(
                icon = Icons.Filled.FormatSize,
                tint = DeliColors.Success,
                title = t("Yazı boyutu"),
                subtitle = "%${(fontScale * 100).toInt()}",
                onClick = { showFontDialog = true },
            )
        }

        SettingsGroupTitle(t("Akış & İçerik"))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(DeliShapes.card)
                .kitSurface(padding = 0.dp),
        ) {
            HosetRow(
                icon = Icons.Filled.Sort,
                tint = DeliColors.Primary,
                title = t("Varsayılan sıralama"),
                subtitle = defaultSort.label,
                onClick = { showSortDialog = true },
            )

            HorizontalDivider(color = DeliColors.Border)

            HosetToggleRow(
                icon = Icons.Filled.ViewAgenda,
                tint = DeliColors.Blue,
                title = t("Sıkışık akış görünümü"),
                subtitle = t("Önizleme metinlerini ve büyük görselleri gizler."),
                checked = compactFeed,
                onToggle = {
                    compactFeed = it
                    HosetPrefs.setCompactFeed(context, it)
                },
            )

            HorizontalDivider(color = DeliColors.Border)

            HosetToggleRow(
                icon = Icons.Filled.VisibilityOff,
                tint = DeliColors.Danger,
                title = t("NSFW medyayı gizle"),
                subtitle = t("NSFW etiketli gönderilerin görsellerini akışta göstermez."),
                checked = hideNsfw,
                onToggle = {
                    hideNsfw = it
                    HosetPrefs.setHideNsfwMedia(context, it)
                },
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

        SettingsGroupTitle(t("Depolama ve Güncelleme"))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(DeliShapes.card)
                .kitSurface(padding = 0.dp),
        ) {
            HosetToggleRow(
                icon = Icons.Filled.Refresh,
                tint = DeliColors.Success,
                title = t("Otomatik güncelleme denetimi"),
                subtitle = t("Açılışta günde bir kez yeni sürüm var mı bakar."),
                checked = autoUpdate,
                onToggle = {
                    autoUpdate = it
                    HosetPrefs.setAutoUpdate(context, it)
                },
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

            HorizontalDivider(color = DeliColors.Border)

            HosetRow(
                icon = Icons.Filled.DeleteSweep,
                tint = DeliColors.Warning,
                title = t("Önbelleği temizle"),
                subtitle = if (cacheCleared) t("Sayfa ve görsel önbelleği temizlendi.") else t("Kayıtlı sayfa ve görsel önbelleğini sıfırlar."),
                onClick = {
                    container.cache.clear()
                    container.forgetImages()
                    cacheCleared = true
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
                text = t("Bu uygulama delidehset.com'un resmi istemcisi değildir; sitenin herkese açık sayfalarını okuyan, gönüllü yazılmış bir istemcidir."),
                color = DeliColors.OnSurfaceVariant,
                fontSize = 12.5.sp,
                lineHeight = 18.sp,
            )
        }
    }

    if (showNameDialog) {
        var draft by remember { mutableStateOf(appTitle) }
        AlertDialog(
            onDismissRequest = { showNameDialog = false },
            containerColor = DeliColors.Surface,
            title = { Text(t("Uygulama adı"), color = DeliColors.OnBackground, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it.take(24) },
                    singleLine = true,
                    placeholder = { Text("Hoset", color = DeliColors.OnSurfaceVariant) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = DeliColors.Background,
                        unfocusedContainerColor = DeliColors.Background,
                        focusedTextColor = DeliColors.OnBackground,
                        unfocusedTextColor = DeliColors.OnBackground,
                    ),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        HosetPrefs.setAppTitle(context, draft)
                        appTitle = HosetPrefs.appTitle(context)
                        showNameDialog = false
                    }
                ) { Text(t("Kaydet"), color = DeliColors.Primary, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showNameDialog = false }) { Text(t("Vazgeç"), color = DeliColors.OnSurfaceVariant) }
            },
        )
    }

    if (showIconDialog) {
        AlertDialog(
            onDismissRequest = { showIconDialog = false },
            containerColor = DeliColors.Surface,
            title = { Text(t("Simge ve kısayol"), color = DeliColors.OnBackground, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (variant in LauncherVariant.entries) {
                        val active = variant == launcher
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(DeliShapes.control)
                                .background(if (active) DeliColors.PrimaryContainer else DeliColors.Background)
                                .clickable {
                                    launcher = variant
                                    HosetPrefs.setLauncherVariant(context, variant)
                                    showIconDialog = false
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(t(variant.label), color = DeliColors.OnBackground, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                Text(t(variant.subtitle), color = DeliColors.OnSurfaceVariant, fontSize = 12.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showIconDialog = false }) { Text(t("Kapat"), color = DeliColors.OnSurfaceVariant) }
            },
        )
    }

    if (showFontDialog) {
        val options = listOf(0.9f to "%90", 1.0f to "%100", 1.1f to "%110", 1.2f to "%120")
        AlertDialog(
            onDismissRequest = { showFontDialog = false },
            containerColor = DeliColors.Surface,
            title = { Text(t("Yazı boyutu"), color = DeliColors.OnBackground, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    for ((scale, label) in options) {
                        val active = kotlin.math.abs(fontScale - scale) < 0.03f
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(DeliShapes.control)
                                .background(if (active) DeliColors.PrimaryContainer else DeliColors.Background)
                                .clickable {
                                    fontScale = scale
                                    HosetPrefs.setFontScale(context, scale)
                                    showFontDialog = false
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(label, color = DeliColors.OnBackground, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showFontDialog = false }) { Text(t("Kapat"), color = DeliColors.OnSurfaceVariant) }
            },
        )
    }

    if (showSortDialog) {
        AlertDialog(
            onDismissRequest = { showSortDialog = false },
            containerColor = DeliColors.Surface,
            title = { Text(t("Varsayılan sıralama"), color = DeliColors.OnBackground, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (sort in FeedSort.entries) {
                        val active = sort == defaultSort
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(DeliShapes.control)
                                .background(if (active) DeliColors.PrimaryContainer else DeliColors.Background)
                                .clickable {
                                    defaultSort = sort
                                    HosetPrefs.setDefaultSort(context, sort)
                                    showSortDialog = false
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(sort.label, color = DeliColors.OnBackground, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSortDialog = false }) { Text(t("Kapat"), color = DeliColors.OnSurfaceVariant) }
            },
        )
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
