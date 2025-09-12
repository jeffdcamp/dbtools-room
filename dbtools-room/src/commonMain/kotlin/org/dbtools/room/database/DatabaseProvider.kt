package org.dbtools.room.database

import androidx.room.RoomDatabase
import kotlinx.atomicfu.locks.ReentrantLock
import kotlinx.atomicfu.locks.withLock
import kotlin.concurrent.atomics.ExperimentalAtomicApi

/**
 * DatabaseProvider is an abstract class that provides a way to manage a RoomDatabase instance.
 * It handles the creation, retrieval, closing, and deletion of the database in a thread-safe manner.
 *
 * @param T The type of the RoomDatabase.
 */
@OptIn(ExperimentalAtomicApi::class)
abstract class DatabaseProvider<T : RoomDatabase> {
    private val lock = ReentrantLock()

    private var database: T? = null

    /**
     * Creates a new instance of the RoomDatabase.
     * This method is called when the database is first accessed and needs to be initialized.
     */
    abstract fun createDatabase(): T

    /**
     * Deletes the database files.
     * This method is called when the database needs to be completely removed from the device.
     */
    abstract fun deleteDatabase()

    /**
     * Gets the database instance.
     * If the database has not been created yet, it will be created.
     *
     * @return The database instance.
     */
    open fun getDatabase(): T {
        return lock.withLock {
            database ?: createDatabase().also { database = it }
        }
    }

    /**
     * Closes the database.
     * If [delete] is true, the database files will also be deleted.
     *
     * @param delete If true, the database files will be deleted.
     */
    open fun closeDatabase(delete: Boolean = false) {
        lock.withLock {
            database?.close()
            database = null
            if (delete) {
                deleteDatabase()
            }
        }
    }
}
