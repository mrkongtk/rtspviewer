package com.mrkongtk.rtspviewer.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.mrkongtk.rtspviewer.data.database.entity.RTSPItem
import com.mrkongtk.rtspviewer.data.database.entity.RTSPItemOrderUpdate
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) for managing the [RTSPItem] entity.
 *
 * This interface defines the standard database interactions (CRUD)
 * for the 'rtsp_item' table, including pagination and bulk updates.
 */
@Dao
interface RTSPItemDao {

    /**
     * Inserts a single [RTSPItem] into the database.
     *
     * If an item with the same primary key already exists, the existing
     * item will be overwritten by the new one ([OnConflictStrategy.REPLACE]).
     *
     * @param item The RTSP item to be inserted.
     * @return The row ID of the newly inserted item.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: RTSPItem): Long

    /**
     * Inserts a list of [RTSPItem]s into the database efficiently.
     *
     * Geared towards bulk operations. Uses [OnConflictStrategy.REPLACE]
     * to handle duplicate primary keys.
     *
     * @param items The list of RTSP items to insert.
     * @return A list of row IDs for the inserted items.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<RTSPItem>): List<Long>

    /**
     * Retrieves a paginated list of [RTSPItem]s from the database.
     *
     * Results are sorted primarily by the custom `order` column (ascending),
     * and secondarily by `name` (alphabetically).
     *
     * @param offset The zero-based index of the first item to return (skip count). Defaults to 0.
     * @param limit The maximum number of items to return. Defaults to 10.
     * @return A list of [RTSPItem]s for the requested page.
     */
    @Query("SELECT * FROM rtsp_item ORDER BY `order` ASC, `name` ASC LIMIT :limit OFFSET :offset")
    suspend fun getItems(offset: Long = 0, limit: Long = 10): List<RTSPItem>

    @Query("SELECT * FROM rtsp_item ORDER BY `order` ASC")
    fun getAllItemsFlow(): Flow<List<RTSPItem>> // Room handles the background thread

    /**
     * Updates an existing [RTSPItem] in the database.
     *
     * Room matches the item against database entries using its Primary Key.
     * All fields in the database row will be overwritten with the values from [item].
     *
     * @param item The RTSPItem with updated data to be persisted.
     * @return The number of rows affected (usually 1 if successful, 0 if not found).
     */
    @Update
    suspend fun update(item: RTSPItem): Int

    /**
     * Batch updates the sort order of multiple items.
     *
     * This uses a **partial update** via the [RTSPItemOrderUpdate] entity to modify
     * only the specific ordering columns. This is significantly more efficient than
     * full updates when reordering a list (e.g., via drag-and-drop).
     *
     * @param updates A list of partial entities containing the ID and the new order.
     * @return The total number of rows successfully updated.
     */
    @Update(entity = RTSPItem::class)
    suspend fun updateOrders(updates: List<RTSPItemOrderUpdate>): Int

    /**
     * Deletes a specific [RTSPItem] from the database.
     *
     * Room locates the item to delete based on its Primary Key.
     *
     * @param item The RTSPItem object to remove.
     * @return The number of rows deleted (usually 1 if successful).
     */
    @Delete
    suspend fun delete(item: RTSPItem): Int

}
