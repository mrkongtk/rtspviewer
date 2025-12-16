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

    /**
     * Asynchronously updates the order of the RTSP items in the data source.
     *
     * This is typically used to persist changes after a user has rearranged items
     * in the UI (e.g., via drag-and-drop). The order in the database will be updated
     * to match the sequence of the provided list.
     *
     * @param items The list of [RTSPItem]s sorted in the new desired order.
     * @return The number of items successfully updated (rows affected).
     */
    suspend fun reorderItems(items: List<RTSPItem>): Int

    /**
     * Asynchronously updates the details of an existing [RTSPItem].
     *
     * This should be called when the user modifies the properties of a stream
     * (e.g., renaming the stream or changing the RTSP URL).
     *
     * @param item The item with updated values. It must contain the correct ID to map to the existing record.
     * @return The number of rows affected (usually 1 if successful, 0 otherwise).
     */
    suspend fun updateItem(item: RTSPItem): Int

    /**
     * Asynchronously removes an [RTSPItem] from the data source.
     *
     * @param item The RTSP stream item to be deleted.
     * @return The number of rows affected (usually 1 if successful, 0 otherwise).
     */
    suspend fun deleteItem(item: RTSPItem): Int
}
