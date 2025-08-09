package org.dbtools.room.database

import androidx.room.RoomDatabase
import co.touchlab.kermit.Logger
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Database repository to hold key/value set of similar databases
 *
 * ```
     * data class LanguageCode(val value: Value)
     *
     * class BookDatabaseRepository: RoomDatabaseRepository<BookDatabase, LanguageCode>(context) {
     *
     *   override fun getDatabaseFilename(key: LanguageCode): String? {
     *       val databasePath: Path = getBookDatabasePath(key)
     *       return if (fileSystem.exists(databasePath)) databasePath.toString() else null
     *   }
     *
     *   override fun createDatabase(filename: String): BookDatabase {
     *       return Room.databaseBuilder<BookDatabase>(filename)
     *           .setDriver(BundledSQLiteDriver())
     *           .build()
     *   }
     *
     *   override fun deleteDatabase(filename: String) {
     *       FileSystem.SYSTEM.deleteDatabaseFiles(filename.toPath())
     *   }
     *
     *   override fun keyToString(key: LanguageCode): String = key.value
     * }
 * ```
 *
 * // Regular use
 * val bookDatabase = bookDatabaseRepository.getDatabase("book-a")
 * bookDatabase?.getAuthorDao()?.findNameById(123)
 *
 */
abstract class RoomDatabaseRepository<out T: RoomDatabase, K> {
    private val databaseList = mutableMapOf<String, RoomDatabaseRepositoryItem<T>>()

    private val creationLocks = mutableMapOf<String, Mutex>()
    private val creationLocksMutex = Mutex()
    private suspend fun getKeyMutex(key: String): Mutex {
        return creationLocksMutex.withLock {
            creationLocks.getOrPut(key) { Mutex() }
        }
    }

    /**
     * Get the database filename/path
     *
     * @param key of the database
     * @return filename/path of the database or null if the database could not set up
     */
    abstract fun getDatabaseFilename(key: K): String?

    /**
     * Create a new database instance
     *
     * @param filename filename/path of the database
     */
    protected abstract fun createDatabase(filename: String): T

    /**
     * Delete the database file
     *
     * @param filename filename/path of the database
     */
    protected abstract fun deleteDatabase(filename: String)

    protected abstract fun keyToString(key: K): String

    /**
     * Get database from internal repository.
     *
     * @param key of the database
     * @return RoomDatabase or null if the database does not exist
     */
    open fun getDatabase(key: K): T? {
        val keyAsString = keyToString(key)
        if (keyAsString.isBlank()) {
            Logger.e { "key for the database is unspecified or blank (check keyToString(key) results)" }
            return null
        }

        val database = databaseList[keyAsString]?.database
        if (database != null) {
            // database is already registered
            return database
        }

        // if database is not registered then try to register it
        //
        // Use runBlocking here so that getDatabase(...) does not need to be a suspend function
        //    1. This section of code should be called VERY rarely (ONLY when first-time registration happens), so it is not a performance issue
        //    2. This allows the caller to not have to use a suspend function (especially when using a Flow Dao function... which shouldn't be a suspend function)
        return runBlocking {
            getKeyMutex(keyAsString).withLock {
                if (!isDatabaseRegistered(key)) {
                    val filename = getDatabaseFilename(key)
                    if (filename != null) {
                        databaseList[keyAsString] = RoomDatabaseRepositoryItem(createDatabase(filename), filename)
                    } else {
                        return@withLock null
                    }
                }

                databaseList[keyAsString]?.database
            }
        }
    }

    /**
     * Identify if the database has been added to the internal repository
     * NOTE: override autoRegisterDatabase(...) function to allow the isDatabaseRegistered(...) function to auto register a database
     *
     * @param key of the database
     * @return true if the database is added to the repository
     */
    fun isDatabaseRegistered(key: K): Boolean = databaseList.containsKey(keyToString(key))

    /**
     * Close the database and remove the reference from the WrapperRepository
     *
     * @param key of the database
     * @param deleteFile Delete the database file and any associated files to this database (default: false)
     *
     * @return true if the entry existed and was removed
     */
    open fun closeDatabase(key: K, deleteFile: Boolean = false): Boolean {
        return closeDatabase(keyToString(key), deleteFile)
    }

    private fun closeDatabase(key: String, deleteFile: Boolean = false): Boolean {
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
        databaseList.keys.forEach { key -> closeDatabase(key, deleteFiles) }
    }
}

internal data class RoomDatabaseRepositoryItem<T: RoomDatabase>(
    val database: T,
    val filename: String
)
