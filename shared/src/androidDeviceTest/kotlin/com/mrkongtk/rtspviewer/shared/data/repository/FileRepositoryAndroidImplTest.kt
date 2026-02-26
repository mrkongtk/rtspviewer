package com.mrkongtk.rtspviewer.shared.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import kotlinx.coroutines.runBlocking
import okio.FileSystem
import okio.Path
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class FileRepositoryAndroidImplTest {

    private lateinit var context: Context
    private lateinit var repository: FileRepositoryAndroidImpl
    private val fileSystem = FileSystem.SYSTEM
    private lateinit var testDir: Path

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        repository = FileRepositoryAndroidImpl(context)

        // Initialize test directory using Okio Paths
        testDir = repository.getCacheDir() / "test_images"
        if (!fileSystem.exists(testDir)) {
            fileSystem.createDirectories(testDir)
        }
    }

    @After
    fun tearDown() {
        // Cleanup files after tests
        if (fileSystem.exists(testDir)) {
            fileSystem.deleteRecursively(testDir)
        }
    }

    @Test
    fun writeJPEG_savesImageBitmapToFile_returnsTrue() = runBlocking {
        // Arrange
        val bitmap = createSolidImageBitmap(100, 100, Color.RED)
        val destFile = testDir / "test_save.jpg"

        // Act
        val result = repository.writeJPEG(destFile, bitmap)

        // Assert
        assertTrue("Function should return true on success", result)
        assertTrue("File should exist on disk", fileSystem.exists(destFile))
        assertTrue("File size should be greater than 0", fileSystem.metadata(destFile).size!! > 0)
    }

    @Test
    fun writeJPEG_overwritesExistingFile_returnsTrue() = runBlocking {
        // Arrange
        val destFile = testDir / "overwrite_test.jpg"

        // 1. Write initial file (Blue)
        val blueBitmap = createSolidImageBitmap(100, 100, Color.BLUE)
        repository.writeJPEG(destFile, blueBitmap)

        // 2. Create new bitmap (Red)
        val redBitmap = createSolidImageBitmap(150, 150, Color.RED)

        // Act
        val result = repository.writeJPEG(destFile, redBitmap)

        // Assert
        assertTrue(result)
        assertTrue(fileSystem.exists(destFile))

        // Verify content changed (check width of loaded image)
        val loaded = repository.readJPEG(destFile)
        assertNotNull(loaded)
        assertEquals(150, loaded!!.width)
    }

    @Test
    fun readJPEG_validFile_returnsImageBitmap() = runBlocking {
        // Arrange
        val destFile = testDir / "read_test.jpg"
        val originalBitmap = createSolidImageBitmap(50, 50, Color.GREEN)
        repository.writeJPEG(destFile, originalBitmap)

        // Act
        val resultBitmap = repository.readJPEG(destFile)

        // Assert
        assertNotNull("Should return an ImageBitmap", resultBitmap)
        assertEquals(50, resultBitmap!!.width)
        assertEquals(50, resultBitmap.height)
    }

    @Test
    fun readJPEG_nonExistentFile_returnsNull() = runBlocking {
        // Arrange
        val missingFile = testDir / "ghost.jpg"

        // Act
        val result = repository.readJPEG(missingFile)

        // Assert
        assertNull("Should return null for missing files", result)
    }

    @Test
    fun readJPEG_corruptFile_returnsNull() = runBlocking {
        // Arrange
        val corruptFile = testDir / "not_an_image.txt"
        // Write garbage text data using Okio
        fileSystem.write(corruptFile) {
            writeUtf8("This is just text, not a JPEG")
        }

        // Act
        val result = repository.readJPEG(corruptFile)

        // Assert
        assertNull("Should return null if file cannot be decoded", result)
    }

    @Test
    fun roundTrip_integrityCheck() = runBlocking {
        // Arrange
        val destFile = testDir / "integrity.jpg"
        val width = 20
        val height = 20
        val color = Color.BLUE
        val original = createSolidImageBitmap(width, height, color)

        // Act
        repository.writeJPEG(destFile, original)
        val loaded = repository.readJPEG(destFile)

        // Assert
        assertNotNull(loaded)
        val androidBitmap = loaded!!.asAndroidBitmap()
        assertEquals(width, androidBitmap.width)
        assertEquals(height, androidBitmap.height)

        // Check a pixel in the middle
        val pixel = androidBitmap.getPixel(10, 10)

        // JPEG is lossy, but Alpha should remain opaque (255)
        assertEquals(255, Color.alpha(pixel))
        // Blue component should be high for a blue solid bitmap
        assertTrue(Color.blue(pixel) > 200)
    }

    /**
     * Helper to create a Compose ImageBitmap for testing
     */
    private fun createSolidImageBitmap(w: Int, h: Int, color: Int): ImageBitmap {
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(color)
        return bitmap.asImageBitmap()
    }
}
