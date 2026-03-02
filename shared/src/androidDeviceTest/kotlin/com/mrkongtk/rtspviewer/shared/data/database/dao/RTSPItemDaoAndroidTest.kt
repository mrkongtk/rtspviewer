package com.mrkongtk.rtspviewer.shared.data.database.dao

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mrkongtk.rtspviewer.shared.data.database.AppDatabase
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RTSPItemDaoAndroidTest : RTSPItemDaoTest() {

    override fun getDatabaseBuilder(): RoomDatabase.Builder<AppDatabase> {
        val context = ApplicationProvider.getApplicationContext<Context>()
        return Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
    }
}
