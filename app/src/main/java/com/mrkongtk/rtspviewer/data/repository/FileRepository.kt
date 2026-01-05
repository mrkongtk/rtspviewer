package com.mrkongtk.rtspviewer.data.repository

import android.graphics.Bitmap
import java.io.File

/**
 * Interface defining operations for file storage handling.
 * Specifically focuses on reading and writing JPEG images.
 */
interface FileRepository {

    /**
     * Asynchronously writes a [Bitmap] to a specific [File] as a JPEG.
     *
     * @param file The destination file where the image should be saved.
     * @param bitmap The source bitmap image to save.
     * @return [Boolean] True if the write operation and file move were successful, false otherwise.
     */
    suspend fun writeJPEG(file: File, bitmap: Bitmap): Boolean

    /**
     * Asynchronously reads a JPEG image from a [File] and converts it to a [Bitmap].
     *
     * @param file The source file to read from.
     * @return [Bitmap]? The decoded bitmap, or null if the file does not exist or decoding fails.
     */
    suspend fun readJPEG(file: File): Bitmap?
}
