package org.dbtools.android.room.jdbc

import android.database.DatabaseErrorHandler
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import java.io.File

/**
 * JdbcSQLiteOpenHelper - JDBC implementation of SupportSQLiteOpenHelper
 *
 * @param path Path to database
 * @param name Name of database.  If null then in memory database
 * @param callback SupportSQLiteOpenHelper.Callback
 * @param password Database password
 * @param databaseErrorHandler Not yet implemented
 * @param enableJdbcTransactionSupport Enable/Disable jdbc support via autoCommit (default = true).  NOTE: known issue as of Room 2.4.0 - bulk insert needs to be fixed (because inserts get put into multiple threads, this sometimes causes the jdbc driver to throw: "database in auto-commit mode")
 * @param onDatabaseConfigureBlock Block of code that is executed AFTER initial database connection and BEFORE database validation
 */
open class JdbcSQLiteOpenHelper(
    val path: String,
    val name: String?,
    val callback: SupportSQLiteOpenHelper.Callback,
    val password: String,
    val enableJdbcTransactionSupport: Boolean = true,
    val databaseErrorHandler: DatabaseErrorHandler? = null, // TODO Implement
    val onDatabaseConfigureBlock: (sqliteDatabase: JdbcSqliteDatabase) -> Unit = {}
) : SupportSQLiteOpenHelper {

    private val dbPath: String

    private var database: JdbcSqliteDatabase? = null
    private var initializing = false

    init {
        dbPath = if (name == null) {
            ":memory:"
        } else {
            val databaseFile = if (path.isBlank()) {
                File(name)
            } else {
                File(path, name)
            }
            databaseFile.parentFile?.mkdirs()
            databaseFile.path
        }
    }

    override fun getDatabaseName(): String {
        return name ?: ":memory:"
    }

    override fun setWriteAheadLoggingEnabled(enabled: Boolean) {
        // Do nothing
    }

    override fun getWritableDatabase(): SupportSQLiteDatabase {
        synchronized(this) {
            return getDatabaseLocked()
        }
    }

    override fun getReadableDatabase(): SupportSQLiteDatabase {
        synchronized(this) {
            return getDatabaseLocked()
        }
    }

    override fun close() {
        check(!initializing) { "Closed during initialization" }

        database?.let {
            if (it.isOpen) {
                it.close()
                database = null
            }
        }
    }

    private fun getDatabaseLocked(): JdbcSqliteDatabase {
        database?.let {
            if (!it.isOpen) {
                database = null
            } else {
                return it
            }
        }

        check(!initializing) { "getDatabase called recursively" }

        var db: JdbcSqliteDatabase? = null
        try {
            initializing = true
            db = JdbcSqliteDatabase(dbPath, enableJdbcTransactionSupport)
            if (password.isNotBlank()) {
                db.execSQL("PRAGMA key = '$password'")
            }

            onDatabaseConfigureBlock(db)
            callback.onConfigure(db)
            val version = callback.version

            val dbVersion = db.version
            if (dbVersion != version) {
                db.beginTransaction()
                try {
                    if (dbVersion == 0) {
                        callback.onCreate(db)
                    } else {
                        if (dbVersion > version) {
                            callback.onDowngrade(db, dbVersion, version)
                        } else {
                            callback.onUpgrade(db, dbVersion, version)
                        }
                    }
                    db.version = version
                    db.setTransactionSuccessful()
                } finally {
                    db.endTransaction()
                }
            }

            callback.onOpen(db)

            database = db
            return db
        } finally {
            initializing = false
            if (db != null && db !== database) {
                db.close()
            }
        }
    }
}