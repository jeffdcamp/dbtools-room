package org.dbtools.android.room.jdbc

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import java.util.UUID

@Database(
    entities = [Item::class],
    version = 1
)
abstract class ItemDatabase : RoomDatabase() {
    abstract val itemDao: ItemDao
}

@Dao
interface ItemDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: Item)

    @Query("SELECT * FROM Item")
    suspend fun findAll(): List<Item>

    @Query("DELETE FROM Item")
    suspend fun deleteAll()

    @Query("SELECT count(1) FROM Item")
    suspend fun findCount(): Int
}

@Entity
data class Item (
    @PrimaryKey
    var id: String = UUID.randomUUID().toString(),
    var name: String,
)