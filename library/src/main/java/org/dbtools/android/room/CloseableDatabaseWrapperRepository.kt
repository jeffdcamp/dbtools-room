package org.dbtools.android.room

import android.app.Application
import androidx.room.RoomDatabase
import org.dbtools.android.room.util.DatabaseUtil
import timber.log.Timber
import java.io.File


/**
 * Closeable database wrapper repository to handle the issue of open statements.
 * https://issuetracker.google.com/issues/64681453
 *
 *
 * ```
     * @Singleton
     * class BookDatabaseRepository
     * @Inject constructor(application: Application): CloseableDatabaseWrapperRepository<ShopDatabase>(application) {
     *
     *   fun getAuthorDao() = getDatabase().authorDao()
     *   fun getContentDao() = getDatabase().contentDao()
     *
     *   override fun createDatabase(application: Application, filename: String): BookDatabase {
     *     return Room.databaseBuilder(application, BookDatabase::class.java, filename)
     *       .fallbackToDestructiveMigration()
     *       .build()
     *   }
     * }
 * ```
 *
 * // Initialize (one time)
 * bookDatabaseRepository.registerDatabase(application, "book-a", "/sdcard/Downloads/booka")
 *
 * ...
 *
 * // Regular use
 * val bookDatabase = bookDatabaseRepository.getDatabase("book-a")
 * bookDatabase.getAuthorDao().findNameById(123)
 */
abstract class CloseableDatabaseWrapperRepository<out T: RoomDatabase>(protected val application: Application) {
    private var databaseList = HashMap<String, T>()

    @Synchronized
    open fun getDatabase(key: String) = databaseList[key] ?: throw IllegalStateException("Database for key [$key] does not exist.  Be sure to registerDatabase(key, filename) before calling getDatabase(key)")

    open fun isDatabaseRegistered(key: String) = databaseList.containsKey(key)

    @Synchronized
    open fun registerDatabase(key: String, filename: String): Boolean {
        if (isDatabaseRegistered(key)) {
            Timber.e("Database already registered for key [$key]")
            return false
        }

        databaseList[key] = createDatabase(filename)

        return true
    }

    protected abstract fun createDatabase(filename: String): T

    /**
     * Close the database and remove the reference from the WrapperRepository
     *
     * @param deleteFile Delete the database file and any associated files to this database (default: false)
     */
    @Synchronized
    open fun closeDatabase(key: String, deleteFile: Boolean = false) {
        val database = databaseList[key]
        database?.let {
            val path = it.openHelper.writableDatabase.path
            it.close()
            if (deleteFile) {
                DatabaseUtil.deleteDatabaseFiles(File(path))
            }
        }
        databaseList.remove(key)
    }

    /**
     * Close the database and remove the reference from the WrapperRepository
     *
     * @param deleteFile Delete the database file and any associated files to this database (default: false)
     */
    open fun closeAllDatabases(deleteFiles: Boolean = false) {
        val keysCopy = ArrayList<String>()
        keysCopy.addAll(databaseList.keys)
        keysCopy.forEach { key -> closeDatabase(key, deleteFiles) }
    }
}
