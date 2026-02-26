package com.mrkongtk.rtspviewer.shared.data.database

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import com.mrkongtk.rtspviewer.shared.data.DeviceInfo
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class DatabaseBuilderAndroidImpl(private val context: Context): DatabaseBuilder, KoinComponent {

    private val deviceInfo: DeviceInfo by inject()

    override fun getBuilder(): RoomDatabase.Builder<AppDatabase> {
        val appContext = context.applicationContext
        val dbFile = appContext.getDatabasePath("db_${deviceInfo.bundleId}")
        return Room.databaseBuilder<AppDatabase>(
            context = appContext,
            name = dbFile.absolutePath
        )
    }

}