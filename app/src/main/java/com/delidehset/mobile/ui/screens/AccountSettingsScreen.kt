package com.delidehset.mobile.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import com.delidehset.mobile.ui.Text
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.delidehset.mobile.ui.components.DeliButtonPrimary
import com.delidehset.mobile.ui.components.DeliOutlinedButton
import com.delidehset.mobile.ui.components.ErrorBox
import com.delidehset.mobile.ui.components.FieldLabel
import com.delidehset.mobile.ui.components.LoadingBox
import com.delidehset.mobile.ui.components.MutedText
import com.delidehset.mobile.ui.theme.DeliColors
import com.delidehset.mobile.vm.AccountSettingsViewModel
import com.delidehset.mobile.vm.SettingsTab
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.delidehset.mobile.ui.theme.DeliShapes

/**
 * The signed-in account settings, drawn natively.
 *
 * delidehset.com keeps them on two tabs — Personal (`/settings`) and Security
 * (`/settings/security`) — and posts every change with JavaScript. This screen mirrors
 * both tabs with Material inputs and calls the same endpoints the site's JavaScript
 * calls, so nothing is handed off to a browser.
 */
@Composable
fun AccountSettingsScreen(
    vm: AccountSettingsViewModel,
    initialTab: String,
    contentPadding: PaddingValues,
    onMessage: (String) -> Unit,
    /** Called once a new picture was accepted, so the app can pick it up. */
    onProfileChanged: () -> Unit = {},
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(initialTab) {
        vm.setTab(SettingsTab.of(initialTab))
        vm.load()
    }

    LaunchedEffect(state.message) {
        state.message?.let { onMessage(it); vm.consume() }
    }

    LaunchedEffect(state.profileChanged) {
        if (state.profileChanged) {
            onProfileChanged()
            vm.consumeProfileChange()
        }
    }

    when {
        state.loading -> LoadingBox(Modifier.fillMaxSize())

        state.error != null && state.personal.languageOptions.isEmpty() ->
            ErrorBox(state.error!!, Modifier.fillMaxSize(), onRetry = { vm.load(force = true) })

        else -> Column(
            modifier = Modifier
                .fillMaxSize()
                .background(DeliColors.Background)
                .verticalScroll(rememberScrollState())
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    top = 12.dp,
                    bottom = contentPadding.calculateBottomPadding() + 32.dp,
                ),
        ) {
            SettingsTabs(state.tab, onSelect = { vm.setTab(it) })

            state.error?.let {
                Spacer(Modifier.height(10.dp))
                InlineError(it)
            }

            Spacer(Modifier.height(14.dp))

            when (state.tab) {
                SettingsTab.PERSONAL -> PersonalTab(
                    vm = vm,
                    state = state,
                    context = context,
                    onPick = { kind, uri ->
                        scope.launch {
                            android.util.Log.d("DeliUpload", "onPick kind=$kind uri=$uri")
                            // Picking a file and then seeing nothing at all happen was the
                            // worst part of this screen: an unreadable file says so now.
                            val file = readPickedFile(context, uri)
                            if (file == null) vm.report("Görsel okunamadı. Başka bir dosya seç.")
                            else vm.uploadImage(kind, file.first, file.second, file.third)
                        }
                    },
                )

                SettingsTab.SECURITY -> SecurityTab(vm = vm, state = state)
            }
        }
    }
}

/* ------------------------------------------------------------------ */
/*  Tabs                                                               */
/* ------------------------------------------------------------------ */

@Composable
private fun SettingsTabs(current: SettingsTab, onSelect: (SettingsTab) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DeliColors.Surface, DeliShapes.control)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        TabButton("Kişisel", current == SettingsTab.PERSONAL, Modifier.weight(1f)) {
            onSelect(SettingsTab.PERSONAL)
        }
        TabButton("Güvenlik", current == SettingsTab.SECURITY, Modifier.weight(1f)) {
            onSelect(SettingsTab.SECURITY)
        }
    }
}

