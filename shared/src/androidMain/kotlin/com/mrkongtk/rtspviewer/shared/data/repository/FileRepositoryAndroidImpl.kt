package com.mrkongtk.rtspviewer.shared.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toOkioPath
import kotlin.random.Random

/**
 * Android implementation of [FileRepository] using Okio for file operations.
 *
 * This implementation handles image persistence by bridging Jetpack Compose [ImageBitmap]
 * and Android's native [Bitmap] compression.
 */
class FileRepositoryAndroidImpl(
    private val context: Context,
) : FileRepository {

    private val fileSystem = FileSystem.SYSTEM

    override fun getCacheDir(): Path = context.cacheDir.toOkioPath()

    /**
     * Persists an [ImageBitmap] as a JPEG file.
     *
     * Uses an atomic write strategy: data is first written to a temporary file and then
     * moved to the target destination. This prevents file corruption if the process
     * is interrupted during writing.
     *
     * @param file The target destination path.
     * @param bitmap The image to save.
     * @return True if the save was successful, false otherwise.
     */
    override suspend fun writeJPEG(file: Path, bitmap: ImageBitmap): Boolean {
        return withContext(Dispatchers.IO) {
            // Generate a unique temp file to avoid collisions during concurrent writes
            val tempPath =
                getCacheDir() / "${System.currentTimeMillis()}_${Random.nextInt(1000, 10000)}.tmp"

            try {
                val androidBitmap = bitmap.asAndroidBitmap()

                // Stage 1: Write to temporary file to ensure data integrity
                fileSystem.write(tempPath) {
                    outputStream().use { os ->
                        androidBitmap.compress(Bitmap.CompressFormat.JPEG, 85, os)
                    }
                }

                // Stage 2: Atomic move to destination (replaces existing file if present)
                fileSystem.atomicMove(tempPath, file)
                true
            } catch (e: Exception) {
                e.printStackTrace()
                // Cleanup partial temp file on failure
                if (fileSystem.exists(tempPath)) fileSystem.delete(tempPath)
                false
            }
        }
    }

    /**
     * Reads a JPEG file and decodes it into an [ImageBitmap].
     *
     * @param file The path to the JPEG file.
     * @return The decoded [ImageBitmap], or null if the file does not exist or is corrupted.
     */
    override suspend fun readJPEG(file: Path): ImageBitmap? {
        return withContext(Dispatchers.IO) {
            try {
                if (fileSystem.exists(file)) {
                    BitmapFactory.decodeFile(file.toString())?.asImageBitmap()
                } else null
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }
}
