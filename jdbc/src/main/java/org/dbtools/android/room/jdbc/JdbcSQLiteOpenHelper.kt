package org.dbtools.android.room.jdbc

import android.arch.persistence.db.SupportSQLiteDatabase
import android.arch.persistence.db.SupportSQLiteOpenHelper
import java.io.File

open class JdbcSQLiteOpenHelper(val path: String,
                                val name: String?,
                                val callback: SupportSQLiteOpenHelper.Callback) : SupportSQLiteOpenHelper {

    private val dbPath: String

    private var database: JdbcSqliteDatabase? = null
    private var initializing = false

    init {
        if (name == null) {
            dbPath = ":memory:"
        } else {
            val databaseFile = if (path.isBlank()) {
                File(name)
            } else {
                File(path, name)
            }
            databaseFile.parentFile.mkdirs()
            dbPath = databaseFile.path
        }
    }

    override fun getDatabaseName(): String {
        return name ?: ":memory:"
    }

    override fun setWriteAheadLoggingEnabled(enabled: Boolean) {
        throw UnsupportedOperationException()
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
            db = JdbcSqliteDatabase(dbPath)

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