package com.mrkongtk.rtspviewer.shared.data.repository

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageBitmapConfig
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import app.cash.turbine.test
import com.mrkongtk.rtspviewer.shared.data.database.AppDatabase
import com.mrkongtk.rtspviewer.shared.data.database.dao.RTSPItemDao
import com.mrkongtk.rtspviewer.shared.data.database.entity.RTSPItem
import com.mrkongtk.rtspviewer.shared.data.database.entity.RTSPItemOrderUpdate
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.runTest
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

abstract class RTSPItemRepositoryImplTest {

    private lateinit var repository: RTSPItemRepositoryImpl
    protected lateinit var fakeDao: FakeRTSPItemDao
    private lateinit var fakeFileRepository: FakeFileRepository

    // Using a simple object to represent a bitmap in common code if actual
    // ImageBitmap creation is difficult in tests
    private val mockBitmap = object : ImageBitmap {
        override val config get() = ImageBitmapConfig.Argb8888
        override val hasAlpha get() = true
        override val height get() = 1
        override val colorSpace get() = ColorSpaces.AdobeRgb
        override val width get() = 1
        override fun prepareToDraw() {}
        override fun readPixels(
            buffer: IntArray,
            startX: Int,
            startY: Int,
            width: Int,
            height: Int,
            bufferOffset: Int,
            stride: Int
        ) {
        }
    }

    abstract fun getDatabaseBuilder(): AppDatabase

    @BeforeTest
    fun setup() {
        fakeDao = FakeRTSPItemDao()
        fakeFileRepository = FakeFileRepository()

        // Mock the AppDatabase to return our fake DAO
        val mockDb = getDatabaseBuilder()

        // Setup Koin for the test environment since the Impl uses 'by inject()'
        startKoin {
            modules(module {
                single<AppDatabase> { mockDb }
                single<FileRepository> { fakeFileRepository }
            })
        }

        repository = RTSPItemRepositoryImpl(fakeFileRepository, mockDb)
    }

    @AfterTest
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun addItem_sanitizes_tags() = runTest {
        val inputItem = RTSPItem(
            id = 0,
            name = "Test Camera",
            uri = "rtsp://link",
            tags = listOf(" outdoor ", "", "HD", "HD "),
            order = 0
        )

        repository.addItem(inputItem)

        val savedItem = fakeDao.insertedItems.first()
        assertEquals(2, savedItem.tags.size)
        assertTrue(savedItem.tags.contains("outdoor"))
        assertTrue(savedItem.tags.contains("HD"))
    }

    @Test
    fun items_flow_automatically_triggers_file_reading() = runTest {
        val item = RTSPItem(1L, "Cam", "uri", emptyList(), 0)
        fakeFileRepository.bitmapToReturn = mockBitmap

        repository.items.test {
            fakeDao.emit(listOf(item))
            val emittedList = awaitItem()

            assertEquals(1, emittedList.size)
            // Verify it was added to the memory cache
            assertEquals(mockBitmap, repository.cachedPreviews.value[1L])
        }
    }

    @Test
    fun previewPathFor_returns_correct_path_format() {
        val item = RTSPItem(555L, "Cam", "uri", emptyList(), 0)
        val path = repository.previewPathFor(item)

        assertTrue(path.toString().contains("preview_555.jpg"))
    }

    @Test
    fun cachePreviewFor_updates_state_flow() {
        val item = RTSPItem(1L, "Cam", "uri", emptyList(), 0)

        repository.cachePreviewFor(item, mockBitmap)
        assertEquals(mockBitmap, repository.cachedPreviews.value[1L])
    }

    @Test
    fun removeCachedPreviews_clears_map() {
        val item1 = RTSPItem(1L, "C1", "u", emptyList(), 0)
        repository.cachePreviewFor(item1, mockBitmap)

        repository.removeCachedPreviews()
        assertTrue(repository.cachedPreviews.value.isEmpty())
    }

    class FakeRTSPItemDao : RTSPItemDao {
        val insertedItems = mutableListOf<RTSPItem>()
        val updatedItems = mutableListOf<RTSPItem>()
        val deletedItems = mutableListOf<RTSPItem>()
        var orderUpdates = listOf<RTSPItemOrderUpdate>()

        private val flow = MutableSharedFlow<List<RTSPItem>>()
        suspend fun emit(list: List<RTSPItem>) = flow.emit(list)

        override suspend fun insert(item: RTSPItem): Long {
            insertedItems.add(item)
            return 1L
        }

        override fun getAllItemsFlow() = flow
        override suspend fun update(item: RTSPItem): Int {
            updatedItems.add(item)
            return 1
        }

        override suspend fun updateOrders(updates: List<RTSPItemOrderUpdate>): Int {
            orderUpdates = updates
            return updates.size
        }

        override suspend fun delete(item: RTSPItem): Int {
            deletedItems.add(item)
            return 1
        }

        // Implement others as no-ops...
        override suspend fun insertAll(items: List<RTSPItem>) = emptyList<Long>()
        override suspend fun getItems(offset: Long, limit: Long) = emptyList<RTSPItem>()
    }

    class FakeFileRepository : FileRepository {
        var readCount = 0
        var lastReadPath: okio.Path? = null
        var bitmapToReturn: ImageBitmap? = null

        override suspend fun readJPEG(file: okio.Path): ImageBitmap? {
            readCount++
            lastReadPath = file
            return bitmapToReturn
        }

        override suspend fun writeJPEG(file: okio.Path, bitmap: ImageBitmap) = true
        override fun getCacheDir(): okio.Path = okio.FileSystem.SYSTEM_TEMPORARY_DIRECTORY
    }
}
