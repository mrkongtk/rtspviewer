package com.mrkongtk.rtspviewer.data.repository

import com.mrkongtk.rtspviewer.data.database.AppDatabase
import com.mrkongtk.rtspviewer.data.database.entity.RTSPItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/**
 * Concrete implementation of [RTSPItemRepository].
 *
 * This repository is responsible for managing data operations for RTSP items,
 * acting as a mediator between the database (Room) and the UI/Domain layer.
 * It utilizes [StateFlow] to provide a reactive stream of data updates.
 *
 * @property db The Room database instance injected via Dependency Injection.
 */
class RTSPItemRepositoryImpl @Inject constructor(
    private val db: AppDatabase,
) : RTSPItemRepository {

    // Internal mutable state flow to handle list updates
    private val _items = MutableStateFlow<List<RTSPItem>>(emptyList())

    /**
     * A read-only [StateFlow] observing the list of [RTSPItem]s.
     * UI components should collect from this flow to receive real-time data updates.
     */
    override val items: StateFlow<List<RTSPItem>> = _items.asStateFlow()

    /**
     * Fetches the complete list of RTSP items from the database and updates
     * the [_items] StateFlow.
     *
     * This method retrieves items from index 0 to [Long.MAX_VALUE].
     */
    override suspend fun loadData() {
        _items.update { _ ->
            db.rtspItemDao().getItems(0, Long.MAX_VALUE)
        }
    }

    /**
     * Inserts a new [RTSPItem] into the database.
     *
     * @param item The RTSP item entity to be added.
     * @return The row ID of the newly inserted item.
     */
    override suspend fun addItem(item: RTSPItem): Long {
        val insertedId = db.rtspItemDao().insert(item)
        return insertedId
    }

}
