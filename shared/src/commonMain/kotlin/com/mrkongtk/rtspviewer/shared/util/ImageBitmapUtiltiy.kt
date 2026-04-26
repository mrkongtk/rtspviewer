package com.mrkongtk.rtspviewer.shared.util

import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint


/**
 * Creates an [ImageBitmap] of a specified size filled with a single solid color.
 *
 * @param width The width of the bitmap in pixels.
 * @param height The height of the bitmap in pixels.
 * @param colour The [Color] to fill the entire bitmap with.
 * @return A new [ImageBitmap] instance with the specified dimensions and color.
 */
fun ImageBitmap.Companion.createPlainImage(width: Int, height: Int, colour: Color): ImageBitmap {
    return ImageBitmap(width, height).also {
        val canvas = Canvas(it)

        val paint = Paint().apply {
            color = colour
        }

        canvas.drawRect(
            left = 0f,
            top = 0f,
            right = width.toFloat(),
            bottom = height.toFloat(),
            paint = paint
        )
    }
}
