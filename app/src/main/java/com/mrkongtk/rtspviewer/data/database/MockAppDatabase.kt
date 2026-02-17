package com.mrkongtk.rtspviewer.data.database

import androidx.room.InvalidationTracker
import com.mrkongtk.rtspviewer.shared.data.database.dao.RTSPItemDao
import com.mrkongtk.rtspviewer.shared.data.database.entity.RTSPItem
import com.mrkongtk.rtspviewer.shared.data.database.entity.RTSPItemOrderUpdate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow

/**
 * A Test Double (Mock) implementation of the [AppDatabase].
 *
 * PURPOSE:
 * This class is designed exclusively for Android Studio Compose Previews and UI testing.
 * It allows the UI layer to be rendered without initializing a full SQLite environment,
 * which would otherwise crash the Preview renderer.
 *
 * BEHAVIOR:
 * - Provides "No-Op" (No Operation) implementations of DAOs.
 * - Returns empty flows or default values to satisfy ViewModel requirements.
 * - Methods that modify state (insert/delete) perform no action to remain side-effect free.
 *
 * NOTE: This should never be used in the production source set (main).
 */
class MockAppDatabase : AppDatabase() {

    private val rtspItemDao = object : RTSPItemDao {
        override suspend fun insert(item: RTSPItem): Long {
            return 0
        }

        override suspend fun insertAll(items: List<RTSPItem>): List<Long> {
            return emptyList()
        }

        override suspend fun getItems(
            offset: Long,
            limit: Long
        ): List<RTSPItem> {
            return emptyList()
        }

        override fun getAllItemsFlow(): Flow<List<RTSPItem>> {
            return emptyList<List<RTSPItem>>().asFlow()
        }

        override suspend fun update(item: RTSPItem): Int {
            return 0
        }

        override suspend fun updateOrders(updates: List<RTSPItemOrderUpdate>): Int {
            return 0
        }

        override suspend fun delete(item: RTSPItem): Int {
            return 0
        }

    }

    override fun rtspItemDao(): RTSPItemDao {
        return rtspItemDao
    }

    override fun createInvalidationTracker(): InvalidationTracker {
        throw Exception("mock InvalidationTracker")
    }

    override fun clearAllTables() {
    }
}
