package com.delidehset.mobile.ui.screens

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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

/**
 * A community's settings, drawn natively.
 *
 * The site puts seven tabs behind `/h/{hole}/settings`; the app offers the three an app
 * can actually do well — the display name, the picture and the content filters — and
 * leaves the rest on the site. The whole screen is gated on what the page itself says:
 * an account that cannot change the community sees that, not a form that fails on save.
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
    var displayName by remember(settings?.hole) { mutableStateOf(settings?.displayName.orEmpty()) }
    var blockedIps by remember(settings?.hole) { mutableStateOf(settings?.blockedIps.orEmpty()) }
    var blockedWords by remember(settings?.hole) { mutableStateOf(settings?.blockedWords.orEmpty()) }

    val pickLogo = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val file = readPickedFile(context, uri)
            if (file == null) onMessage(tr("Görsel okunamadı. Başka bir dosya seç."))
            else vm.uploadLogo(hole, file.first, file.second, file.third)
        }
    }

    Box(Modifier.fillMaxSize().background(DeliColors.Background)) {
        when {
            state.loading && settings == null -> LoadingBox(Modifier.fillMaxSize())
            settings == null -> ErrorBox(
                state.error ?: "Topluluk ayarları okunamadı.",
                Modifier.fillMaxSize(),
                onRetry = { vm.load(hole, force = true) },
            )
            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(
                        start = 16.dp,
                        end = 16.dp,
                        top = 12.dp,
                        bottom = contentPadding.calculateBottomPadding() + 32.dp,
                    ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    "/h/$hole",
                    color = DeliColors.OnBackground,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                )
                MutedText(t("Topluluk ayarları"))

                state.error?.let { ErrorBox(it) }

                if (!settings.canEdit) {
                    DeliCard {
                        Text(
                            "Bu topluluğun ayarlarını değiştirme yetkin yok.",
                            color = DeliColors.OnSurfaceVariant,
                            fontSize = 14.sp,
                        )
                        Spacer(Modifier.height(6.dp))
                        MutedText(t("Yalnızca topluluğu yönetenler ayarları değiştirebilir."))
                        Spacer(Modifier.height(12.dp))
                        com.delidehset.mobile.ui.components.DeliOutlinedButton(t("Sayfayı uygulamada aç")) {
                            onOpenWeb("/h/$hole/settings", "/h/$hole")
                        }
                    }
                    return@Column
                }

                // ---- picture ----
                DeliCard {
                    Text(
                        t("Topluluk görseli"),
                        color = DeliColors.OnBackground,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        HoleLogo(url = settings.logoUrl, name = hole)
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
                                if (state.uploading) "Yükleniyor…" else t("Görsel yükle"),
                                color = DeliColors.OnBackground,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                }

                // ---- display name ----
                DeliCard {
                    Text(
                        t("Görünen ad"),
                        color = DeliColors.OnBackground,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(4.dp))
                    MutedText(t("Boş bırakırsan topluluğun adı kullanılır."))
                    Spacer(Modifier.height(10.dp))
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

                // ---- content filters ----
                DeliCard {
                    Text(
                        t("İçerik filtreleri"),
                        color = DeliColors.OnBackground,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        t("Engellenen IP aralıkları"),
                        color = DeliColors.OnSurfaceVariant,
                        fontSize = 13.sp,
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
                    Spacer(Modifier.height(10.dp))
                    Text(
                        t("Engellenen kelimeler"),
                        color = DeliColors.OnSurfaceVariant,
                        fontSize = 13.sp,
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
                    text = t(if (state.saving) "Kaydediliyor…" else "Kaydet"),
                    enabled = !state.saving,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    vm.save(hole, displayName, blockedIps, blockedWords)
                }

                com.delidehset.mobile.ui.components.DeliOutlinedButton(
                    text = t("Sayfayı uygulamada aç"),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    onOpenWeb("/h/$hole/settings", "/h/$hole")
                }
            }
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
internal fun HoleLogo(url: String?, name: String, size: androidx.compose.ui.unit.Dp = 52.dp) {
    val context = LocalContext.current
    val given = url?.takeIf { !HoleStore.isGenericLogo(it) }
    var resolved by remember(name, given) {
        mutableStateOf(given ?: HoleStore.cachedLogo(name))
    }
    LaunchedEffect(name, given) {
        if (given != null) {
            HoleStore.put(name, logo = given)
            resolved = given
            return@LaunchedEffect
        }
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
