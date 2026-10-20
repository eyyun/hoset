package com.delidehset.mobile.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.delidehset.mobile.data.DeliHttp
import com.delidehset.mobile.data.HoleStore
import com.delidehset.mobile.ui.Text
import com.delidehset.mobile.ui.components.DeliButtonPrimary
import com.delidehset.mobile.ui.components.DeliCard
import com.delidehset.mobile.ui.components.DeliOutlinedButton
import com.delidehset.mobile.ui.components.ErrorBox
import com.delidehset.mobile.ui.components.LoadingBox
import com.delidehset.mobile.ui.components.MutedText
import com.delidehset.mobile.ui.t
import com.delidehset.mobile.ui.tr
import com.delidehset.mobile.ui.theme.DeliColors
import com.delidehset.mobile.ui.theme.DeliShapes
import com.delidehset.mobile.ui.theme.nameColor
import com.delidehset.mobile.vm.HoleSettingsViewModel
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

/**
 * A community's settings, drawn natively with all tabs:
 * General, Content filters, Membership, Privacy & visibility, Flairs & rules, Appearance, Lua, Mod reports.
 */
@Composable
fun HoleSettingsScreen(
    vm: HoleSettingsViewModel,
    hole: String,
    contentPadding: PaddingValues,
    onMessage: (String) -> Unit,
    onOpenWeb: (path: String, title: String) -> Unit = { _, _ -> },
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(hole) { vm.load(hole) }
    LaunchedEffect(state.message) {
        state.message?.let { onMessage(it); vm.consumeMessage() }
    }

    val settings = state.settings
    var selectedTab by remember { mutableIntStateOf(0) }

    // General form states
    var displayName by remember(settings?.hole, settings?.displayName) { mutableStateOf(settings?.displayName.orEmpty()) }
    var customDomain by remember(settings?.hole, settings?.customDomain) { mutableStateOf(settings?.customDomain.orEmpty()) }
    var titleEditWindow by remember(settings?.hole, settings?.titleEditWindow) { mutableStateOf(settings?.titleEditWindow.orEmpty()) }

    // Filters form states
    var blockedIps by remember(settings?.hole, settings?.blockedIps) { mutableStateOf(settings?.blockedIps.orEmpty()) }
    var blockedWords by remember(settings?.hole, settings?.blockedWords) { mutableStateOf(settings?.blockedWords.orEmpty()) }

    // Membership form states
    var joinType by remember(settings?.hole, settings?.joinType) { mutableStateOf(settings?.joinType.orEmpty().ifBlank { "public" }) }
    var allowCustomUserFlairs by remember(settings?.hole, settings?.allowCustomUserFlairs) { mutableStateOf(settings?.allowCustomUserFlairs ?: false) }
    var newUserFlairText by remember { mutableStateOf("") }
    var newUserFlairColor by remember { mutableStateOf("") }
    var exileUsername by remember { mutableStateOf("") }

    // Flairs & Rules form states
    var newPostFlairText by remember { mutableStateOf("") }
    var newPostFlairColor by remember { mutableStateOf("") }
    var newRuleText by remember { mutableStateOf("") }

    // Cropping dialog state
    var cropSourceBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val pickLogo = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                cropSourceBitmap = BitmapFactory.decodeStream(stream)
            }
        }
    }

    val pickBanner = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val file = readPickedFile(context, uri)
            if (file == null) onMessage(tr("Görsel okunamadı. Başka bir dosya seç."))
            else vm.uploadBanner(hole, file.first, file.second, file.third)
        }
    }

    val tabs = listOf(
        t("Genel"),
        t("İçerik filtreleri"),
        t("Üyelik"),
        t("Gizlilik ve görünürlük"),
        t("Flairlar ve kurallar"),
        t("Görünüm"),
        t("Lua scripting"),
        t("Mod raporları"),
    )

    Box(Modifier.fillMaxSize().background(DeliColors.Background)) {
        when {
            state.loading && settings == null -> LoadingBox(Modifier.fillMaxSize())
            settings == null -> ErrorBox(
                state.error ?: tr("Topluluk ayarları okunamadı."),
                Modifier.fillMaxSize(),
                onRetry = { vm.load(hole, force = true) },
            )
            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = contentPadding.calculateBottomPadding()),
            ) {
                // Header
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                ) {
                    Text(
                        "/h/$hole",
                        color = DeliColors.OnBackground,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    MutedText(t("Topluluk ayarları"))
                }

                // Tab strip
                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = DeliColors.Background,
                    contentColor = DeliColors.Primary,
                    edgePadding = 16.dp,
                    indicator = { tabPositions ->
                        if (selectedTab < tabPositions.size) {
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                color = DeliColors.Primary,
                            )
                        }
                    },
                    divider = {},
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = {
                                Text(
                                    text = title,
                                    fontSize = 13.sp,
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedTab == index) DeliColors.Primary else DeliColors.OnSurfaceVariant,
                                )
                            },
                        )
                    }
                }

                // Content pane
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    state.error?.let { ErrorBox(it) }

                    if (!settings.canEdit) {
                        DeliCard {
                            Text(
                                t("Bu topluluğun ayarlarını değiştirme yetkin yok."),
                                color = DeliColors.OnSurfaceVariant,
                                fontSize = 14.sp,
                            )
                            Spacer(Modifier.height(6.dp))
                            MutedText(t("Yalnızca topluluğu yönetenler ayarları değiştirebilir."))
                            Spacer(Modifier.height(12.dp))
                            DeliOutlinedButton(t("Sayfayı uygulamada aç")) {
                                onOpenWeb("/h/$hole/settings", "/h/$hole")
                            }
                        }
                        return@Column
                    }

                    when (selectedTab) {
                        0 -> {
                            // ---- 1. GENERAL ----
                            DeliCard {
                                Text(
                                    t("Topluluk görseli"),
                                    color = DeliColors.OnBackground,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Spacer(Modifier.height(10.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    HoleLogo(url = settings.logoUrl, name = hole, autoFetch = true)
                                    Spacer(Modifier.width(12.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(DeliShapes.control)
                                            .background(DeliColors.SurfaceVariant)
                                            .clickable(enabled = !state.uploading) {
                                                pickLogo.launch("image/*")
                                            }
                                            .padding(horizontal = 14.dp, vertical = 9.dp),
                                    ) {
                                        Text(
                                            if (state.uploading) t("Yükleniyor…") else t("Görsel yükle"),
                                            color = DeliColors.OnBackground,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium,
                                        )
                                    }
                                }
                            }

                            DeliCard {
                                Text(
                                    t("Görünen ad"),
                                    color = DeliColors.OnBackground,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Spacer(Modifier.height(4.dp))
                                MutedText(t("Boş bırakırsan topluluğun adı kullanılır."))
                                Spacer(Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = displayName,
                                    onValueChange = { if (it.length <= 50) displayName = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    placeholder = { Text("/h/$hole", color = DeliColors.OnSurfaceVariant) },
                                    shape = DeliShapes.control,
                                    colors = fieldColors(),
                                )
                            }

                            DeliCard {
                                Text(
                                    t("Özel alan adı"),
                                    color = DeliColors.OnBackground,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Spacer(Modifier.height(4.dp))
                                MutedText(t("Topluluğa yönlendirilen özel alan adı."))
                                Spacer(Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = customDomain,
                                    onValueChange = { customDomain = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    placeholder = { Text("ornek.com", color = DeliColors.OnSurfaceVariant) },
                                    shape = DeliShapes.control,
                                    colors = fieldColors(),
                                )
                            }

                            DeliCard {
                                Text(
                                    t("Başlık düzenleme süresi"),
                                    color = DeliColors.OnBackground,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Spacer(Modifier.height(4.dp))
                                MutedText(t("Saniye veya dakika cinsinden"))
                                Spacer(Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = titleEditWindow,
                                    onValueChange = { titleEditWindow = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    placeholder = { Text("0", color = DeliColors.OnSurfaceVariant) },
                                    shape = DeliShapes.control,
                                    colors = fieldColors(),
                                )
                            }

                            DeliButtonPrimary(
                                text = t(if (state.saving) "Kaydediliyor…" else "Genel ayarları kaydet"),
                                enabled = !state.saving,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                vm.saveGeneral(hole, displayName, customDomain, titleEditWindow)
                            }
                        }

                        1 -> {
                            // ---- 2. İÇERİK FİLTRELERİ ----
                            DeliCard {
                                Text(
                                    t("Engellenen IP aralıkları"),
                                    color = DeliColors.OnBackground,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Spacer(Modifier.height(6.dp))
                                OutlinedTextField(
                                    value = blockedIps,
                                    onValueChange = { blockedIps = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    minLines = 3,
                                    maxLines = 6,
                                    placeholder = { Text("192.0.2.5", color = DeliColors.OnSurfaceVariant) },
                                    shape = DeliShapes.control,
                                    colors = fieldColors(),
                                )
                                Spacer(Modifier.height(14.dp))
                                Text(
                                    t("Engellenen kelimeler"),
                                    color = DeliColors.OnBackground,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Spacer(Modifier.height(6.dp))
                                OutlinedTextField(
                                    value = blockedWords,
                                    onValueChange = { blockedWords = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    minLines = 3,
                                    maxLines = 8,
                                    placeholder = { Text(t("Her satıra bir kelime"), color = DeliColors.OnSurfaceVariant) },
                                    shape = DeliShapes.control,
                                    colors = fieldColors(),
                                )
                            }

                            DeliButtonPrimary(
                                text = t(if (state.saving) "Kaydediliyor…" else "Filtreleri kaydet"),
                                enabled = !state.saving,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                vm.saveFilters(hole, blockedIps, blockedWords)
                            }
                        }

                        2 -> {
                            // ---- 3. MEMBERSHIP ----
                            DeliCard {
                                Text(
                                    t("Katılım ayarları"),
                                    color = DeliColors.OnBackground,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Spacer(Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(DeliShapes.control)
                                            .background(if (joinType != "invite_only") DeliColors.Primary else DeliColors.SurfaceVariant)
                                            .clickable { joinType = "public" }
                                            .padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            t("Herkese açık"),
                                            color = if (joinType != "invite_only") Color.White else DeliColors.OnSurfaceVariant,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium,
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(DeliShapes.control)
                                            .background(if (joinType == "invite_only") DeliColors.Primary else DeliColors.SurfaceVariant)
                                            .clickable { joinType = "invite_only" }
                                            .padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            t("Yalnızca davetle"),
                                            color = if (joinType == "invite_only") Color.White else DeliColors.OnSurfaceVariant,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium,
                                        )
                                    }
                                }
                                Spacer(Modifier.height(14.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        t("Özel kullanıcı flairlerine izin ver"),
                                        color = DeliColors.OnBackground,
                                        fontSize = 14.sp,
                                        modifier = Modifier.weight(1f),
                                    )
                                    Switch(
                                        checked = allowCustomUserFlairs,
                                        onCheckedChange = { allowCustomUserFlairs = it },
                                        colors = SwitchDefaults.colors(checkedThumbColor = DeliColors.Primary),
                                    )
                                }
                                Spacer(Modifier.height(10.dp))
                                DeliButtonPrimary(
                                    text = t("Kaydet"),
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    vm.saveJoin(hole, joinType, allowCustomUserFlairs)
                                }
                            }

                            // User Flairs
                            DeliCard {
                                Text(
                                    t("Kullanıcı etiketleri"),
                                    color = DeliColors.OnBackground,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Spacer(Modifier.height(8.dp))
                                if (settings.userFlairs.isEmpty()) {
                                    MutedText(t("Henüz kullanıcı etiketi eklenmemiş."))
                                } else {
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        for (flair in settings.userFlairs) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(DeliColors.SurfaceVariant, DeliShapes.control)
                                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                            ) {
                                                Text(
                                                    flair.text,
                                                    color = DeliColors.OnBackground,
                                                    fontSize = 13.sp,
                                                    modifier = Modifier.weight(1f),
                                                )
                                                IconButton(
                                                    onClick = { vm.deleteUserFlair(hole, flair.text) },
                                                    modifier = Modifier.size(24.dp),
                                                ) {
                                                    Icon(
                                                        Icons.Default.Delete,
                                                        contentDescription = t("Sil"),
                                                        tint = DeliColors.Danger,
                                                        modifier = Modifier.size(16.dp),
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                                Spacer(Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    OutlinedTextField(
                                        value = newUserFlairText,
                                        onValueChange = { newUserFlairText = it },
                                        placeholder = { Text(t("Yeni kullanıcı etiketi"), color = DeliColors.OnSurfaceVariant) },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true,
                                        shape = DeliShapes.control,
                                        colors = fieldColors(),
                                    )
                                    Box(
                                        modifier = Modifier
                                            .clip(DeliShapes.control)
                                            .background(DeliColors.Primary)
                                            .clickable {
                                                if (newUserFlairText.isNotBlank()) {
                                                    vm.addUserFlair(hole, newUserFlairText.trim(), newUserFlairColor.takeIf { it.isNotBlank() })
                                                    newUserFlairText = ""
                                                    newUserFlairColor = ""
                                                }
                                            }
                                            .padding(horizontal = 14.dp, vertical = 12.dp),
                                    ) {
                                        Text(t("Ekle"), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                    }
                                }
                            }

                            // Pending join requests
                            DeliCard {
                                Text(
                                    t("Bekleyen katılım istekleri"),
                                    color = DeliColors.OnBackground,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Spacer(Modifier.height(8.dp))
                                if (settings.pendingRequests.isEmpty()) {
                                    MutedText(t("Bekleyen katılım isteği yok."))
                                } else {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        for (req in settings.pendingRequests) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(DeliColors.SurfaceVariant, DeliShapes.control)
                                                    .padding(8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                            ) {
                                                Text(
                                                    "@" + req.username,
                                                    color = DeliColors.OnBackground,
                                                    fontSize = 13.sp,
                                                    modifier = Modifier.weight(1f),
                                                )
                                                TextButton(onClick = { vm.approveJoin(hole, req.username) }) {
                                                    Text(t("Onayla"), color = DeliColors.Primary, fontSize = 12.sp)
                                                }
                                                TextButton(onClick = { vm.declineJoin(hole, req.username) }) {
                                                    Text(t("Reddet"), color = DeliColors.OnSurfaceVariant, fontSize = 12.sp)
                                                }
                                                TextButton(onClick = { vm.blockJoin(hole, req.username) }) {
                                                    Text(t("Engelle"), color = DeliColors.Danger, fontSize = 12.sp)
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Blocked members
                            DeliCard {
                                Text(
                                    t("Engellenen üyeler"),
                                    color = DeliColors.OnBackground,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Spacer(Modifier.height(8.dp))
                                if (settings.blockedMembers.isEmpty()) {
                                    MutedText(t("Engellenen üye yok."))
                                } else {
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        for (b in settings.blockedMembers) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(DeliColors.SurfaceVariant, DeliShapes.control)
                                                    .padding(8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                            ) {
                                                Text(
                                                    "@" + b.username,
                                                    color = DeliColors.OnBackground,
                                                    fontSize = 13.sp,
                                                    modifier = Modifier.weight(1f),
                                                )
                                                TextButton(onClick = { vm.unblockJoin(hole, b.username) }) {
                                                    Text(t("Engeli kaldır"), color = DeliColors.Primary, fontSize = 12.sp)
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Exile user
                            DeliCard {
                                Text(
                                    t("Kullanıcı sürgün et"),
                                    color = DeliColors.OnBackground,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Spacer(Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    OutlinedTextField(
                                        value = exileUsername,
                                        onValueChange = { exileUsername = it },
                                        placeholder = { Text(t("Kullanıcı adı"), color = DeliColors.OnSurfaceVariant) },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true,
                                        shape = DeliShapes.control,
                                        colors = fieldColors(),
                                    )
                                    Box(
                                        modifier = Modifier
                                            .clip(DeliShapes.control)
                                            .background(DeliColors.Danger)
                                            .clickable {
                                                if (exileUsername.isNotBlank()) {
                                                    vm.exileUser(hole, exileUsername.trim().removePrefix("@"))
                                                    exileUsername = ""
                                                }
                                            }
                                            .padding(horizontal = 14.dp, vertical = 12.dp),
                                    ) {
                                        Text(t("Sürgün et"), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                    }
                                }
                            }
                        }

                        3 -> {
                            // ---- 4. GİZLİLİK VE GÖRÜNÜRLÜK ----
                            DeliCard {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            t("Gizli mod"),
                                            color = DeliColors.OnBackground,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.SemiBold,
                                        )
                                        Spacer(Modifier.height(2.dp))
                                        MutedText(t("Topluluğu varsayılan olarak engelli yap"))
                                    }
                                    Switch(
                                        checked = settings.stealth,
                                        onCheckedChange = { vm.toggleStealth(hole) },
                                        colors = SwitchDefaults.colors(checkedThumbColor = DeliColors.Primary),
                                    )
                                }
                            }

                            DeliCard {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            t("Herkese açık kullanım"),
                                            color = DeliColors.OnBackground,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.SemiBold,
                                        )
                                        Spacer(Modifier.height(2.dp))
                                        MutedText(t("Bu topluluğu engellenemez ve kullanıcıları sürgün edilemez yap"))
                                    }
                                    Switch(
                                        checked = settings.publicUse,
                                        onCheckedChange = { vm.togglePublicUse(hole) },
                                        colors = SwitchDefaults.colors(checkedThumbColor = DeliColors.Primary),
                                    )
                                }
                            }

                            DeliCard {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            t("Moderatör listesini gizle"),
                                            color = DeliColors.OnBackground,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.SemiBold,
                                        )
                                        Spacer(Modifier.height(2.dp))
                                        MutedText(t("Moderatör listesini üyelerden gizle"))
                                    }
                                    Switch(
                                        checked = settings.hideModList,
                                        onCheckedChange = { vm.toggleModList(hole) },
                                        colors = SwitchDefaults.colors(checkedThumbColor = DeliColors.Primary),
                                    )
                                }
                            }

                            DeliCard {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            t("Anonim gönderi"),
                                            color = DeliColors.OnBackground,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.SemiBold,
                                        )
                                        Spacer(Modifier.height(2.dp))
                                        MutedText(t("Toplulukta anonim gönderi paylaşılmasına izin ver"))
                                    }
                                    Switch(
                                        checked = settings.anonPosting,
                                        onCheckedChange = { vm.toggleAnonPosting(hole) },
                                        colors = SwitchDefaults.colors(checkedThumbColor = DeliColors.Primary),
                                    )
                                }
                            }
                        }

                        4 -> {
                            // ---- 5. FLAIRLAR VE KURALLAR ----
                            DeliCard {
                                Text(
                                    t("Gönderi etiketleri"),
                                    color = DeliColors.OnBackground,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Spacer(Modifier.height(8.dp))
                                if (settings.flairs.isEmpty()) {
                                    MutedText(t("Henüz gönderi etiketi eklenmemiş."))
                                } else {
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        for (flair in settings.flairs) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(DeliColors.SurfaceVariant, DeliShapes.control)
                                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                            ) {
                                                Text(
                                                    flair.text,
                                                    color = DeliColors.OnBackground,
                                                    fontSize = 13.sp,
                                                    modifier = Modifier.weight(1f),
                                                )
                                                IconButton(
                                                    onClick = { vm.deleteFlair(hole, flair.text) },
                                                    modifier = Modifier.size(24.dp),
                                                ) {
                                                    Icon(
                                                        Icons.Default.Delete,
                                                        contentDescription = t("Sil"),
                                                        tint = DeliColors.Danger,
                                                        modifier = Modifier.size(16.dp),
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                                Spacer(Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    OutlinedTextField(
                                        value = newPostFlairText,
                                        onValueChange = { newPostFlairText = it },
                                        placeholder = { Text(t("Yeni gönderi etiketi"), color = DeliColors.OnSurfaceVariant) },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true,
                                        shape = DeliShapes.control,
                                        colors = fieldColors(),
                                    )
                                    Box(
                                        modifier = Modifier
                                            .clip(DeliShapes.control)
                                            .background(DeliColors.Primary)
                                            .clickable {
                                                if (newPostFlairText.isNotBlank()) {
                                                    vm.addFlair(hole, newPostFlairText.trim(), newPostFlairColor.takeIf { it.isNotBlank() })
                                                    newPostFlairText = ""
                                                    newPostFlairColor = ""
                                                }
                                            }
                                            .padding(horizontal = 14.dp, vertical = 12.dp),
                                    ) {
                                        Text(t("Ekle"), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                    }
                                }
                            }

                            DeliCard {
                                Text(
                                    t("Topluluk kuralları"),
                                    color = DeliColors.OnBackground,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Spacer(Modifier.height(8.dp))
                                if (settings.rules.isEmpty()) {
                                    MutedText(t("Henüz kural tanımlanmamış."))
                                } else {
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        settings.rules.forEachIndexed { idx, rule ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(DeliColors.SurfaceVariant, DeliShapes.control)
                                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                            ) {
                                                Text(
                                                    "${idx + 1}. ${rule.text}",
                                                    color = DeliColors.OnBackground,
                                                    fontSize = 13.sp,
                                                    modifier = Modifier.weight(1f),
                                                )
                                                IconButton(
                                                    onClick = { vm.deleteRule(hole, rule.text) },
                                                    modifier = Modifier.size(24.dp),
                                                ) {
                                                    Icon(
                                                        Icons.Default.Delete,
                                                        contentDescription = t("Sil"),
                                                        tint = DeliColors.Danger,
                                                        modifier = Modifier.size(16.dp),
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                                Spacer(Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    OutlinedTextField(
                                        value = newRuleText,
                                        onValueChange = { newRuleText = it },
                                        placeholder = { Text(t("Yeni kural"), color = DeliColors.OnSurfaceVariant) },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true,
                                        shape = DeliShapes.control,
                                        colors = fieldColors(),
                                    )
                                    Box(
                                        modifier = Modifier
                                            .clip(DeliShapes.control)
                                            .background(DeliColors.Primary)
                                            .clickable {
                                                if (newRuleText.isNotBlank()) {
                                                    vm.addRule(hole, newRuleText.trim())
                                                    newRuleText = ""
                                                }
                                            }
                                            .padding(horizontal = 14.dp, vertical = 12.dp),
                                    ) {
                                        Text(t("Ekle"), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                    }
                                }
                            }
                        }

                        5 -> {
                            // ---- 6. GÖRÜNÜM ----
                            DeliCard {
                                Text(
                                    t("Topluluk afişleri"),
                                    color = DeliColors.OnBackground,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Spacer(Modifier.height(8.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(DeliShapes.control)
                                        .background(DeliColors.SurfaceVariant)
                                        .clickable { pickBanner.launch("image/*") }
                                        .padding(horizontal = 14.dp, vertical = 9.dp),
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Upload, contentDescription = null, tint = DeliColors.Primary, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text(t("Afiş yükle"), color = DeliColors.Primary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                    }
                                }
                                Spacer(Modifier.height(12.dp))
                                if (settings.banners.isEmpty()) {
                                    MutedText(t("Henüz afiş yüklenmemiş."))
                                } else {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        for (banner in settings.banners) {
                                            AsyncImage(
                                                model = if (banner.startsWith("http")) banner else DeliHttp.BASE_URL + banner,
                                                contentDescription = null,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(90.dp)
                                                    .clip(DeliShapes.control),
                                                contentScale = ContentScale.Crop,
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        6 -> {
                            // ---- 7. LUA SCRIPTING ----
                            DeliCard {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        "Lua scripting",
                                        color = DeliColors.OnBackground,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(DeliShapes.small)
                                            .background(DeliColors.Primary.copy(alpha = 0.2f))
                                            .padding(horizontal = 8.dp, vertical = 2.dp),
                                    ) {
                                        Text(t("Yakında"), color = DeliColors.Primary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                                Spacer(Modifier.height(10.dp))
                                Text(
                                    t("Lua betik desteği ile topluluğunuzun işlevselliğini özelleştirebilecek, özel komutlar oluşturabilecek ve otomatik moderasyon kuralları tanımlayabileceksiniz."),
                                    color = DeliColors.OnSurfaceVariant,
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp,
                                )
                            }
                        }

                        7 -> {
                            // ---- 8. MOD REPORTS ----
                            DeliCard {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Flag, contentDescription = null, tint = DeliColors.Primary, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        t("Mod raporları"),
                                        color = DeliColors.OnBackground,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }
                                Spacer(Modifier.height(10.dp))
                                if (settings.reports.isEmpty()) {
                                    MutedText(t("Henüz bekleyen mod raporu bulunmuyor."))
                                } else {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        for (r in settings.reports) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(DeliColors.SurfaceVariant, DeliShapes.control)
                                                    .padding(10.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    if (r.content.isNotBlank()) {
                                                        Text(
                                                            r.content,
                                                            color = DeliColors.OnBackground,
                                                            fontSize = 13.sp,
                                                            fontWeight = FontWeight.Medium,
                                                        )
                                                    }
                                                    if (r.reason.isNotBlank()) {
                                                        Text(
                                                            t("Neden:") + " " + r.reason,
                                                            color = DeliColors.Danger,
                                                            fontSize = 12.sp,
                                                        )
                                                    }
                                                    if (r.reporter.isNotBlank()) {
                                                        MutedText(t("Bildiren:") + " @" + r.reporter)
                                                    }
                                                }
                                                IconButton(onClick = { vm.deleteReport(hole, r.id) }) {
                                                    Icon(Icons.Default.Close, contentDescription = t("Raporu sil"), tint = DeliColors.OnSurfaceVariant)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    com.delidehset.mobile.ui.components.DeliOutlinedButton(
                        text = t("Sayfayı uygulamada aç"),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        onOpenWeb("/h/$hole/settings", "/h/$hole")
                    }
                }
            }
        }

        // Cropping dialog when a logo image is picked
        cropSourceBitmap?.let { src ->
            IconCropDialog(
                bitmap = src,
                title = t("Topluluk görselini kırp"),
                onDismiss = { cropSourceBitmap = null },
                onCropped = { cropped ->
                    cropSourceBitmap = null
                    val stream = ByteArrayOutputStream()
                    cropped.compress(Bitmap.CompressFormat.JPEG, 92, stream)
                    val bytes = stream.toByteArray()
                    vm.uploadLogo(hole, "hole_logo.jpg", "image/jpeg", bytes)
                },
            )
        }

        if (state.saving || state.uploading) {
            CircularProgressIndicator(
                color = DeliColors.Primary,
                modifier = Modifier.align(Alignment.TopEnd).padding(18.dp).size(20.dp),
            )
        }
    }
}

@Composable
private fun fieldColors() = TextFieldDefaults.colors(
    focusedContainerColor = DeliColors.Surface,
    unfocusedContainerColor = DeliColors.Surface,
    focusedTextColor = DeliColors.OnBackground,
    unfocusedTextColor = DeliColors.OnBackground,
    cursorColor = DeliColors.Primary,
    focusedIndicatorColor = DeliColors.Primary,
    unfocusedIndicatorColor = DeliColors.Border,
)

/** The community's own picture, or its initial when the site has none. */
@Composable
internal fun HoleLogo(
    url: String?,
    name: String,
    size: androidx.compose.ui.unit.Dp = 52.dp,
    autoFetch: Boolean = false,
) {
    val context = LocalContext.current
    val given = url?.takeIf { !HoleStore.isGenericLogo(it) }
    var resolved by remember(name, given) {
        mutableStateOf(given ?: HoleStore.cachedLogo(name))
    }
    LaunchedEffect(name, given, autoFetch) {
        if (given != null) {
            HoleStore.put(name, logo = given)
            resolved = given
            return@LaunchedEffect
        }
        val cached = HoleStore.cachedLogo(name)
        if (cached != null) {
            resolved = cached
            return@LaunchedEffect
        }
        if (!autoFetch) return@LaunchedEffect
        val app = context.applicationContext as? com.delidehset.mobile.DeliApp ?: return@LaunchedEffect
        resolved = HoleStore.remember(app.container.repository, name)
    }

    val model = resolved?.takeIf { it.isNotBlank() }?.let {
        if (it.startsWith("http")) it else DeliHttp.BASE_URL + it
    }
    if (model != null) {
        AsyncImage(
            model = model,
            contentDescription = null,
            modifier = Modifier.size(size).clip(CircleShape),
            contentScale = ContentScale.Crop,
        )
    } else {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(nameColor(name).copy(alpha = 0.22f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                com.delidehset.mobile.ui.components.initialOf(name).orEmpty(),
                color = nameColor(name),
                fontSize = (size.value * 0.4f).sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}
