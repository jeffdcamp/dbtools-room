package org.dbtools.android.room.jdbc

import android.app.Application
import androidx.room.Room
import assertk.assertThat
import assertk.assertions.isEqualTo
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okio.FileSystem
import okio.Path.Companion.toPath
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.concurrent.Executors

class JdbcReadWriteTest {
    private lateinit var itemDatabase: ItemDatabase

    @BeforeEach
    fun setUp() {
        val fileSystem = FileSystem.SYSTEM
        val databaseFile = "build/room-test-filesystem/database/jdbc-readwrite.db".toPath()
        fileSystem.delete(databaseFile)

        itemDatabase = Room.databaseBuilder(mockk<Application>(), ItemDatabase::class.java, databaseFile.name)
            .allowMainThreadQueries()
            .setTransactionExecutor(Executors.newSingleThreadExecutor())
            .openHelperFactory(JdbcSQLiteOpenHelperFactory(databaseFile.toFile().parentFile.path, enableJdbcTransactionSupport = false))
//            .setQueryCallback({ sql, args -> println("Query: [$sql]  Args: $args") }) { it.run() }
            .build()
    }

    @Test
    fun `single insert`() = runTest {
        val itemDao = itemDatabase.itemDao()

        itemDao.insert(Item("1", "Name1"))
        assertThat(itemDao.findCount()).isEqualTo(1)
    }

    @Test
    fun `auto-gen single insert`() = runTest {
        val autoGenItemDao = itemDatabase.autoGentItemDao()

        autoGenItemDao.insert(AutoGenItem(name = "Name1"))
        assertThat(autoGenItemDao.findCount()).isEqualTo(1)
    }

    @Test
    fun `double insert`() = runTest {
        val itemDao = itemDatabase.itemDao()

        itemDao.insert(Item("1", "Name1"))
        itemDao.insert(Item("2", "Name2"))
        assertThat(itemDao.findCount()).isEqualTo(2)
    }

    @Test
    fun `auto-gen double insert`() = runTest {
        val autoGenItemDao = itemDatabase.autoGentItemDao()

        autoGenItemDao.insert(AutoGenItem(name = "Name1"))
        autoGenItemDao.insert(AutoGenItem(name = "Name2"))
        assertThat(autoGenItemDao.findCount()).isEqualTo(2)
    }

    @Test
    fun `bulk insert`() = runTest {
        val itemDao = itemDatabase.itemDao()

        val items = listOf(
            Item("1", "Name1"),
            Item("2", "Name2"),
            Item("3", "Name3")
        )
        itemDao.insertAll(items)
        assertThat(itemDao.findCount()).isEqualTo(3)
    }

    @Test
    fun `auto-gen bulk insert`() = runTest {
        val autoGenItemDao = itemDatabase.autoGentItemDao()

        val items = listOf(
            AutoGenItem(name = "Name1"),
            AutoGenItem(name = "Name2"),
            AutoGenItem(name = "Name3"),
        )
        autoGenItemDao.insertAll(items)
        assertThat(autoGenItemDao.findCount()).isEqualTo(3)
    }

    @Test
    fun `single update`() = runTest {
        val itemDao = itemDatabase.itemDao()

        val item1 = Item(id = "1", name = "Name1")
        itemDao.insert(item1)

        assertThat(itemDao.findCount()).isEqualTo(1)
        assertThat(itemDao.findNameById(item1.id)).isEqualTo("Name1")

        val item1a = item1.copy(name = "Name1a")
        itemDao.update(item1a)

        assertThat(itemDao.findCount()).isEqualTo(1)
        assertThat(itemDao.findNameById(item1a.id)).isEqualTo("Name1a")
    }

    @Test
    fun `auto-gen single update`() = runTest {
        val autoGenItemDao = itemDatabase.autoGentItemDao()

        val item1 = AutoGenItem(name = "Name1")
        item1.id = autoGenItemDao.insert(item1)

        assertThat(autoGenItemDao.findCount()).isEqualTo(1)
        assertThat(autoGenItemDao.findNameById(item1.id)).isEqualTo("Name1")

        val item1a = item1.copy(name = "Name1a")
        autoGenItemDao.update(item1a)

        assertThat(autoGenItemDao.findCount()).isEqualTo(1)
        assertThat(autoGenItemDao.findNameById(item1a.id)).isEqualTo("Name1a")
    }

    @Test
    fun `bulk delete`() = runTest {
        val itemDao = itemDatabase.itemDao()

        val items = listOf(
            Item("1", "Name1"),
            Item("2", "Name2"),
            Item("3", "Name3")
        )

        itemDao.insertAll(items)
        assertThat(itemDao.findCount()).isEqualTo(3)

        itemDao.deleteAll(items.take(2))
        assertThat(itemDao.findCount()).isEqualTo(1)
    }

    @Test
    fun `auto-gen bulk delete`() = runTest {
        val autoGenItemDao = itemDatabase.autoGentItemDao()

        val items = listOf(
            AutoGenItem(name = "Name1"),
            AutoGenItem(name = "Name2"),
            AutoGenItem(name = "Name3"),
        )

        val ids = autoGenItemDao.insertAll(items)

        // assign ids to inserted items
        ids.forEachIndexed { index, id -> items[index].id = id }

        assertThat(autoGenItemDao.findCount()).isEqualTo(3)

        autoGenItemDao.deleteAll(items.take(2))
        assertThat(autoGenItemDao.findCount()).isEqualTo(1)
    }
}
