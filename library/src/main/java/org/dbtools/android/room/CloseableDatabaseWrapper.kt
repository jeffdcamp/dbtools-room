@file:Suppress("unused")

package org.dbtools.android.room

import android.app.Application
import androidx.room.RoomDatabase
import org.dbtools.android.room.util.DatabaseUtil
import java.io.File
import java.util.concurrent.atomic.AtomicReference


/**
 * Closeable database wrapper to handle the issue of open statements.
 * https://issuetracker.google.com/issues/64681453
 *
 * ```
     * @Singleton
     * class ShopDatabaseManager
     * @Inject constructor(application: Application): CloseableDatabaseWrapper<ShopDatabase>(application) {
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
abstract class CloseableDatabaseWrapper<out T: RoomDatabase>(protected val application: Application) {
    private var _database = AtomicReference<T?>()

    @Synchronized
    open fun getDatabase() = _database.get() ?: createAndSetDatabase()

    protected abstract fun createDatabase(): T

    private fun createAndSetDatabase(): T {
        val database = createDatabase()
        _database.set(database)
        return database
    }

    /**
     * Close the database and remove the reference from the Wrapper
     *
     * @param deleteFile Delete the database file and any associated files to this database (default: false)
     */
    @Synchronized
    open fun closeDatabase(deleteFile: Boolean = false) {
        val database = _database.get()
        database?.let {
            val path = it.openHelper.writableDatabase.path
            it.close()
            if (deleteFile) {
                DatabaseUtil.deleteDatabaseFiles(File(path))
            }
        }
        _database.set(null)
    }
}
