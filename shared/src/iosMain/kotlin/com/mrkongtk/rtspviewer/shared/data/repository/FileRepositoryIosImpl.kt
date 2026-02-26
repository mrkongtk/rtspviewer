package com.mrkongtk.rtspviewer.shared.data.repository

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import org.jetbrains.skia.Image
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSDate
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask
import platform.Foundation.timeIntervalSince1970
import kotlin.random.Random

/**
 * iOS implementation of [FileRepository] using Okio for file operations
 * and Skia for image processing.
 */
class FileRepositoryIosImpl : FileRepository {

    private val fileSystem = FileSystem.SYSTEM

    /**
     * Retrieves the platform-standard Caches directory path.
     */
    override fun getCacheDir(): Path {
        val cacheDir = NSFileManager.defaultManager
            .URLsForDirectory(NSCachesDirectory, NSUserDomainMask)
            .first() as NSURL
        return cacheDir.path!!.toPath()
    }

    /**
     * Saves an [ImageBitmap] to the disk as a JPEG.
     *
     * Uses a "write-to-temp-then-move" strategy to ensure file integrity (atomic write).
     * This prevents partially written files if the process is interrupted.
     */
    override suspend fun writeJPEG(file: Path, bitmap: ImageBitmap): Boolean {
        return withContext(Dispatchers.IO) {
            // Generate a unique temporary path to avoid collisions during concurrent writes
            val tempPath = getCacheDir() / "${NSDate().timeIntervalSince1970}_${
                Random.nextInt(
                    1000,
                    10000
                )
            }.tmp"

            try {
                // Convert Compose ImageBitmap to Skia Image for encoding
                val skiaBitmap = bitmap.asSkiaBitmap()
                val skiaImage = Image.makeFromBitmap(skiaBitmap)

                // Encode to JPEG data with 85% quality
                val encodedData =
                    skiaImage.encodeToData(org.jetbrains.skia.EncodedImageFormat.JPEG, 85)

                encodedData?.bytes?.let { bytes ->
                    // Stage 1: Write data to a temporary file
                    fileSystem.write(tempPath) {
                        write(bytes)
                    }

                    // Stage 2: Move the temporary file to the final destination atomically
                    fileSystem.atomicMove(tempPath, file)
                    true
                } ?: false
            } catch (e: Exception) {
                e.printStackTrace()
                // Clean up the temporary file if an error occurred during writing
                if (fileSystem.exists(tempPath)) {
                    fileSystem.delete(tempPath)
                }
                false
            }
        }
    }

    /**
     * Reads a JPEG file from the given [file] path and converts it back to an [ImageBitmap].
     * Returns null if the file does not exist or decoding fails.
     */
    override suspend fun readJPEG(file: Path): ImageBitmap? {
        return withContext(Dispatchers.IO) {
            if (!fileSystem.exists(file)) {
                return@withContext null
            }

            try {
                val bytes = fileSystem.read(file) {
                    readByteArray()
                }

                // Decode raw bytes into a Skia Image and convert to Compose ImageBitmap
                val skiaImage = Image.makeFromEncoded(bytes)
                skiaImage.toComposeImageBitmap()
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }
}
