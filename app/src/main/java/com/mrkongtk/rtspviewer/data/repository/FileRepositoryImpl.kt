package com.mrkongtk.rtspviewer.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.Calendar
import javax.inject.Inject
import kotlin.random.Random

/**
 * Concrete implementation of [FileRepository].
 *
 * This implementation utilizes [Dispatchers.IO] to perform disk operations off the main thread.
 * It implements an "Atomic Write" strategy for saving files to prevent data corruption.
 */
class FileRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : FileRepository {

    /**
     * Writes a JPEG using an atomic approach:
     * 1. Writes data to a temporary file in the cache directory.
     * 2. Moves the temporary file to the final destination atomically.
     *
     * This ensures that the destination file is never in a half-written or corrupted state.
     */
    override suspend fun writeJPEG(file: File, bitmap: Bitmap): Boolean {
        return withContext(Dispatchers.IO) {
            val cacheDir: File = context.cacheDir

            // Create a unique temporary file path in the cache directory
            // using current timestamp and a random integer to avoid collisions.
            val tempPath = File(
                cacheDir,
                "${Calendar.getInstance().timeInMillis}_${
                    Random.nextInt(
                        1000,
                        10000
                    )
                }.${file.extension}"
            )

            try {
                // Write the compressed bitmap data to the temporary file first
                val saveSuccess = BufferedOutputStream(FileOutputStream(tempPath)).use { bos ->
                    // Compress to JPEG with 85% quality
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 85, bos)
                }

                if (saveSuccess) {
                    // If writing to temp was successful, perform an atomic move to the final destination.
                    // REPLACE_EXISTING: Overwrites the destination if it exists.
                    // ATOMIC_MOVE: Ensures the move happens as a single operation (system dependent).
                    Files.move(
                        tempPath.toPath(),
                        file.toPath(),
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE
                    )
                    true
                } else {
                    false
                }
            } catch (e: IOException) {
                Log.e("FileRepositoryImpl", "write JPEG error", e)
                false
            }
        }
    }

    override suspend fun readJPEG(file: File): Bitmap? {
        return withContext(Dispatchers.IO) {
            // Verify file existence before attempting to decode
            if (file.exists() && file.isFile) {
                try {
                    BitmapFactory.decodeFile(file.absolutePath)
                } catch (e: IOException) {
                    Log.e("FileRepositoryImpl", "read JPEG error", e)
                    null
                }
            } else {
                null
            }
        }
    }
}
