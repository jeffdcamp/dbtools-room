package org.dbtools.android.room

import android.app.Application
import android.arch.persistence.room.RoomDatabase
import java.io.File
import java.io.IOException
import java.util.concurrent.atomic.AtomicReference


/**
 * Closeable database manager to handle the issue of open statements.
 * https://issuetracker.google.com/issues/64681453
 *
 * ```
 * @Singleton
 * class ShopDatabaseManager
 * @Inject constructor(application: Application): CloseableDatabaseManager<ShopDatabase>(application) {
 *
 *   fun getCheeseDao() = getDatabase().cheeseDao()
 *   fun getCommentDao() = getDatabase().commentDao()
 *
 *   override fun createDatabase(application: Application): ShopDatabase {
 *     return Room.databaseBuilder(application, ShopDatabase::class.java, ShopDatabase.DATABASE_NAME)
 *       .fallbackToDestructiveMigration()
 *       .build()
 *   }
 * }
 * ```
 */
abstract class CloseableDatabaseManager<out T: RoomDatabase>(protected val application: Application) {
    private var _database = AtomicReference<T?>()

    @Synchronized
    fun getDatabase() = _database.get() ?: createAndSetDatabase()

    protected abstract fun createDatabase(application: Application): T

    private fun createAndSetDatabase(): T {
        val database = createDatabase(application)
        _database.set(database)
        return database
    }

    @Synchronized
    fun closeDatabase(deleteFile: Boolean = false) {
        val database = _database.get()
        database?.let {
            val path = it.openHelper.writableDatabase.path
            it.close()
            if (deleteFile) {
                File(path).deleteQuietly()
            }
        }
        _database.set(null)
    }

    private fun File.deleteQuietly(): Boolean {
        try {
            if (exists()) {
                return delete()
            }
        } catch (ignore: IOException) {
            // IGNORE Exception
        }
        return false
    }
}
