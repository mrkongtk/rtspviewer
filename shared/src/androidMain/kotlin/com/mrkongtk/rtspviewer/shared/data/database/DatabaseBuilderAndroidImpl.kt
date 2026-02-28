package com.mrkongtk.rtspviewer.shared.data.database

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import com.mrkongtk.rtspviewer.shared.data.DeviceInfo
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Android-specific database configuration.
 * Stores the database in the standard system database path.
 */
class DatabaseBuilderAndroidImpl(private val context: Context): DatabaseBuilder, KoinComponent {

    private val deviceInfo: DeviceInfo by inject()

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
