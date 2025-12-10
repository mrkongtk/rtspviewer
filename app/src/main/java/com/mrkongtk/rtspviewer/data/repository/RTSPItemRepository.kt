package com.mrkongtk.rtspviewer.data.repository

import com.mrkongtk.rtspviewer.data.database.entity.RTSPItem
import kotlinx.coroutines.flow.StateFlow

/**
 * Defines the contract for the repository responsible for managing RTSP stream items.
 *
 * This repository acts as the single source of truth for the application's data layer,
 * abstracting the underlying data sources (such as a local database or network).
 */
interface RTSPItemRepository {

    /**
     * A state holder exposing the current list of [RTSPItem]s.
     *
     * This StateFlow allows observers (like ViewModels or UI components) to reactively
     * update whenever the data source changes. It always holds the latest state.
     */
    val items: StateFlow<List<RTSPItem>>

    /**
     * Asynchronously loads the RTSP items from the underlying data source.
     *
     * This function should be called to initiate data fetching or to refresh the content
     * contained in [items].
     */
    suspend fun loadData()

    /**
     * Asynchronously adds a new [RTSPItem] to the data source.
     *
     * @param item The RTSP stream item to be persisted.
     * @return The unique identifier (typically the row ID) of the newly inserted item.
     */
    suspend fun addItem(item: RTSPItem): Long
}
