package org.dbtools.android.room.jdbctest.extensions.testdata

import androidx.room.Database
import androidx.room.RoomDatabase

// Sample database to test database extensions
@Database(
    entities = [TestEntity::class],
    version = 2
)
abstract class TestDatabase : RoomDatabase() {

    abstract val testDao: TestDao
}