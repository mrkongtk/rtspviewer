package com.mrkongtk.rtspviewer.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.mrkongtk.rtspviewer.data.database.dao.RTSPItemDao
import com.mrkongtk.rtspviewer.data.database.entity.RTSPItem
import java.net.URLDecoder
import java.net.URLEncoder

/**
 * The main Room Database class for the application.
 *
 * This abstract class serves as the primary access point to the persisted data.
 * It defines the database configuration and serves as the bridge between
 * the SQLite database and the DAO (Data Access Object) classes.
 */
@Database(
    entities = [
        RTSPItem::class // Specifies the tables (entities) included in this database
    ],
    version = 1, // The current version of the database schema
    exportSchema = true, // Exports schema to a JSON file (useful for version control/migrations)
    autoMigrations = [
        // Define auto-migrations here when bumping the version number
    ]
)
@TypeConverters(ListStringConverters::class) // Registers converters to handle custom data types
abstract class AppDatabase : RoomDatabase() {

    /**
     * Provides access to the [RTSPItemDao].
     *
     * @return The DAO for performing CRUD operations on the rtsp_item table.
     */
    abstract fun rtspItemDao(): RTSPItemDao
}

/**
 * Type Converters to map complex objects to types supported by SQLite.
 *
 * Room/SQLite does not natively support storing lists. This class handles
 * converting a [List] of Strings into a single String (and vice versa)
 * so it can be stored in a text column.
 */
class ListStringConverters {

    /**
     * Converts a stored String from the database back into a List<String>.
     *
     * @param value The comma-separated string retrieved from the database.
     * @return A list of decoded strings.
     */
    @TypeConverter
    fun fromString(value: String): List<String> {
        // Split the string by commas and decode each item to restore special characters
        return value.split(",").map { URLDecoder.decode(it, Charsets.UTF_8.name()) }
    }

    /**
     * Converts a List<String> into a single String for database storage.
     *
     * It uses URL encoding to ensure that if the original string contains commas
     * or special characters, they don't break the separation logic.
     *
     * @param list The list of strings to store.
     * @return A single comma-separated, URL-encoded string.
     */
    @TypeConverter
    fun fromList(list: List<String>): String {
        // Encode each item to UTF-8 to handle special chars, then join them with commas
        return list.joinToString(",", transform = { URLEncoder.encode(it, Charsets.UTF_8.name()) })
    }
}
