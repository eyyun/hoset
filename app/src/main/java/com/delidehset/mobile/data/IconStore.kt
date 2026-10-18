package com.delidehset.mobile.data

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import java.io.File

/**
 * A home-screen icon made from the person's own picture.
 *
 * Android decides an app's launcher icon at install time, so "choose your icon" cannot mean
 * quietly repainting the one already on the home screen. What it can mean — and what this
 * does — is a pinned shortcut carrying the cropped picture: the same app, the same label,
 * a face you picked. The picture is also kept on disk so the settings row can show it and
 * offer to pin it again after a reinstall.
 *
 * Cropping is done in source pixels, from a square viewfinder the person dragged and
 * pinched; [cropBox] is the arithmetic for that and is pure, so it is covered by tests.
 */
object IconStore {

    /** Launcher icons are drawn at up to 192 dp; 512 px covers every density in use. */
    private const val ICON_PX = 512

    private const val SHORTCUT_ID = "hoset-custom-icon"

    fun file(context: Context): File = File(context.filesDir, "custom_icon.png")

    fun hasCustom(context: Context): Boolean = file(context).exists()

    fun load(context: Context): Bitmap? =
        runCatching { BitmapFactory.decodeFile(file(context).absolutePath) }.getOrNull()

    /**
     * Saves a cropped square and pins it to the home screen.
     *
     * Returns the saved file, or null when the platform has no launcher that accepts a
     * pinned shortcut — the picture is still saved, so the row keeps it.
     */
    fun saveAndPin(context: Context, cropped: Bitmap, label: String): File? {
        val square = Bitmap.createScaledBitmap(cropped, ICON_PX, ICON_PX, true)
        val target = file(context)
        val saved = runCatching {
            target.outputStream().use { out -> square.compress(Bitmap.CompressFormat.PNG, 100, out) }
        }.isSuccess
        if (!saved) return null
        pin(context, square, label)
        return target
    }

    private fun pin(context: Context, bitmap: Bitmap, label: String) {
        runCatching {
            val shortcut = ShortcutInfoCompat.Builder(context, SHORTCUT_ID)
                .setShortLabel(label.take(40).ifBlank { "Hoset" })
                .setLongLabel(label.take(40).ifBlank { "Hoset" })
                .setIcon(IconCompat.createWithAdaptiveBitmap(bitmap))
                .setIntent(
                    Intent(context, com.delidehset.mobile.MainActivity::class.java)
                        .setAction(Intent.ACTION_VIEW),
                )
                .build()
            ShortcutManagerCompat.requestPinShortcut(context, shortcut, null)
        }
    }

    /** One crop of the picture, in its own pixels. */
    data class CropBox(val x: Int, val y: Int, val width: Int, val height: Int)

    /**
     * The part of an image that fills a square viewfinder.
     *
     * The picture is drawn to *cover* the viewfinder at zoom 1, then scaled and panned, so
     * the source rectangle is the inverse of that transform. Everything is clamped to the
     * image, which is what keeps a corner of empty background out of the icon when somebody
     * drags past the edge.
     */
    fun cropBox(
        imageWidth: Int,
        imageHeight: Int,
        viewfinder: Float,
        scale: Float,
        offsetX: Float,
        offsetY: Float,
    ): CropBox? {
        if (imageWidth <= 0 || imageHeight <= 0 || viewfinder <= 0f) return null
        val cover = maxOf(viewfinder / imageWidth, viewfinder / imageHeight)
        val zoom = (cover * scale.coerceAtLeast(1f))
        // Drawn size on screen, and where its top-left corner sits.
        val drawnW = imageWidth * zoom
        val drawnH = imageHeight * zoom
        val maxDx = (drawnW - viewfinder) / 2f
        val maxDy = (drawnH - viewfinder) / 2f
        val dx = offsetX.coerceIn(-maxDx, maxDx)
        val dy = offsetY.coerceIn(-maxDy, maxDy)
        val left = viewfinder / 2f + dx - drawnW / 2f
        val top = viewfinder / 2f + dy - drawnH / 2f

        val x = (-left / zoom).toInt().coerceIn(0, imageWidth - 1)
        val y = (-top / zoom).toInt().coerceIn(0, imageHeight - 1)
        val side = (viewfinder / zoom).toInt()
            .coerceIn(1, minOf(imageWidth - x, imageHeight - y))
        return CropBox(x, y, side, side)
    }
}
