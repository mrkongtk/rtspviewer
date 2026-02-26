package com.mrkongtk.rtspviewer.shared.data.database

import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

interface DatabaseBuilder {
    fun getBuilder(): RoomDatabase.Builder<AppDatabase>
}

fun DatabaseBuilder.getDatabase(): AppDatabase {
    val builder = getBuilder()
    return builder
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
}
