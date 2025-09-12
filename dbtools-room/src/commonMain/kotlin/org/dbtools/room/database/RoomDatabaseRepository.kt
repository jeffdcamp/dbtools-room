package org.dbtools.room.database

import androidx.room.RoomDatabase
import co.touchlab.kermit.Logger
import kotlinx.atomicfu.locks.ReentrantLock
import kotlinx.atomicfu.locks.withLock

/**
 * Database repository to hold key/value set of similar databases
 *
 * @param <K> The type of the key used to identify each database instance
 * @param <T> The type of the RoomDatabase managed by this repository
 *
 * ```
     * data class LanguageCode(val value: Value)
     *
     * class BookDatabaseRepository: RoomDatabaseRepository<LanguageCode, BookDatabase>(context) {
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
abstract class RoomDatabaseRepository<K, out T: RoomDatabase> {
    private val lock = ReentrantLock()

    private val databaseList = mutableMapOf<String, RoomDatabaseRepositoryItem<T>>()

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
        return lock.withLock {
            if (!isDatabaseRegistered(key)) {
                val filename = getDatabaseFilename(key)
                if (filename != null) {
                    databaseList[keyAsString] = RoomDatabaseRepositoryItem(createDatabase(filename), filename)
                } else {
                    Logger.e { "Could not create database for key: [$keyAsString] because getDatabaseFilename($key) returned null" }
                    return@withLock null
                }
            }

            databaseList[keyAsString]?.database
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
