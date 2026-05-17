package com.mrkongtk.rtspviewer.shared.data.repository

import androidx.room.InvalidationTracker
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mrkongtk.rtspviewer.shared.data.database.AppDatabase
import com.mrkongtk.rtspviewer.shared.data.database.dao.RTSPItemDao
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RTSPItemRepositoryImplAndroidTest : RTSPItemRepositoryImplTest() {

    private val mockDb = object : AppDatabase() {
        override fun rtspItemDao(): RTSPItemDao = fakeDao
        override fun createInvalidationTracker(): InvalidationTracker {
            TODO("Not yet implemented")
        }

        override fun clearAllTables() {
            TODO("Not yet implemented")
        }
    }

    override fun getDatabaseBuilder(): AppDatabase {
        return mockDb
    }
}
