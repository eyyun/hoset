package com.delidehset.mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.delidehset.mobile.BuildConfig
import com.delidehset.mobile.ui.Text
import com.delidehset.mobile.ui.tf
import com.delidehset.mobile.ui.theme.DeliColors
import com.delidehset.mobile.vm.UpdateState

/**
 * The update prompt, in the app's own shapes.
 *
 * One dialog for every stage of the same conversation — offered, downloading, handed to the
 * installer — so the answer to "what is it doing" never has to be found in a notification.
 */
@Composable
fun UpdateDialog(
    state: UpdateState,
    onDownload: () -> Unit,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
) {
    val title = when (state) {
        is UpdateState.Available -> "Güncelleme var"
        is UpdateState.Downloading -> "Güncelleme indiriliyor"
        is UpdateState.Ready -> "Kurulum açıldı"
        is UpdateState.UpToDate -> "Güncel"
        is UpdateState.Failed, UpdateState.Checking, UpdateState.Idle -> "Güncelleme"
    }
    val confirm = when (state) {
        is UpdateState.Available -> "İndir ve kur"
        is UpdateState.Failed -> "Tekrar dene"
        else -> "Tamam"
    }
    val dismiss = when (state) {
        is UpdateState.Available -> "Sonra"
        is UpdateState.Downloading -> "Arka planda"
        else -> null
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DeliColors.Surface,
        title = { Text(title, color = DeliColors.OnBackground) },
        text = {
            Column {
                when (state) {
                    is UpdateState.Available -> {
                        Text(
                            text = tf("Yeni sürüm {0} hazır. Şu an {1}.", state.info.version, BuildConfig.VERSION_NAME),
                            color = DeliColors.OnBackground,
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = if (state.info.sizeBytes > 0) {
                                tf(
                                    "İndirilecek dosya {0} MB. Kurulumu Android'in kendi onay ekranı soracak.",
                                    state.info.sizeBytes / (1024 * 1024),
                                )
                            } else {
                                "Kurulumu Android'in kendi onay ekranı soracak."
                            },
                            color = DeliColors.OnSurfaceVariant,
                            fontSize = 12.5.sp,
                            lineHeight = 18.sp,
                        )
                    }
                    is UpdateState.Downloading -> {
                        val percent = (state.progress * 100).toInt().coerceIn(0, 100)
                        Text(
                            text = tf("İndiriliyor… %{0}", percent),
                            color = DeliColors.OnBackground,
                            fontSize = 14.sp,
                        )
                        Spacer(Modifier.height(10.dp))
                        LinearProgressIndicator(
                            progress = { state.progress.coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth(),
                            color = DeliColors.Primary,
                            trackColor = DeliColors.SurfaceVariant,
                        )
                    }
                    is UpdateState.Ready -> Text(
                        text = "İndirdim, sistemin kurulum ekranına verdim. Onaylarsan uygulama kendini yeniden başlatır; oturumun ve ayarların kalır.",
                        color = DeliColors.OnBackground,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                    )
                    is UpdateState.UpToDate -> Text(
                        text = tf("En son sürümü kullanıyorsun ({0}).", state.version),
                        color = DeliColors.OnBackground,
                        fontSize = 14.sp,
                    )
                    is UpdateState.Failed -> Text(
                        text = state.message,
                        color = DeliColors.Danger,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                    )
                    UpdateState.Checking -> Text(
                        text = "Denetleniyor…",
                        color = DeliColors.OnSurfaceVariant,
                        fontSize = 14.sp,
                    )
                    UpdateState.Idle -> Text(
                        text = "Şu an yeni bir sürüm yok.",
                        color = DeliColors.OnSurfaceVariant,
                        fontSize = 14.sp,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = when (state) {
                is UpdateState.Available -> onDownload
                is UpdateState.Failed -> onRetry
                else -> onDismiss
            }) {
                Text(confirm, color = DeliColors.Blue, fontWeight = FontWeight.Medium)
            }
        },
        dismissButton = if (dismiss == null) null else {
            {
                TextButton(onClick = onDismiss) {
                    Text(dismiss, color = DeliColors.OnSurfaceVariant)
                }
            }
        },
    )
}

/**
 * The drawer's last line: the version this build is, and a way to ask.
 *
 * It is also where a waiting update announces itself, because the drawer is the one place
 * that is one tap away from every screen.
 */
@Composable
fun UpdateRow(
    version: String,
    state: UpdateState,
    onCheck: () -> Unit,
) {
    val available = state is UpdateState.Available
    val progress = (state as? UpdateState.Downloading)?.progress

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable { onCheck() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Filled.Refresh,
                contentDescription = null,
                tint = if (available) DeliColors.Primary else DeliColors.OnSurfaceVariant,
                modifier = Modifier.size(17.dp),
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = when {
                    progress != null -> tf("Güncelleniyor… %{0}", (progress * 100).toInt().coerceIn(0, 100))
                    state is UpdateState.Checking -> "Güncelleme denetleniyor…"
                    available -> "Güncelleme hazır"
                    else -> tf("Sürüm {0}", version)
                },
                color = if (available) DeliColors.Primary else DeliColors.OnSurfaceVariant,
                fontSize = 12.5.sp,
                fontWeight = if (available) FontWeight.SemiBold else FontWeight.Normal,
                modifier = Modifier.weight(1f),
            )
            if (available) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(DeliColors.PrimaryContainer)
                        .padding(horizontal = 7.dp, vertical = 2.dp),
                ) {
                    Text("Yeni", color = DeliColors.OnBackground, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}
