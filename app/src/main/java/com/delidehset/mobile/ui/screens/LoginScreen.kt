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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import com.delidehset.mobile.ui.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.delidehset.mobile.ui.components.DeliButtonPrimary
import com.delidehset.mobile.ui.components.DeliOutlinedButton
import com.delidehset.mobile.ui.components.FieldLabel
import com.delidehset.mobile.ui.components.LoadingBox
import com.delidehset.mobile.ui.components.MutedText
import com.delidehset.mobile.ui.theme.DeliColors
import com.delidehset.mobile.vm.AuthViewModel

@Composable
fun LoginScreen(
    vm: AuthViewModel,
    contentPadding: PaddingValues,
    onSignedIn: () -> Unit,
    onOpenWebAuth: (mode: String) -> Unit,
    onMessage: (String) -> Unit,
) {
    val state by vm.state.collectAsStateWithLifecycle()
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    LaunchedEffect(state.done) { if (state.done) onSignedIn() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeliColors.Background)
            .verticalScroll(rememberScrollState())
            .padding(
                start = 20.dp,
                end = 20.dp,
                top = 24.dp,
                bottom = contentPadding.calculateBottomPadding() + 24.dp,
            ),
    ) {
        Text("Oturum Aç", color = DeliColors.OnBackground, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        MutedText("Delidehşet hesabınla giriş yap. Oturum cihazda saklanır.")

        Spacer(Modifier.height(18.dp))

        FieldLabel("Kullanıcı adı")
        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(8.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            colors = deliFieldColors(),
        )

        FieldLabel("Şifre")
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(8.dp),
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            colors = deliFieldColors(),
        )

        state.error?.let {
            Spacer(Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0x33F85149), RoundedCornerShape(6.dp))
                    .padding(10.dp)
            ) { Text(it, color = DeliColors.Danger, fontSize = 13.sp) }
        }

        Spacer(Modifier.height(18.dp))
        if (state.busy) {
            LoadingBox()
        } else {
            DeliButtonPrimary("Giriş yap", enabled = username.isNotBlank() && password.isNotBlank()) {
                vm.login(username, password, onSuccess = {})
            }
        }

        Spacer(Modifier.height(24.dp))

        // Everything else is native, but signing up needs Cloudflare Turnstile, which is
        // a browser challenge — so it opens the real page inside the app and hands the
        // session back the moment it exists.
        DeliOutlinedButton("Kayıt ol (uygulama içinde)") { onOpenWebAuth("signup") }
        Spacer(Modifier.height(8.dp))
        Text(
            "Kayıt ekranı, site doğrulaması için uygulama içinde açılır; giriş yaptığın an oturum native tarafa aktarılır.",
            color = DeliColors.OnSurfaceVariant,
            fontSize = 11.sp,
        )

        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            MutedText("Giriş yapamıyor musun?")
            Text(
                "Site içinde dene",
                color = DeliColors.Blue,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 6.dp).clickable { onOpenWebAuth("login") },
            )
        }
    }
}

@Composable
private fun deliFieldColors() = TextFieldDefaults.colors(
    focusedContainerColor = DeliColors.Surface,
    unfocusedContainerColor = DeliColors.Surface,
    focusedTextColor = DeliColors.OnBackground,
    unfocusedTextColor = DeliColors.OnBackground,
    cursorColor = DeliColors.Primary,
    focusedIndicatorColor = DeliColors.Primary,
    unfocusedIndicatorColor = DeliColors.Border,
)
