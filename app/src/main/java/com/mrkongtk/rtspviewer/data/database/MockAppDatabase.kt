package com.mrkongtk.rtspviewer.data.database

import androidx.room.InvalidationTracker
import com.mrkongtk.rtspviewer.data.database.dao.RTSPItemDao

/**
 * A mock implementation of [AppDatabase] intended primarily for UI Previews.
 *
 * This class satisfies dependency requirements for ViewModels or Composables
 * during preview rendering but does not support actual database operations.
 * Calling any method on this class will result in an [Exception].
 */
class MockAppDatabase : AppDatabase() {

    /**
     * Throws an exception as DAO access is not supported in mocks/previews.
     */
    override fun rtspItemDao(): RTSPItemDao {
        throw Exception("mock RTSPItemDao")
    }

    override fun createInvalidationTracker(): InvalidationTracker {
        throw Exception("mock InvalidationTracker")
    }

    override fun clearAllTables() {
        throw Exception("mock clearAllTables")
    }
}
