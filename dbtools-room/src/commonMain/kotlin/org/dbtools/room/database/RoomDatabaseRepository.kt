package org.dbtools.room.database

import androidx.room.RoomDatabase
import co.touchlab.kermit.Logger

/**
 * Database repository to hold key/value set of similar databases
 *
 * ```
     * class BookDatabaseRepository: RoomDatabaseRepository<BookDatabase>(context) {
     *
     *   override fun createDatabase(filename: String): BookDatabase {
     *           return Room.databaseBuilder<BookDatabase>(filename)
     *               .setDriver(BundledSQLiteDriver())
     *               .build()
     *    }
     *
     *   override fun deleteDatabase(filename: String) {
     *           FileSystem.SYSTEM.deleteDatabaseFiles(filename.toPath())
     *    }
     * }
 * ```
 * // Initialize (one time)
 * bookDatabaseRepository.registerDatabase("book-a", "/sdcard/Downloads/booka")
 *
 * ...
 *
 * // Regular use
 * val bookDatabase = bookDatabaseRepository.getDatabase("book-a")
 * bookDatabase?.getAuthorDao()?.findNameById(123)

 *
 * ==== OR (with auto registration)
 *
 * ```
     * class BookDatabaseRepository: RoomDatabaseRepository<BookDatabase>(context) {
     *
     *   override fun createDatabase(filename: String): BookDatabase {
     *           return Room.databaseBuilder<BookDatabase>(filename)
     *               .setDriver(BundledSQLiteDriver())
     *               .build()
     *    }
     *
     *   override fun deleteDatabase(filename: String) {
     *           FileSystem.SYSTEM.deleteDatabaseFiles(filename.toPath())
     *    }
     *
     *   override fun autoRegisterDatabase(key: String): Boolean {
     *      val databasePath: Path = GLFileUtil.getBookDatabaseFile(LangIso3(key))
     *
     *      if (FileSystem.SYSTEM.exists(databasePath)) {
     *          registerDatabase(key, databasePath.toString())
     *          return true
     *      }
     *
     *      return false
     *   }
     * }
 * ```
 *
 * // Regular use (getDatabase(...) will auto register "book-a"
 * val bookDatabase = bookDatabaseRepository.getDatabase("book-a")
 * bookDatabase.getAuthorDao().findNameById(123)
 *
 */
abstract class RoomDatabaseRepository<out T: RoomDatabase> {
    private val databaseList = mutableMapOf<String, RoomDatabaseRepositoryItem<T>>()

    protected abstract fun createDatabase(filename: String): T
    protected abstract fun deleteDatabase(filename: String)

    /**
     * Get database from internal repository.
     * NOTE: override autoRegisterDatabase(...) function to allow the getDatabase(...) function to auto register a database
     *
     * @param key of the database
     * @return RoomDatabase or null if the database does not exist
     */
    open fun getDatabase(key: String): T? {
        if (key.isBlank()) {
            Logger.e { "key for the database is unspecified" }
            return null
        }

        // if database is not registered, then try to auto-register
        if (!isDatabaseRegistered(key, true)) {
            Logger.d { "database is not registered for key [$key]" }
            return null
        }

        return databaseList[key]?.database
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
     * @param autoRegister if the database has not yet been added to the databaseList, then auto-add it if missing
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

    /*
     * This function will call the override function autoRegisterDatabase(...)
     * @return true if the override function autoRegisterDatabase(...) successfully registered the database (this function will verify if it actually got added AFTER)
     */
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
    open fun registerDatabase(key: String, filename: String): Boolean {
        if (isDatabaseRegistered(key, false)) {
            Logger.w { "Database already registered for key [$key]" }
            return false
        }

        databaseList[key] = RoomDatabaseRepositoryItem(createDatabase(filename), filename)

        return true
    }

    /**
     * Close the database and remove the reference from the WrapperRepository
     *
     * @param key of the database
     * @param deleteFile Delete the database file and any associated files to this database (default: false)
     *
     * @return true if the entry existed and was removed
     */
    open fun closeDatabase(key: String, deleteFile: Boolean = false): Boolean {
        try {
            val databaseRepositoryItem = databaseList[key]
            databaseRepositoryItem?.let {
                databaseRepositoryItem.database.close()

                if (deleteFile) {
                    deleteDatabase(databaseRepositoryItem.filename)
                }

            }
        } catch(ignore: Exception) {
            Logger.e(ignore) { "Failed to close database - key: [$key]" }
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
}

internal data class RoomDatabaseRepositoryItem<T: RoomDatabase>(
    val database: T,
    val filename: String
)
