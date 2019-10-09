package org.dbtools.android.room.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import java.util.concurrent.Executor
import java.util.concurrent.Executors

@Database(
    entities = [
        Foo::class
    ],
    version = 1,
    exportSchema = false
)
abstract class TestDatabase : RoomDatabase() {
    abstract val fooDao: FooDao

    companion object {
        val transactionExecutor: Executor = Executors.newSingleThreadExecutor()
    }
}

@Entity
data class Foo(
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0L,
    var value: Int = 0
)

@Dao
interface FooDao {
    @Insert
    suspend fun insert(foo: Foo): Long

    @Delete
    suspend fun delete(foo: Foo): Int

    @Query("SELECT * FROM Foo")
    suspend fun findAll(): List<Foo>

    @Query("SELECT * FROM foo")
    fun findAllLiveData(): LiveData<List<Foo>>
}
