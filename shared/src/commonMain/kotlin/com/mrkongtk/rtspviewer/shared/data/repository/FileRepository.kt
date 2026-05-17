package com.mrkongtk.rtspviewer.shared.data.repository

import androidx.compose.ui.graphics.ImageBitmap
import okio.Path

/**
 * Interface defining operations for file storage handling.
 * Specifically focuses on reading and writing JPEG images using [Path] and [ImageBitmap].
 */
interface FileRepository {

    /**
     * Asynchronously writes an [ImageBitmap] to a specific [Path] as a JPEG.
     *
     * @param file The destination [Path] where the image should be saved.
     * @param bitmap The source [ImageBitmap] to save.
     * @return [Boolean] True if the write operation was successful, false otherwise.
     */
    suspend fun writeJPEG(file: Path, bitmap: ImageBitmap): Boolean

    /**
     * Asynchronously reads a JPEG image from a [Path] and converts it to an [ImageBitmap].
     *
     * @param file The source [Path] to read from.
     * @return [ImageBitmap]? The decoded image bitmap, or null if the file does not exist or decoding fails.
     */
    suspend fun readJPEG(file: Path): ImageBitmap?

    /**
     * Returns the [Path] to the cache directory.
     *
     * @return The cache directory [Path].
     */
    fun getCacheDir(): Path
}
