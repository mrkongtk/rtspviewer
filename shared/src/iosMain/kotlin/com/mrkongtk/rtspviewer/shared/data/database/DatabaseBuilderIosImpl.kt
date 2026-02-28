package com.mrkongtk.rtspviewer.shared.data.database

import androidx.room.Room
import androidx.room.RoomDatabase
import com.mrkongtk.rtspviewer.shared.data.DeviceInfo
import kotlinx.cinterop.ExperimentalForeignApi
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

/**
 * iOS-specific database configuration.
 * Stores the database in the Application's Document Directory.
 */
class DatabaseBuilderIosImpl : DatabaseBuilder, KoinComponent {

    private val deviceInfo: DeviceInfo by inject()

    override fun getBuilder(): RoomDatabase.Builder<AppDatabase> {
        val dbFilePath = documentDirectory() + "/db_${deviceInfo.bundleId}.db"
        return Room.databaseBuilder<AppDatabase>(
            name = dbFilePath,
        )
    }

    /**
     * Resolves the standard iOS Documents directory path.
     */
    @OptIn(ExperimentalForeignApi::class)
    private fun documentDirectory(): String {
        val documentDirectory = NSFileManager.defaultManager.URLForDirectory(
            directory = NSDocumentDirectory,
            inDomain = NSUserDomainMask,
            appropriateForURL = null,
            create = false,
            error = null,
        )
        return requireNotNull(documentDirectory?.path)
    }
}
