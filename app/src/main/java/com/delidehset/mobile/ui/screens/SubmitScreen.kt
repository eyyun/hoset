package com.delidehset.mobile.ui.screens

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import com.delidehset.mobile.ui.Text
import com.delidehset.mobile.ui.tr
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.delidehset.mobile.ui.components.DeliButtonPrimary
import com.delidehset.mobile.ui.components.ErrorBox
import com.delidehset.mobile.ui.components.FieldLabel
import com.delidehset.mobile.ui.components.LoadingBox
import com.delidehset.mobile.ui.components.MutedText
import com.delidehset.mobile.ui.theme.DeliColors
import com.delidehset.mobile.vm.SubmitViewModel
import com.delidehset.mobile.ui.theme.DeliShapes

@Composable
fun SubmitScreen(
    vm: SubmitViewModel,
    signedIn: Boolean,
    contentPadding: PaddingValues,
    onSubmitted: () -> Unit,
    onMessage: (String) -> Unit,
    onOpenLogin: () -> Unit,
    /** When the composer is opened from a community, it starts on that community. */
    initialHole: String? = null,
) {
    val state by vm.state.collectAsStateWithLifecycle()
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var hole by remember(initialHole) { mutableStateOf(initialHole.orEmpty()) }
    var nsfw by remember { mutableStateOf(false) }
    var anonymous by remember { mutableStateOf(!signedIn) }
    var tripcode by remember { mutableStateOf("") }
    var holeMenu by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { vm.load() }
    LaunchedEffect(state.done) {
        if (state.done) {
            onMessage(tr("Gönderi paylaşıldı."))
            onSubmitted()
        }
    }

    val holeOptions = state.form?.holeOptions.orEmpty()
    val isAnonymousForm = state.form?.fields?.any { it.name == "anonymous" } == true

    when {
        state.loading -> LoadingBox(Modifier.fillMaxSize())

        state.form == null && state.error != null ->
            ErrorBox(state.error!!, Modifier.fillMaxSize(), onRetry = { vm.load(true) })

        else -> Column(
            modifier = Modifier
                .fillMaxSize()
                .background(DeliColors.Background)
                .verticalScroll(rememberScrollState())
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    top = 16.dp,
                    bottom = contentPadding.calculateBottomPadding() + 32.dp,
                ),
        ) {
            Text("Gönderi oluştur", color = DeliColors.OnBackground, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            MutedText("Site ile aynı alanlar: title, body, hole.")

            Spacer(Modifier.height(8.dp))

            // ---- hole ----
            FieldLabel("Topluluk")
            Box(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DeliColors.Surface, DeliShapes.control)
                        .clickable { holeMenu = true }
                        .padding(14.dp)
                ) {
                    Text(
                        text = hole.ifBlank { "Bir topluluk seç" },
                        color = if (hole.isBlank()) DeliColors.OnSurfaceVariant else DeliColors.OnBackground,
                        fontSize = 14.sp,
                    )
                }
                DropdownMenu(
                    expanded = holeMenu,
                    onDismissRequest = { holeMenu = false },
                    containerColor = DeliColors.Surface,
                ) {
                    if (holeOptions.isEmpty()) {
                        DropdownMenuItem(
                            text = { Text("Topluluk bulunamadı", color = DeliColors.OnSurfaceVariant) },
                            onClick = { holeMenu = false },
                        )
                    }
                    for ((value, label) in holeOptions) {
                        DropdownMenuItem(
                            text = { Text(label, color = DeliColors.OnBackground) },
                            onClick = { hole = value; holeMenu = false },
                        )
                    }
                }
            }

            // ---- title ----
            FieldLabel("Başlık (en fazla 500)")
            OutlinedTextField(
                value = title,
                onValueChange = { if (it.length <= 500) title = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Zorunlu", color = DeliColors.OnSurfaceVariant) },
                shape = DeliShapes.control,
                colors = submitFieldColors(),
            )

            // ---- body ----
            FieldLabel("Metin")
            OutlinedTextField(
                value = body,
                onValueChange = { if (it.length <= 50000) body = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Bağlantı veya görsel varsa isteğe bağlı", color = DeliColors.OnSurfaceVariant) },
                minLines = 6,
                maxLines = 16,
                shape = DeliShapes.control,
                colors = submitFieldColors(),
            )

            // ---- nsfw ----
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("NSFW", color = DeliColors.OnBackground, fontSize = 14.sp)
                Spacer(Modifier.weight(1f))
                Switch(
                    checked = nsfw,
                    onCheckedChange = { nsfw = it },
                    colors = SwitchDefaults.colors(checkedThumbColor = DeliColors.Primary),
                )
            }

            // ---- anonymous (only when the server offers it) ----
            if (isAnonymousForm) {
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Anonim gönder", color = DeliColors.OnBackground, fontSize = 14.sp)
                        MutedText("Tripcode ile aynı kimlik altında görünürsün, gizli değer saklanmaz.")
                    }
                    Switch(
                        checked = anonymous,
                        onCheckedChange = { anonymous = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = DeliColors.Primary),
                    )
                }
                if (anonymous) {
                    FieldLabel("Tripcode gizli değeri")
                    OutlinedTextField(
                        value = tripcode,
                        onValueChange = { tripcode = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        placeholder = { Text("Aynı gizli değeri kullan", color = DeliColors.OnSurfaceVariant) },
                        shape = DeliShapes.control,
                        colors = submitFieldColors(),
                    )
                }
            }

            state.error?.let {
                Spacer(Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0x33F85149), DeliShapes.small)
                        .padding(10.dp)
                ) { Text(it, color = DeliColors.Danger, fontSize = 13.sp) }
            }

            Spacer(Modifier.height(18.dp))
            if (!signedIn && isAnonymousForm) {
                MutedText("Oturum açmadan da gönderi paylaşabilirsin (anonim).")
                Spacer(Modifier.height(8.dp))
            }
            if (state.sending) {
                LoadingBox()
            } else {
                DeliButtonPrimary("Paylaş", enabled = title.isNotBlank() && hole.isNotBlank()) {
                    vm.submit(
                        title = title,
                        body = body,
                        hole = hole,
                        nsfw = nsfw,
                        anonymous = anonymous && isAnonymousForm,
                        tripcodeSecret = tripcode.takeIf { it.isNotBlank() },
                    )
                }
            }
            if (!signedIn) {
                Spacer(Modifier.height(10.dp))
                Text(
                    "Normal (anonim olmayan) gönderi için oturum aç",
                    color = DeliColors.Blue,
                    fontSize = 12.sp,
                    modifier = Modifier.clickable { onOpenLogin() },
                )
            }
        }
    }
}

@Composable
private fun submitFieldColors() = TextFieldDefaults.colors(
    focusedContainerColor = DeliColors.Surface,
    unfocusedContainerColor = DeliColors.Surface,
    focusedTextColor = DeliColors.OnBackground,
    unfocusedTextColor = DeliColors.OnBackground,
    cursorColor = DeliColors.Primary,
    focusedIndicatorColor = DeliColors.Primary,
    unfocusedIndicatorColor = DeliColors.Border,
)
