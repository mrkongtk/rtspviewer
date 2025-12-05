package com.mrkongtk.rtspviewer.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.mrkongtk.rtspviewer.data.database.entity.RTSPItem

/**
 * Data Access Object (DAO) for the [RTSPItem] entity.
 * This interface defines the database interactions for the 'rtsp_item' table.
 */
@Dao
interface RTSPItemDao {

    /**
     * Inserts a list of [RTSPItem]s into the database.
     *
     * If an item with the same primary key already exists, the old item
     * will be replaced by the new one (OnConflictStrategy.REPLACE).
     *
     * @param items The list of RTSP items to insert.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<RTSPItem>)

    /**
     * Retrieves a paginated list of [RTSPItem]s from the database.
     *
     * The results are sorted in ascending order based on the `order` column.
     *
     * @param offset The number of items to skip (used for pagination). Default is 0.
     * @param limit The maximum number of items to return. Default is 10.
     * @return A list of [RTSPItem]s.
     */
    @Query("SELECT * FROM rtsp_item ORDER BY `order` ASC LIMIT :limit OFFSET :offset")
    suspend fun getItems(offset: Long = 0, limit: Long = 10): List<RTSPItem>

    /**
     * Updates an existing [RTSPItem] in the database.
     * Room matches the item against the database by its Primary Key.
     *
     * @param user The RTSPItem with updated data to be saved.
     */
    @Update
    suspend fun update(user: RTSPItem)

    /**
     * Deletes a specific [RTSPItem] from the database.
     * Room finds the item to delete based on its Primary Key.
     *
     * @param user The RTSPItem to remove.
     */
    @Delete
    suspend fun delete(user: RTSPItem)
}
