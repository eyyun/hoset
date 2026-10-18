package com.delidehset.mobile.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.delidehset.mobile.data.IconStore
import com.delidehset.mobile.ui.Text
import com.delidehset.mobile.ui.components.DeliButtonPrimary
import com.delidehset.mobile.ui.t
import com.delidehset.mobile.ui.theme.DeliColors
import com.delidehset.mobile.ui.theme.DeliShapes

/**
 * Picks the square of a picture that becomes the icon.
 *
 * One square viewfinder, drag to move the picture under it, pinch to zoom. The crop is
 * computed in the picture's own pixels (see [IconStore.cropBox]), so what is inside the
 * frame is exactly what ends up on the home screen rather than an approximation of it.
 */
@Composable
fun IconCropDialog(
    bitmap: Bitmap,
    title: String,
    onDismiss: () -> Unit,
    onCropped: (Bitmap) -> Unit,
) {
    var boxPx by remember { mutableFloatStateOf(0f) }
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    // The picture covers the frame at zoom 1; panning is clamped to what the frame can
    // show, so the background never appears behind a corner while dragging.
    fun clamp(next: Offset, zoom: Float): Offset {
        if (boxPx <= 0f) return next
        val cover = maxOf(boxPx / bitmap.width, boxPx / bitmap.height)
        val drawn = cover * zoom
        val maxX = ((bitmap.width * drawn - boxPx) / 2f).coerceAtLeast(0f)
        val maxY = ((bitmap.height * drawn - boxPx) / 2f).coerceAtLeast(0f)
        return Offset(next.x.coerceIn(-maxX, maxX), next.y.coerceIn(-maxY, maxY))
    }

    val gestures = rememberTransformableState { zoomChange, panChange, _ ->
        val next = (scale * zoomChange).coerceIn(1f, 6f)
        scale = next
        offset = clamp(offset + panChange, next)
    }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .clip(DeliShapes.card)
                .background(DeliColors.Surface)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = title,
                color = DeliColors.OnBackground,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(DeliShapes.control)
                    .background(DeliColors.Background)
                    .onSizeChanged { boxPx = it.width.toFloat() }
                    .transformable(gestures),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            translationX = offset.x
                            translationY = offset.y
                        },
                )
                // The frame itself: what is inside it is the icon.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .border(1.5.dp, Color.White.copy(alpha = 0.8f), DeliShapes.control),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onDismiss) {
                    Text(t("Vazgeç"), color = DeliColors.OnSurfaceVariant)
                }
                DeliButtonPrimary(
                    text = t("Kullan"),
                    modifier = Modifier.weight(1f),
                ) {
                    val box = IconStore.cropBox(
                        imageWidth = bitmap.width,
                        imageHeight = bitmap.height,
                        viewfinder = boxPx,
                        scale = scale,
                        offsetX = offset.x,
                        offsetY = offset.y,
                    )
                    if (box != null) {
                        onCropped(
                            Bitmap.createBitmap(bitmap, box.x, box.y, box.width, box.height),
                        )
                    }
                }
            }
        }
    }
}