@Composable
private fun TabButton(text: String, active: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .clip(DeliShapes.small)
            .background(if (active) DeliColors.PrimaryContainer else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 9.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            color = if (active) DeliColors.OnBackground else DeliColors.OnSurfaceVariant,
            fontSize = 13.sp,
            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

/* ------------------------------------------------------------------ */
/*  Personal tab                                                       */
/* ------------------------------------------------------------------ */

@Composable
private fun PersonalTab(
    vm: AccountSettingsViewModel,
    state: com.delidehset.mobile.vm.AccountSettingsUiState,
    context: Context,
    onPick: (kind: String, uri: Uri) -> Unit,
) {
    var pendingKind by remember { mutableStateOf("profile") }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) onPick(pendingKind, uri)
    }

    SectionCard(
        title = "Profil fotoğrafı",
        hint = "Görsel seçtiğin an yüklenir. En fazla 8 MB.",
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            DeliOutlinedButton("Fotoğraf seç") {
                pendingKind = "profile"
                picker.launch("image/*")
            }
            DeliOutlinedButton("Banner seç") {
                pendingKind = "banner"
                picker.launch("image/*")
            }
        }
        Spacer(Modifier.height(6.dp))
        MutedText("Yüklemeler siteye doğrudan gider; düz tarayıcı açılmaz.")
    }

    SectionCard(title = "Hakkında (bio)", hint = "Profilinde görünür. En fazla 500 karakter.") {
        DeliField(
            value = state.bio,
            onValueChange = { if (it.length <= 500) vm.setBio(it) },
            singleLine = false,
            minLines = 4,
            maxLines = 8,
        )
        Spacer(Modifier.height(4.dp))
        MutedText("${state.bio.length}/500")
    }

    SectionCard(title = "Zamirler", hint = "En fazla 15 karakter, örn. o/onun.") {
        DeliField(
            value = state.pronouns,
            onValueChange = { if (it.length <= 15) vm.setPronouns(it) },
            modifier = Modifier.width(200.dp),
        )
    }

    // The language is not here: the app and the site are one setting, and it is picked once,
    // in Ayarlar (see SettingsScreen). Two pickers that did the same thing in two places was
    // the thing that made this page feel like a form to fill in.

    SectionCard(title = "Tema", hint = "Uygulama teması sitenin temasından bağımsızdır.") {
        DeliDropdown(
            value = state.theme,
            options = state.personal.themeOptions.ifEmpty { listOf("midnight" to "midnight") },
            onSelect = vm::setTheme,
        )
        Spacer(Modifier.height(12.dp))
        FieldLabel("Tema rengi")
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .width(26.dp)
                    .height(26.dp)
                    .clip(CircleShape)
                    .background(parseHexColor(state.themeColor)),
            )
            Spacer(Modifier.width(10.dp))
            DeliField(
                value = state.themeColor,
                onValueChange = vm::setThemeColor,
                modifier = Modifier.width(160.dp),
                placeholder = "805ad5",
            )
        }
    }

    Spacer(Modifier.height(6.dp))
    if (state.saving) {
        LoadingBox()
    } else {
        DeliButtonPrimary("Değişiklikleri kaydet") { vm.savePersonal() }
    }
    Spacer(Modifier.height(8.dp))
    MutedText("Kişisel sekmedeki değişiklikler tek seferde siteye gönderilir.")
}

/* ------------------------------------------------------------------ */
/*  Security tab                                                       */
/* ------------------------------------------------------------------ */

@Composable
private fun SecurityTab(vm: AccountSettingsViewModel, state: com.delidehset.mobile.vm.AccountSettingsUiState) {
    SectionCard(title = "Şifre değiştir", hint = "Yeni şifre en az 8 karakter olmalı.") {
        DeliField(
            value = state.password.old,
            onValueChange = { vm.setPassword(it, state.password.new, state.password.confirm) },
            placeholder = "Mevcut şifre",
            password = true,
        )
        Spacer(Modifier.height(8.dp))
        DeliField(
            value = state.password.new,
            onValueChange = { vm.setPassword(state.password.old, it, state.password.confirm) },
            placeholder = "Yeni şifre",
            password = true,
        )
        Spacer(Modifier.height(8.dp))
        DeliField(
            value = state.password.confirm,
            onValueChange = { vm.setPassword(state.password.old, state.password.new, it) },
            placeholder = "Yeni şifre (tekrar)",
            password = true,
        )
        Spacer(Modifier.height(12.dp))
        DeliButtonPrimary("Şifreyi değiştir", enabled = !state.saving) { vm.savePassword() }
    }

    SectionCard(title = "E-posta adresi", hint = "Onay için mevcut şifreni girmen gerekir.") {
        DeliField(
            value = state.email.email,
            onValueChange = vm::setEmailAddress,
            placeholder = "ornek@eposta.com",
        )
        Spacer(Modifier.height(8.dp))
        DeliField(
            value = state.email.password,
            onValueChange = vm::setEmailPassword,
            placeholder = "Şifre",
            password = true,
        )
        Spacer(Modifier.height(12.dp))
        DeliButtonPrimary("E-postayı güncelle", enabled = !state.saving) { vm.saveEmail() }
    }

    SectionCard(title = "İki adımlı doğrulama") {
        MutedText(
            if (state.security.twoFactorAvailable) {
                "2FA kurulumu QR kod okutmayı gerektirdiği için bu adım şimdilik site üzerinden yapılıyor. Şifre ve e-posta değişikliği uygulama içinde."
            } else {
                "Bu hesap için site tarafında 2FA kurulumu görünmüyor."
            }
        )
    }
}

/* ------------------------------------------------------------------ */
/*  Small building blocks                                              */
/* ------------------------------------------------------------------ */

@Composable
private fun SectionCard(
    title: String,
    hint: String? = null,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .background(DeliColors.Surface, DeliShapes.control)
            .padding(14.dp),
    ) {
        Text(title, color = DeliColors.OnBackground, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        hint?.let {
            Spacer(Modifier.height(3.dp))
            MutedText(it)
        }
        Spacer(Modifier.height(10.dp))
        content()
    }
}

