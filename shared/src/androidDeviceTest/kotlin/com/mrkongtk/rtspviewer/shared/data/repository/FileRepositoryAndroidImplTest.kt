package com.mrkongtk.rtspviewer.shared.data.repository

import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FileRepositoryAndroidTest : FileRepositoryTest() {

    override fun createRepository(): FileRepository {
        return FileRepositoryAndroidImpl(ApplicationProvider.getApplicationContext())
    }

    override fun createSolidImageBitmap(w: Int, h: Int, color: Color): ImageBitmap {
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(color.toArgb())
        return bitmap.asImageBitmap()
    }
}
