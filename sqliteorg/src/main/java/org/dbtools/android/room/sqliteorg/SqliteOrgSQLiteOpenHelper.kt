package org.dbtools.android.room.sqliteorg

import android.arch.persistence.db.SupportSQLiteDatabase
import android.arch.persistence.db.SupportSQLiteOpenHelper
import android.content.Context
import org.sqlite.database.sqlite.SQLiteDatabase
import org.sqlite.database.sqlite.SQLiteOpenHelper
import java.io.File

open class SqliteOrgSQLiteOpenHelper(context: Context,
                                     path: String,
                                     name: String?,
                                     version: Int,
                                     callback: SupportSQLiteOpenHelper.Callback,
                                     password: String) : SupportSQLiteOpenHelper {
    private val delegate: OpenHelper

    init {
        val databaseFilepath: String
        if (path.isEmpty()) {
            databaseFilepath = context.getDatabasePath(name).absolutePath
        } else {
            databaseFilepath = path + "/" + name
        }

        val databaseFile = File(databaseFilepath)
        databaseFile.parentFile.mkdirs()

        delegate = SqliteOrgSQLiteOpenHelper.OpenHelper(context, databaseFilepath, version, callback, password)
    }

    override fun getDatabaseName(): String? {
        return delegate.databaseName
    }

    override fun setWriteAheadLoggingEnabled(enabled: Boolean) {
        delegate.setWriteAheadLoggingEnabled(enabled)
    }

    override fun getWritableDatabase(): SupportSQLiteDatabase {
        return delegate.getWritableSupportDatabase()
    }

    override fun getReadableDatabase(): SupportSQLiteDatabase {
        return delegate.getReadableSupportDatabase()
    }

    override fun close() {
        delegate.close()
    }

    class OpenHelper(context: Context, private val name: String?, version: Int, private val callback: SupportSQLiteOpenHelper.Callback, private val password: String) : SQLiteOpenHelper(context, name, null, version) {
        var wrappedDb: SqliteOrgDatabase? = null

        init {
            System.loadLibrary("sqliteX") // load the sqlite.org library
        }

        override fun onCreate(sqLiteDatabase: SQLiteDatabase) {
            wrappedDb = SqliteOrgDatabase(sqLiteDatabase)
            callback.onCreate(wrappedDb)
        }

        override fun onUpgrade(sqLiteDatabase: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            callback.onUpgrade(getWrappedDb(sqLiteDatabase), oldVersion, newVersion)
        }

        override fun onConfigure(db: SQLiteDatabase) {
            callback.onConfigure(getWrappedDb(db))
        }

        override fun onDowngrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            callback.onDowngrade(getWrappedDb(db), oldVersion, newVersion)
        }

        override fun onOpen(db: SQLiteDatabase) {
            callback.onOpen(getWrappedDb(db))
        }

        fun getWritableSupportDatabase(): SupportSQLiteDatabase {
            val db = super.getWritableDatabase()
            return getWrappedDb(db)
        }

        fun getReadableSupportDatabase(): SupportSQLiteDatabase {
            val db = super.getReadableDatabase()
            return getWrappedDb(db)
        }

        fun getWrappedDb(sqLiteDatabase: SQLiteDatabase): SqliteOrgDatabase {
            if (wrappedDb == null) {
                val database = SqliteOrgDatabase(sqLiteDatabase)
                if (password.isNotBlank()) {
                    database.execSQL("PRAGMA key = '$password'")
                }

                wrappedDb = database
            }

            return wrappedDb!!
        }

        @Synchronized
        override fun close() {
            super.close()
            wrappedDb = null
        }

        override fun getDatabaseName(): String? {
            return name
        }
    }
}