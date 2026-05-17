package com.mrkongtk.rtspviewer.shared.data.repository

import androidx.room.InvalidationTracker
import com.mrkongtk.rtspviewer.shared.data.database.AppDatabase
import com.mrkongtk.rtspviewer.shared.data.database.dao.RTSPItemDao

class RTSPItemRepositoryImplIosTest : RTSPItemRepositoryImplTest() {

    private val mockDb = object : AppDatabase() {
        override fun rtspItemDao(): RTSPItemDao = fakeDao
        override fun createInvalidationTracker(): InvalidationTracker {
            TODO("Not yet implemented")
        }

    }

    override fun getDatabaseBuilder(): AppDatabase {
        return mockDb
    }
}
