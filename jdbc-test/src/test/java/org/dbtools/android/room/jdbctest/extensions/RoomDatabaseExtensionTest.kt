package org.dbtools.android.room.jdbctest.extensions

import android.app.Application
import org.dbtools.android.room.jdbctest.extensions.testdata.TestDatabase
import org.dbtools.android.room.jdbctest.extensions.testdata.TestEntity
import org.dbtools.android.room.jdbctest.util.TestFileSystem
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.mockito.Mockito.mock
import java.io.File

class RoomDatabaseExtensionTest {

    @JvmField
    @RegisterExtension
    val roomDatabaseExtensionDefaultPath = RoomDatabaseExtension(mock(Application::class.java), TestDatabase::class.java)

    @Test
    fun `insert rows`() {
        // Simple database operations test using the extension...
        roomDatabaseExtensionDefaultPath.testDatabase.testDao.insert(TestEntity(1L, "Name1", 30, "Cool Fact 1"))
        roomDatabaseExtensionDefaultPath.testDatabase.testDao.insert(TestEntity(2L, "Name2", 31, "Cool Fact 2"))

        val results = roomDatabaseExtensionDefaultPath.testDatabase.testDao.findAllTestEntities()
        assertEquals(2, results.size)
        assertNotNull(results.find { it.name == "Name1" })
        assertNotNull(results.find { it.name == "Name2" })

        // Verify the extension created the proper database file...
        val databaseFile = File(TestFileSystem.INTERNAL_DATABASES_DIR_PATH, "insert_rows.db")
        assertTrue(databaseFile.exists())
        assertTrue(databaseFile.isFile)
    }
}