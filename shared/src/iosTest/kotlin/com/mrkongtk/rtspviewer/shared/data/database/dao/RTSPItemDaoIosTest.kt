package com.mrkongtk.rtspviewer.shared.data.database.dao

import androidx.room.Room
import androidx.room.RoomDatabase
import com.mrkongtk.rtspviewer.shared.data.database.AppDatabase

class RTSPItemDaoIosTest : RTSPItemDaoTest() {

    override fun getDatabaseBuilder(): RoomDatabase.Builder<AppDatabase> {
        return Room.inMemoryDatabaseBuilder<AppDatabase>()
    }
}
