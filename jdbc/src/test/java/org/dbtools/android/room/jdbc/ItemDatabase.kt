package org.dbtools.android.room.jdbc

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Update
import java.util.UUID

@Database(
    entities = [Item::class, AutoGenItem::class],
    version = 1
)
abstract class ItemDatabase : RoomDatabase() {
    abstract fun itemDao(): ItemDao
    abstract fun autoGentItemDao(): AutoGenItemDao
}

@Dao
interface ItemDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: Item)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entity: List<Item>)

    @Update
    suspend fun update(entity: Item)

    @Delete
    suspend fun deleteAll(entity: List<Item>)

    @Query("SELECT * FROM Item")
    suspend fun findAll(): List<Item>

    @Query("SELECT name FROM Item WHERE id = :id")
    suspend fun findNameById(id: String): String?

    @Query("DELETE FROM Item")
    suspend fun deleteAll()

    @Query("SELECT count(1) FROM Item")
    suspend fun findCount(): Int
}

@Entity
data class Item (
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String,
)


@Dao
interface AutoGenItemDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: AutoGenItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entity: List<AutoGenItem>): List<Long>

    @Update
    suspend fun update(entity: AutoGenItem)

    @Delete
    suspend fun deleteAll(entity: List<AutoGenItem>)

    @Query("SELECT * FROM AutoGenItem")
    suspend fun findAll(): List<AutoGenItem>

    @Query("SELECT name FROM AutoGenItem WHERE id = :id")
    suspend fun findNameById(id: Long): String?

    @Query("DELETE FROM AutoGenItem")
    suspend fun deleteAll()

    @Query("SELECT count(1) FROM AutoGenItem")
    suspend fun findCount(): Int
}

@Entity
data class AutoGenItem (
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0L,
    val name: String,
)