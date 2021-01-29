package org.dbtools.android.room.jdbctest.extensions.testdata

import androidx.room.Entity
import androidx.room.PrimaryKey

// Sample Entity to test database extensions
@Entity(tableName = "testEntity")
data class TestEntity(
    @PrimaryKey
    var id: Long,

    var name: String,
    var age: Int,
    var coolFact: String?
)