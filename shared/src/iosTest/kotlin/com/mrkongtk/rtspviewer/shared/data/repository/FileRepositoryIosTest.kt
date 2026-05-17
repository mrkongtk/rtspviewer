package com.mrkongtk.rtspviewer.shared.data.repository

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asComposeImageBitmap
import androidx.compose.ui.graphics.toArgb
import org.jetbrains.skia.Bitmap

class FileRepositoryIosTest : FileRepositoryTest() {

    override fun createRepository(): FileRepository {
        return FileRepositoryIosImpl()
    }

    override fun createSolidImageBitmap(w: Int, h: Int, color: Color): ImageBitmap {
        val skiaBitmap = Bitmap()
        skiaBitmap.allocN32Pixels(w, h)
        val canvas = org.jetbrains.skia.Canvas(skiaBitmap)
        canvas.clear(color.toArgb())
        return skiaBitmap.asComposeImageBitmap()
    }
}
