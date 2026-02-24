package com.mrkongtk.rtspviewer.shared.data.database.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.mrkongtk.rtspviewer.shared.data.database.AppDatabase
import com.mrkongtk.rtspviewer.shared.data.database.entity.RTSPItem
import com.mrkongtk.rtspviewer.shared.data.database.entity.RTSPItemOrderUpdate
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RTSPItemDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: RTSPItemDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        // Using an in-memory database because the information is saved in RAM
        // and disappears when the process is killed.
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        dao = db.rtspItemDao()
    }

    @After
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
            RTSPItem(1, "B", "url", emptyList(), 2), // Higher order
            RTSPItem(2, "A", "url", emptyList(), 1), // Lower order
            RTSPItem(3, "C", "url", emptyList(), 1)  // Same order as A, but name is C
        )
        dao.insertAll(items)

        // Should return A then C because order is 1, then B because order is 2
        val result = dao.getItems(offset = 0, limit = 10)
        assertEquals(2L, result[0].id) // A
        assertEquals(3L, result[1].id) // C
        assertEquals(1L, result[2].id) // B
    }

    @Test
    fun testPartialOrderUpdate() = runTest {
        val item = RTSPItem(1, "Office", "rtsp://url", emptyList(), 0)
        dao.insert(item)

        // Update only the order
        dao.updateOrders(listOf(RTSPItemOrderUpdate(id = 1, order = 99)))

        val updatedItem = dao.getItems(limit = 1)[0]
        assertEquals(99, updatedItem.order)
        assertEquals("Office", updatedItem.name) // Name should remain unchanged
    }

    @Test
    fun testFlowUpdates() = runTest {
        dao.getAllItemsFlow().test {
            // Initially empty
            assertEquals(0, awaitItem().size)

            val item = RTSPItem(1, "Office", "rtsp://url", emptyList(), 0)
            dao.insert(item)

            // Flow should emit new list
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
