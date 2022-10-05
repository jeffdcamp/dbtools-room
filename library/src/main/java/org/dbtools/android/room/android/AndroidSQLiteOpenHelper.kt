@file:Suppress("PackageDirectoryMismatch") // so we don't have to repeat all the EXACT same code in
package org.dbtools.android.room.android

import android.content.Context
import android.database.DatabaseErrorHandler
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import androidx.sqlite.db.SupportSQLiteOpenHelper
import java.io.File

open class AndroidSQLiteOpenHelper(
    context: Context,
    path: String,
    name: String?,
    callback: SupportSQLiteOpenHelper.Callback,
    databaseErrorHandler: DatabaseErrorHandler? = null,
    onDatabaseConfigureBlock: (sqliteDatabase: SQLiteDatabase) -> Unit = {}
) : SupportSQLiteOpenHelper {

    private val delegate: OpenHelper

    init {
        val databaseFile = if (path.isEmpty()) {
            context.getDatabasePath(name)
        } else {
            File(path, name ?: "database")
        }
        databaseFile.parentFile?.mkdirs()

        delegate = OpenHelper(context, onDatabaseConfigureBlock, databaseFile.absolutePath, callback, databaseErrorHandler)
    }

    override val databaseName: String?
        get() {
            return delegate.databaseName
        }

    override fun setWriteAheadLoggingEnabled(enabled: Boolean) {
        delegate.setWriteAheadLoggingEnabled(enabled)
    }

    override val writableDatabase: AndroidSQLiteDatabase
        get() {
            return delegate.getWritableSupportDatabase()
        }

    override val readableDatabase: AndroidSQLiteDatabase
        get() {
            return delegate.getReadableSupportDatabase()
        }

    override fun close() {
        delegate.close()
    }

    class OpenHelper(
        context: Context,
        private val onDatabaseConfigureBlock: (sqliteDatabase: SQLiteDatabase) -> Unit = {},
        private val name: String?,
        private val callback: SupportSQLiteOpenHelper.Callback,
        databaseErrorHandler: DatabaseErrorHandler? = null
    ) : SQLiteOpenHelper(
        context,
        name,
        null,
        callback.version,
        databaseErrorHandler ?: DatabaseErrorHandler { dbObj -> callback.onCorruption(AndroidSQLiteDatabase(dbObj)) }
    ) {

        private var wrappedDb: AndroidSQLiteDatabase? = null

        override fun onCreate(sqLiteDatabase: SQLiteDatabase) {
            val wrappedDb = AndroidSQLiteDatabase(sqLiteDatabase)
            this.wrappedDb = wrappedDb
            callback.onCreate(wrappedDb)
        }

        override fun onUpgrade(sqLiteDatabase: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            callback.onUpgrade(getWrappedDb(sqLiteDatabase), oldVersion, newVersion)
        }

        override fun onConfigure(db: SQLiteDatabase) {
            onDatabaseConfigureBlock(db)
            callback.onConfigure(getWrappedDb(db))
        }

        override fun onDowngrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            callback.onDowngrade(getWrappedDb(db), oldVersion, newVersion)
        }

        override fun onOpen(db: SQLiteDatabase) {
            callback.onOpen(getWrappedDb(db))
        }

        fun getWritableSupportDatabase(): AndroidSQLiteDatabase {
            return getWrappedDb(super.getWritableDatabase())
        }

        fun getReadableSupportDatabase(): AndroidSQLiteDatabase {
            return getWrappedDb(super.getReadableDatabase())
        }

        private fun getWrappedDb(sqLiteDatabase: SQLiteDatabase): AndroidSQLiteDatabase {
            return wrappedDb ?: run {
                val database = AndroidSQLiteDatabase(sqLiteDatabase)
                wrappedDb = database
                database
            }
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