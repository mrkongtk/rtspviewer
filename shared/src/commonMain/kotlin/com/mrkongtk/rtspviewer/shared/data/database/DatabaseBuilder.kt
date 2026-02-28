package com.mrkongtk.rtspviewer.shared.data.database

import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

/**
 * Interface defines the contract for creating a Room Database builder
 * based on platform-specific storage locations.
 */
interface DatabaseBuilder {
    fun getBuilder(): RoomDatabase.Builder<AppDatabase>
}

/**
 * Extension function to finalize the database configuration.
 * Uses BundledSQLiteDriver for cross-platform compatibility.
 */
fun DatabaseBuilder.getDatabase(): AppDatabase {
    return getBuilder()
        .setDriver(BundledSQLiteDriver()) // Use bundled SQLite instead of system SQLite
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
}
