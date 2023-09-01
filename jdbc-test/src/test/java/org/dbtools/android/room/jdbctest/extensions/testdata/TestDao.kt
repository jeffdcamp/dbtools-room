package org.dbtools.android.room.jdbctest.extensions.testdata

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

// Sample dao to test database extensions
    @Dao
    interface TestDao {

        @Insert(onConflict = OnConflictStrategy.REPLACE)
        fun insert(entity: TestEntity)

        @Query("SELECT * FROM TestEntity")
        fun findAllTestEntities(): List<TestEntity>

        @Query("SELECT count(1) FROM TestEntity")
        fun findCount(): Int

        @Query("DELETE FROM testEntity")
        fun deleteAllTestEntities()
    }