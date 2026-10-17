package com.delidehset.mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import com.delidehset.mobile.ui.Text
import com.delidehset.mobile.ui.tr
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.delidehset.mobile.ui.components.DeliButtonPrimary
import com.delidehset.mobile.ui.components.LoadingBox
import com.delidehset.mobile.ui.components.MutedText
import com.delidehset.mobile.ui.deliViewModel
import com.delidehset.mobile.ui.theme.DeliColors
import com.delidehset.mobile.vm.ContactViewModel
import com.delidehset.mobile.ui.theme.DeliShapes

/**
 * The site's "Bize ulaşın" page, drawn natively.
 *
 * The page is a single `message` textarea posted to `/contact`, so there is nothing to
 * host in a browser: the same request is made from here.
 */
@Composable
fun ContactScreen(
    contentPadding: PaddingValues,
    onMessage: (String) -> Unit,
) {
    val vm = deliViewModel { ContactViewModel(it.repository) }
    val state by vm.state.collectAsStateWithLifecycle()
    var text by remember { mutableStateOf("") }

    LaunchedEffect(state.sent) {
        if (state.sent) {
            onMessage(tr("Mesaj gönderildi."))
            text = ""
            vm.consume()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeliColors.Background)
            .verticalScroll(rememberScrollState())
            .padding(
                start = 16.dp,
                end = 16.dp,
                top = 16.dp,
                bottom = contentPadding.calculateBottomPadding() + 24.dp,
            ),
    ) {
        Text("Bize ulaşın", color = DeliColors.OnBackground, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp))
        MutedText("Mesajın site yöneticilerine iletilir. En fazla 10000 karakter.")

        Spacer(Modifier.height(14.dp))
        OutlinedTextField(
            value = text,
            onValueChange = { if (it.length <= 10_000) text = it },
            modifier = Modifier.fillMaxWidth(),
            minLines = 6,
            maxLines = 12,
            placeholder = { Text("Mesajın", color = DeliColors.OnSurfaceVariant) },
            shape = DeliShapes.control,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = DeliColors.Surface,
                unfocusedContainerColor = DeliColors.Surface,
                focusedTextColor = DeliColors.OnBackground,
                unfocusedTextColor = DeliColors.OnBackground,
                cursorColor = DeliColors.Primary,
                focusedIndicatorColor = DeliColors.Primary,
                unfocusedIndicatorColor = DeliColors.Border,
            ),
        )

        state.error?.let {
            Spacer(Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0x33F85149), DeliShapes.small)
                    .padding(10.dp),
            ) { Text(it, color = DeliColors.Danger, fontSize = 13.sp) }
        }

        Spacer(Modifier.height(16.dp))
        if (state.sending) {
            LoadingBox()
        } else {
            DeliButtonPrimary("Gönder", enabled = text.isNotBlank()) { vm.send(text) }
        }
    }
}
