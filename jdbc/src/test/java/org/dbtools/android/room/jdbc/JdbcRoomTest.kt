package org.dbtools.android.room.jdbc

import android.app.Application
import androidx.room.Room
import androidx.room.withTransaction
import com.google.common.truth.Truth
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import java.util.concurrent.Executors

class JdbcRoomTest {

    private lateinit var itemDatabase: ItemDatabase

    @BeforeEach
    fun setUp() {
        val databaseFile = File("build/room-test-filesystem/database/item.db")

        itemDatabase = Room.databaseBuilder(mockk<Application>(), ItemDatabase::class.java, databaseFile.name)
            .allowMainThreadQueries()
            .setTransactionExecutor(Executors.newSingleThreadExecutor())
            .openHelperFactory(JdbcSQLiteOpenHelperFactory(databaseFile.parentFile.path))
//            .setQueryCallback({ sql, args -> println("Query: [$sql]  Args: $args") }) { it.run() }
            .build()
    }


    @Test
    fun loadTestWithTransaction() = runBlocking {
        testRange(itemDatabase, 1, true)
        testRange(itemDatabase, 10, true)
        testRange(itemDatabase, 25, true)
        testRange(itemDatabase, 100, true)
    }

    @Test
    fun loadTestWithOutTransaction() = runBlocking {
        // single insert works just fine with
        testRange(itemDatabase, 1, false)

        // todo: known issue - bulk insert needs to be fixed (because inserts get put into multiple threads, this sometimes causes the jdbc driver to throw: "database in auto-commit mode")
//        testRange(itemDatabase, 10, false)
//        testRange(itemDatabase, 25, false)
//        testRange(itemDatabase, 100, false)
    }

    private suspend fun testRange(itemDatabase: ItemDatabase, numItems: Int, withTransaction: Boolean) {
        val itemDao = itemDatabase.itemDao

        // remove any existing records
        itemDao.deleteAll()

        // get list of names
        val itemNames = (1..numItems).map { it.toString() }

        // insert
        if (withTransaction) {
            itemDatabase.withTransaction {
                itemNames.forEach { itemDao.insert(Item(name = it)) }
            }
        } else {
            itemNames.forEach { itemDao.insert(Item(name = it)) }
        }

        // make sure all was inserted
        Truth.assertThat(itemDao.findCount()).isEqualTo(itemNames.size)
    }
}


