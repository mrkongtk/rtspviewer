package com.mrkongtk.rtspviewer.shared.data.database

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import com.mrkongtk.rtspviewer.shared.data.DeviceInfo

/**
 * Android-specific database configuration.
 * Stores the database in the standard system database path.
 */
class DatabaseBuilderAndroidImpl(
    private val context: Context,
    private val deviceInfo: DeviceInfo
) : DatabaseBuilder {

    override fun getBuilder(): RoomDatabase.Builder<AppDatabase> {
        val appContext = context.applicationContext
        // Construct the path using the application's internal database directory
        val dbFile = appContext.getDatabasePath("db_${deviceInfo.bundleId}")

        return Room.databaseBuilder<AppDatabase>(
            context = appContext,
            name = dbFile.absolutePath
        )
    }
}
