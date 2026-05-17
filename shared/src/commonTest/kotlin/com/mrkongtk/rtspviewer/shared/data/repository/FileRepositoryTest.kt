package com.mrkongtk.rtspviewer.shared.data.repository

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.coroutines.test.runTest
import okio.FileSystem
import okio.Path
import okio.SYSTEM
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

abstract class FileRepositoryTest {

    private val fileSystem = FileSystem.SYSTEM
    lateinit var repository: FileRepository
    private lateinit var testDir: Path

    // Platform-specific way to create a test bitmap
    abstract fun createSolidImageBitmap(w: Int, h: Int, color: Color): ImageBitmap

    @BeforeTest
    fun setUp() {
        repository = createRepository()
        testDir = repository.getCacheDir() / "test_images"
        if (!fileSystem.exists(testDir)) {
            fileSystem.createDirectories(testDir)
        }
    }

    @AfterTest
    fun tearDown() {
        if (fileSystem.exists(testDir)) {
            fileSystem.deleteRecursively(testDir)
        }
    }

    abstract fun createRepository(): FileRepository

    @Test
    fun writeJPEG_savesImageBitmapToFile_returnsTrue() = runTest {
        val bitmap = createSolidImageBitmap(100, 100, Color.Red)
        val destFile = testDir / "test_save.jpg"

        val result = repository.writeJPEG(destFile, bitmap)

        assertTrue(result, "Function should return true on success")
        assertTrue(fileSystem.exists(destFile), "File should exist on disk")
        assertTrue(fileSystem.metadata(destFile).size!! > 0)
    }

    @Test
    fun readJPEG_validFile_returnsImageBitmap() = runTest {
        val destFile = testDir / "read_test.jpg"
        val originalBitmap = createSolidImageBitmap(50, 50, Color.Green)
        repository.writeJPEG(destFile, originalBitmap)

        val resultBitmap = repository.readJPEG(destFile)

        assertNotNull(resultBitmap, "Should return an ImageBitmap")
        assertEquals(50, resultBitmap.width)
        assertEquals(50, resultBitmap.height)
    }

    @Test
    fun readJPEG_nonExistentFile_returnsNull() = runTest {
        val missingFile = testDir / "ghost.jpg"
        val result = repository.readJPEG(missingFile)
        assertNull(result)
    }

    @Test
    fun readJPEG_corruptFile_returnsNull() = runTest {
        val corruptFile = testDir / "not_an_image.txt"
        fileSystem.write(corruptFile) { writeUtf8("Not a JPEG") }

        val result = repository.readJPEG(corruptFile)
        assertNull(result)
    }
}
