package org.dbtools.android.room.jdbctest.extensions

import org.dbtools.android.room.jdbctest.BuildConfig
import org.dbtools.android.room.jdbctest.database.BaseMigrationTest
import org.dbtools.android.room.jdbctest.extensions.testdata.Migrate12Bad
import org.dbtools.android.room.jdbctest.extensions.testdata.Migrate12Good
import org.dbtools.android.room.jdbctest.extensions.testdata.TestDatabase
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class RoomMigrationTestValidMigration : BaseMigrationTest<TestDatabase>(BuildConfig.SCHEMA_PATH, TestDatabase::class.java, Migrate12Good()) {

    @Test
    fun `migrate 1 to 2`() {
        testMigration(1, 2)
    }
}

class RoomMigrationTestBadMigration : BaseMigrationTest<TestDatabase>(BuildConfig.SCHEMA_PATH, TestDatabase::class.java, Migrate12Bad()) {

    @Test
    fun `migrate 1 to 2 bad`() {
        assertThrows<IllegalStateException> {
            testMigration(1, 2)
        }
    }
}