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

class DatabaseBuilderIosImpl : DatabaseBuilder, KoinComponent {

    private val deviceInfo: DeviceInfo by inject()

    override fun getBuilder(): RoomDatabase.Builder<AppDatabase> {
        val dbFilePath = documentDirectory() + "/db_${deviceInfo.bundleId}.db"
        return Room.databaseBuilder<AppDatabase>(
            name = dbFilePath,
        )
    }

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

