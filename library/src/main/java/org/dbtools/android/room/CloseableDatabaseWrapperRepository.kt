package org.dbtools.android.room

import android.app.Application
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
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
     *   override fun createDatabase(application: Application, filename: String): BookDatabase {
     *     return Room.databaseBuilder(application, BookDatabase::class.java, filename)
     *       .fallbackToDestructiveMigration()
     *       .build()
     *   }
     * }
 * ```
 * // Initialize (one time)
 * bookDatabaseRepository.registerDatabase(application, "book-a", "/sdcard/Downloads/booka")
 *
 * ...
 *
 * // Regular use
 * val bookDatabase = bookDatabaseRepository.getDatabase("book-a")
 * bookDatabase.getAuthorDao().findNameById(123)

 *
 * ==== OR (with auto registration)
 *
 * ```
     * @Singleton
     * class BookDatabaseRepository
     * @Inject constructor(application: Application): CloseableDatabaseWrapperRepository<ShopDatabase>(application) {
     *
     *  override fun autoRegisterDatabase(key: String): Boolean {
     *      val databaseFile = fileUtil.getBookDatabase(key)
     *      registerDatabase(key, databaseFile.absolutePath)
     *      return true
     *  }
     *
     *   override fun createDatabase(application: Application, filename: String): BookDatabase {
     *     return Room.databaseBuilder(application, BookDatabase::class.java, filename)
     *       .fallbackToDestructiveMigration()
     *       .build()
     *   }
     * }
 * ```
 *
 * // Regular use (getDatabase(...) will auto register "book-a"
 * val bookDatabase = bookDatabaseRepository.getDatabase("book-a")
 * bookDatabase.getAuthorDao().findNameById(123)
 *
 */
abstract class CloseableDatabaseWrapperRepository<out T: RoomDatabase>(protected val application: Application) {
    private var databaseList = HashMap<String, T>()

    /**
     * Get database from internal repository.
     * NOTE: override autoRegisterDatabase(...) function to allow the getDatabase(...) function to auto register a database
     *
     * @param key of the database
     * @return RoomDatabase or null if the database does not exist
     */
    open fun getDatabase(key: String): T? {
        if (key.isBlank()) {
            Timber.e("key for the database is unspecified")
            return null
        }

        // if database is not registered, then try to auto-register
        if (!isDatabaseRegistered(key, true)) {
            Timber.d("database is not registered for key [$key]")
            return null
        }

        return databaseList[key]
    }

    /**
     * Override this function to allow the getDatabase(...) function to auto register a database.  The override function needs to call registerDatabase(...)
     *
     * @param key of the database
     * @return true if the database was registered
     */
    open fun autoRegisterDatabase(key: String) = false

    /**
     * Identify if the database has been added to the internal repository
     * NOTE: override autoRegisterDatabase(...) function to allow the isDatabaseRegistered(...) function to auto register a database
     *
     * @param key of the database
     * @return true if the database is added to the repository
     */
    open fun isDatabaseRegistered(key: String, autoRegister: Boolean = true): Boolean {
        val registered = databaseList.containsKey(key)

        return when {
            registered -> true
            !registered && autoRegister -> internalAutoRegister(key)
            else -> false
        }
    }

    /**
     * This function will call the override function autoRegisterDatabase(...)
     * @return true if the override function autoRegisterDatabase(...) successfully registered the database (this function will verify if it actually got added AFTER)
     */
    @Synchronized
    private fun internalAutoRegister(key: String): Boolean {
        if (autoRegisterDatabase(key)) {
            // make sure it actually got added
            return databaseList.containsKey(key)
        }

        return false
    }

    /**
     * Add database to internal repository
     *
     * @param key of the database
     * @param filename of the database
     * @return true if the database was added
     */
    @Synchronized
    open fun registerDatabase(key: String, filename: String): Boolean {
        if (isDatabaseRegistered(key, false)) {
            Timber.d("Database already registered for key [$key]")
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
     *
     * @return true if the entry existed and was removed
     */
    @Synchronized
    open fun closeDatabase(key: String, deleteFile: Boolean = false): Boolean {
        val database = databaseList[key]
        database?.let {
            val path = it.openHelper.writableDatabase.path
            it.close()
            if (deleteFile) {
                DatabaseUtil.deleteDatabaseFiles(File(path))
            }
        }

        return databaseList.remove(key) != null
    }

    /**
     * Close the database and remove the reference from the WrapperRepository
     *
     * @param deleteFiles Delete the database file and any associated files to this database (default: false)
     */
    open fun closeAllDatabases(deleteFiles: Boolean = false) {
        val keysCopy = ArrayList<String>()
        keysCopy.addAll(databaseList.keys)
        keysCopy.forEach { key -> closeDatabase(key, deleteFiles) }
    }

    fun dropView(database: SupportSQLiteDatabase, viewName: String) {
        DatabaseUtil.dropView(database, viewName)
    }

    fun dropAllViews(database: SupportSQLiteDatabase, views: List<DatabaseViewQuery>) {
        DatabaseUtil.dropAllViews(database, views)
    }

    fun createView(database: SupportSQLiteDatabase, viewName: String, viewQuery: String) {
        DatabaseUtil.createView(database, viewName, viewQuery)
    }

    fun createAllViews(database: SupportSQLiteDatabase, views: List<DatabaseViewQuery>) {
        DatabaseUtil.createAllViews(database, views)
    }

    fun recreateView(database: SupportSQLiteDatabase, viewName: String, viewQuery: String) {
        DatabaseUtil.recreateView(database, viewName, viewQuery)
    }

    fun recreateAllViews(database: SupportSQLiteDatabase, views: List<DatabaseViewQuery>) {
        DatabaseUtil.recreateAllViews(database, views)
    }
}
