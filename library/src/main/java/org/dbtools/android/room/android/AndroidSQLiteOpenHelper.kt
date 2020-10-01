@file:Suppress("PackageDirectoryMismatch") // so we don't have to repeat all the EXACT same code in
package org.dbtools.android.room.android

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import androidx.sqlite.db.SupportSQLiteOpenHelper
import java.io.File

open class AndroidSQLiteOpenHelper(
    context: Context,
    path: String,
    name: String?,
    callback: SupportSQLiteOpenHelper.Callback,
    onDatabaseConfigureBlock: (sqliteDatabase: SQLiteDatabase) -> Unit = {}
) : SupportSQLiteOpenHelper {

    private val delegate: OpenHelper

    init {
        val databaseFile = if (path.isEmpty()) {
            context.getDatabasePath(name)
        } else {
            File(path, name)
        }
        databaseFile.parentFile?.mkdirs()

        delegate = OpenHelper(context, onDatabaseConfigureBlock, databaseFile.absolutePath, callback)
    }

    override fun getDatabaseName(): String? {
        return delegate.databaseName
    }

    override fun setWriteAheadLoggingEnabled(enabled: Boolean) {
        delegate.setWriteAheadLoggingEnabled(enabled)
    }

    override fun getWritableDatabase(): AndroidSQLiteDatabase {
        return delegate.getWritableSupportDatabase()
    }

    override fun getReadableDatabase(): AndroidSQLiteDatabase {
        return delegate.getReadableSupportDatabase()
    }

    override fun close() {
        delegate.close()
    }

    class OpenHelper(
        context: Context,
        private val onDatabaseConfigureBlock: (sqliteDatabase: SQLiteDatabase) -> Unit = {},
        private val name: String?,
        private val callback: SupportSQLiteOpenHelper.Callback
    ) : SQLiteOpenHelper(
        context, name, null, callback.version
    ) {

        private var wrappedDb: AndroidSQLiteDatabase? = null

        override fun onCreate(sqLiteDatabase: SQLiteDatabase) {
            wrappedDb = AndroidSQLiteDatabase(sqLiteDatabase)
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
            if (wrappedDb == null) {
                val database = AndroidSQLiteDatabase(sqLiteDatabase)

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