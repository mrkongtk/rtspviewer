package com.mrkongtk.rtspviewer.shared.data.database.dao

import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import app.cash.turbine.test
import com.mrkongtk.rtspviewer.shared.data.database.AppDatabase
import com.mrkongtk.rtspviewer.shared.data.database.entity.RTSPItem
import com.mrkongtk.rtspviewer.shared.data.database.entity.RTSPItemOrderUpdate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

abstract class RTSPItemDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: RTSPItemDao

    abstract fun getDatabaseBuilder(): RoomDatabase.Builder<AppDatabase>

    @BeforeTest
    fun createDb() {
        // Use the platform-specific builder
        val builder = getDatabaseBuilder()

        // Use BundledSQLiteDriver for KMM in-memory testing
        db = builder
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.Unconfined)
            .build()

        dao = db.rtspItemDao()
    }

    @AfterTest
    fun closeDb() {
        db.close()
    }

    @Test
    fun insertAndGetItem() = runTest {
        val item = RTSPItem(1, "Office", "rtsp://url", listOf("work"), 0)
        dao.insert(item)

        val items = dao.getItems(limit = 1)
        assertEquals(1, items.size)
        assertEquals("Office", items[0].name)
    }

    @Test
    fun testPaginationAndSorting() = runTest {
        val items = listOf(
            RTSPItem(1, "B", "url", emptyList(), 2),
            RTSPItem(2, "A", "url", emptyList(), 1),
            RTSPItem(3, "C", "url", emptyList(), 1)
        )
        dao.insertAll(items)

        val result = dao.getItems(offset = 0, limit = 10)
        assertEquals(2L, result[0].id) // A
        assertEquals(3L, result[1].id) // C
        assertEquals(1L, result[2].id) // B
    }

    @Test
    fun testPartialOrderUpdate() = runTest {
        val item = RTSPItem(1, "Office", "rtsp://url", emptyList(), 0)
        dao.insert(item)

        dao.updateOrders(listOf(RTSPItemOrderUpdate(id = 1, order = 99)))

        val updatedItem = dao.getItems(limit = 1)[0]
        assertEquals(99, updatedItem.order)
        assertEquals("Office", updatedItem.name)
    }

    @Test
    fun testFlowUpdates() = runTest {
        dao.getAllItemsFlow().test {
            assertEquals(0, awaitItem().size)

            val item = RTSPItem(1, "Office", "rtsp://url", emptyList(), 0)
            dao.insert(item)

            val list = awaitItem()
            assertEquals(1, list.size)
            assertEquals("Office", list[0].name)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun testDelete() = runTest {
        val item = RTSPItem(1, "Office", "rtsp://url", emptyList(), 0)
        dao.insert(item)
        dao.delete(item)

        val items = dao.getItems()
        assertEquals(0, items.size)
    }
}
