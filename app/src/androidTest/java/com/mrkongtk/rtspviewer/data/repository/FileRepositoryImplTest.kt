package com.mrkongtk.rtspviewer.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream

@RunWith(AndroidJUnit4::class)
@SmallTest
class FileRepositoryImplTest {

    private lateinit var context: Context
    private lateinit var repository: FileRepositoryImpl
    private lateinit var testDir: File

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        repository = FileRepositoryImpl(context)

        // Create a specific directory for tests to keep things clean
        testDir = File(context.cacheDir, "test_images")
        if (!testDir.exists()) {
            testDir.mkdirs()
        }
    }

    @After
    fun tearDown() {
        // cleanup files after tests
        if (testDir.exists()) {
            testDir.deleteRecursively()
        }
    }

    @Test
    fun writeJPEG_savesBitmapToFile_returnsTrue() = runBlocking {
        // Arrange
        val bitmap = createSolidBitmap(100, 100, Color.RED)
        val destFile = File(testDir, "test_save.jpg")

        // Act
        val result = repository.writeJPEG(destFile, bitmap)

        // Assert
        assertTrue("Function should return true on success", result)
        assertTrue("File should exist on disk", destFile.exists())
        assertTrue("File size should be greater than 0", destFile.length() > 0)
    }

    @Test
    fun writeJPEG_overwritesExistingFile_returnsTrue() = runBlocking {
        // Arrange
        val destFile = File(testDir, "overwrite_test.jpg")

        // 1. Write initial file (Blue)
        val blueBitmap = createSolidBitmap(100, 100, Color.BLUE)
        repository.writeJPEG(destFile, blueBitmap)
        val initialSize = destFile.length()

        // 2. Create new bitmap (Red) - potentially different compression size
        val redBitmap = createSolidBitmap(100, 100, Color.RED)

        // Act
        val result = repository.writeJPEG(destFile, redBitmap)

        // Assert
        assertTrue(result)
        assertTrue(destFile.exists())
        // We ensure the file is still a valid image
        val loaded = repository.readJPEG(destFile)
        assertNotNull(loaded)
        assertEquals(100, loaded!!.width)
    }

    @Test
    fun readJPEG_validFile_returnsBitmap() = runBlocking {
        // Arrange
        val destFile = File(testDir, "read_test.jpg")
        val originalBitmap = createSolidBitmap(50, 50, Color.GREEN)
        repository.writeJPEG(destFile, originalBitmap)

        // Act
        val resultBitmap = repository.readJPEG(destFile)

        // Assert
        assertNotNull("Should return a bitmap", resultBitmap)
        assertEquals(50, resultBitmap!!.width)
        assertEquals(50, resultBitmap.height)
    }

    @Test
    fun readJPEG_nonExistentFile_returnsNull() = runBlocking {
        // Arrange
        val missingFile = File(testDir, "ghost.jpg")

        // Act
        val result = repository.readJPEG(missingFile)

        // Assert
        assertNull("Should return null for missing files", result)
    }

    @Test
    fun readJPEG_corruptFile_returnsNull() = runBlocking {
        // Arrange
        val corruptFile = File(testDir, "not_an_image.txt")
        // Write text data instead of image data
        FileOutputStream(corruptFile).use {
            it.write("This is just text, not a JPEG".toByteArray())
        }

        // Act
        val result = repository.readJPEG(corruptFile)

        // Assert
        assertNull("Should return null if file cannot be decoded", result)
    }

    @Test
    fun roundTrip_integrityCheck() = runBlocking {
        // Arrange
        val destFile = File(testDir, "integrity.jpg")
        val width = 20
        val height = 20
        val color = Color.BLUE
        val original = createSolidBitmap(width, height, color)

        // Act
        repository.writeJPEG(destFile, original)
        val loaded = repository.readJPEG(destFile)

        // Assert
        assertNotNull(loaded)
        assertEquals(width, loaded!!.width)
        assertEquals(height, loaded.height)

        // Check a pixel in the middle
        // Note: JPEG compression is lossy, so exact color matching
        // implies we account for artifacts, but solid colors usually survive well.
        // We check if the pixel is not transparent and generally blue-ish.
        val pixel = loaded.getPixel(10, 10)

        // Alpha should be 255 (opaque) because JPEG doesn't support transparency
        assertEquals(255, Color.alpha(pixel))
        // Blue component should be high
        assertTrue(Color.blue(pixel) > 200)
    }

    /**
     * Helper to create a simple bitmap for testing
     */
    private fun createSolidBitmap(w: Int, h: Int, color: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(color)
        return bitmap
    }
}