@Composable
private fun InlineError(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0x33F85149), DeliShapes.small)
            .padding(10.dp),
    ) { Text(text, color = DeliColors.Danger, fontSize = 13.sp) }
}

@Composable
private fun DeliField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(),
    placeholder: String? = null,
    password: Boolean = false,
    singleLine: Boolean = true,
    minLines: Int = 1,
    maxLines: Int = 1,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        singleLine = singleLine,
        minLines = minLines,
        maxLines = maxLines,
        placeholder = placeholder?.let { { Text(it, color = DeliColors.OnSurfaceVariant) } },
        visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None,
        shape = DeliShapes.control,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = DeliColors.Background,
            unfocusedContainerColor = DeliColors.Background,
            focusedTextColor = DeliColors.OnBackground,
            unfocusedTextColor = DeliColors.OnBackground,
            cursorColor = DeliColors.Primary,
            focusedIndicatorColor = DeliColors.Primary,
            unfocusedIndicatorColor = DeliColors.Border,
        ),
    )
}

@Composable
private fun DeliDropdown(value: String, options: List<Pair<String, String>>, onSelect: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val label = options.firstOrNull { it.first == value }?.second ?: value
    Box {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(DeliColors.Background, DeliShapes.control)
                .clickable { open = true }
                .padding(14.dp),
        ) {
            Text(label, color = DeliColors.OnBackground, fontSize = 14.sp)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }, containerColor = DeliColors.Surface) {
            for ((v, text) in options) {
                DropdownMenuItem(
                    text = { Text(text, color = DeliColors.OnBackground) },
                    onClick = { onSelect(v); open = false },
                )
            }
        }
    }
}

/**
 * Prepares the picked picture for the site's upload form.
 *
 * A picture straight out of the gallery is not something the form accepts: phone cameras
 * hand out 8–12 MB JPEGs and HEIC, the site's field wants a JPG, and the whole upload used
 * to fail on exactly that. The image is decoded (sampled down first, so a 50 MP photo does
 * not have to be loaded at full size), scaled to a sane edge length and re-encoded as JPEG,
 * which also drops the EXIF orientation problem along with most of the bytes.
 */
internal suspend fun readPickedFile(context: Context, uri: Uri): Triple<String, String, ByteArray>? =
    withContext(Dispatchers.IO) {
        runCatching {
            val rawName = context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0 && cursor.moveToFirst()) cursor.getString(idx) else null
            }
            val bytes = prepareUpload(context, uri)
            if (bytes == null) {
                android.util.Log.w("DeliUpload", "no usable image in $uri")
                return@runCatching null
            }
            val name = (rawName?.substringBeforeLast('.')?.takeIf { it.isNotBlank() } ?: "upload") + ".jpg"
            android.util.Log.d("DeliUpload", "picked $uri -> $name, ${bytes.size} B")
            Triple(name, "image/jpeg", bytes)
        }.onFailure { android.util.Log.w("DeliUpload", "could not read $uri", it) }
            .getOrNull()
    }

/** Longest edge of what actually gets uploaded. Plenty for a profile picture. */
private const val MAX_UPLOAD_EDGE = 1280

private fun prepareUpload(context: Context, uri: Uri): ByteArray? = runCatching {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@runCatching null

    var sample = 1
    while (bounds.outWidth / (sample * 2) >= MAX_UPLOAD_EDGE &&
        bounds.outHeight / (sample * 2) >= MAX_UPLOAD_EDGE
    ) {
        sample *= 2
    }

    val decoded = context.contentResolver.openInputStream(uri)?.use {
        BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
    } ?: return@runCatching null

    val longest = maxOf(decoded.width, decoded.height)
    val scaled = if (longest > MAX_UPLOAD_EDGE) {
        val ratio = MAX_UPLOAD_EDGE / longest.toFloat()
        val resized = Bitmap.createScaledBitmap(
            decoded,
            (decoded.width * ratio).toInt().coerceAtLeast(1),
            (decoded.height * ratio).toInt().coerceAtLeast(1),
            true,
        )
        if (resized !== decoded) decoded.recycle()
        resized
    } else {
        decoded
    }

    val out = ByteArrayOutputStream()
    scaled.compress(Bitmap.CompressFormat.JPEG, 88, out)
    scaled.recycle()
    out.toByteArray().takeIf { it.isNotEmpty() }
}.onFailure { android.util.Log.w("DeliUpload", "could not prepare image", it) }
    .getOrNull()

private fun parseHexColor(value: String): Color = runCatching {
    val hex = value.trim().removePrefix("#")
    when (hex.length) {
        6 -> Color(0xFF000000 or hex.toLong(16))
        8 -> Color(hex.toLong(16))
        else -> DeliColors.SurfaceVariant
    }
}.getOrDefault(DeliColors.SurfaceVariant)
