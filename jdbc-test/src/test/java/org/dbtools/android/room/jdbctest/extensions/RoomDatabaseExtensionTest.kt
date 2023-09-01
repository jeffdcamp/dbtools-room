package org.dbtools.android.room.jdbctest.extensions

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isTrue
import io.mockk.mockk
import org.dbtools.android.room.jdbctest.extensions.testdata.TestDatabase
import org.dbtools.android.room.jdbctest.extensions.testdata.TestEntity
import org.dbtools.android.room.jdbctest.util.RoomTestFileSystem
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import java.io.File

class RoomDatabaseExtensionTest {

    @JvmField
    @RegisterExtension
    val roomDatabaseExtensionDefaultPath = RoomDatabaseExtension(mockk(), TestDatabase::class.java)

    @Test
    fun `insert rows`() {
        // Simple database operations test using the extension...
        roomDatabaseExtensionDefaultPath.testDatabase.testDao.insert(TestEntity(1L, "Name1", 30, "Cool Fact 1"))
        roomDatabaseExtensionDefaultPath.testDatabase.testDao.insert(TestEntity(2L, "Name2", 31, "Cool Fact 2"))

        val results = roomDatabaseExtensionDefaultPath.testDatabase.testDao.findAllTestEntities()
        assertThat(results.size).isEqualTo(2)
        assertThat(results.find { it.name == "Name1" }).isNotNull()
        assertThat(results.find { it.name == "Name2" }).isNotNull()

        // Verify the extension created the proper database file...
        val databaseFile = File(RoomTestFileSystem.INTERNAL_DATABASES_DIR_PATH, "insert_rows.db")
        assertThat(databaseFile.exists()).isTrue()
        assertThat(databaseFile.isFile).isTrue()
    }
}