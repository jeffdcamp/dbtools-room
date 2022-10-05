package org.dbtools.android.room.sqliteorg

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import org.sqlite.database.DatabaseErrorHandler
import org.sqlite.database.sqlite.SQLiteDatabase
import org.sqlite.database.sqlite.SQLiteOpenHelper
import java.io.File

@Suppress("LongParameterList")
open class SqliteOrgSQLiteOpenHelper(
    context: Context,
    path: String,
    name: String?,
    callback: SupportSQLiteOpenHelper.Callback,
    password: String,
    libraryLoaderBlock: () -> Unit = SqliteOrgSQLiteOpenHelperFactory.loadSqliteLibrary,
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

        delegate = OpenHelper(
            context = context,
            libraryLoaderBlock = libraryLoaderBlock,
            onDatabaseConfigureBlock = onDatabaseConfigureBlock,
            name = databaseFile.absolutePath,
            callback = callback,
            password = password,
            errorHandler = databaseErrorHandler ?: DatabaseErrorHandler { dbObj -> callback.onCorruption(SqliteOrgDatabase(dbObj)) })
    }

    override val databaseName: String?
        get() {
            return delegate.databaseName
        }

    override fun setWriteAheadLoggingEnabled(enabled: Boolean) {
        delegate.setWriteAheadLoggingEnabled(enabled)
    }

    override val writableDatabase: SupportSQLiteDatabase
        get() {
            return delegate.getWritableSupportDatabase()
        }

    override val readableDatabase: SupportSQLiteDatabase
        get() {
            return delegate.getReadableSupportDatabase()
        }

    override fun close() {
        delegate.close()
    }

    @Suppress("LongParameterList")
    class OpenHelper(
        context: Context,
        libraryLoaderBlock: () -> Unit = {},
        private val onDatabaseConfigureBlock: (sqliteDatabase: SQLiteDatabase) -> Unit = {},
        private val name: String?,
        private val callback: SupportSQLiteOpenHelper.Callback,
        private val password: String,
        errorHandler: DatabaseErrorHandler? = null
    ) : SQLiteOpenHelper(
        context, name, null, callback.version, errorHandler
    ) {

        var wrappedDb: SqliteOrgDatabase? = null

        init {
            libraryLoaderBlock()
        }

        override fun onCreate(sqLiteDatabase: SQLiteDatabase) {
            val wrappedDb = SqliteOrgDatabase(sqLiteDatabase)
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

        fun getWritableSupportDatabase(): SupportSQLiteDatabase {
            val db = super.getWritableDatabase()
            return getWrappedDb(db)
        }

        fun getReadableSupportDatabase(): SupportSQLiteDatabase {
            val db = super.getReadableDatabase()
            return getWrappedDb(db)
        }

        fun getWrappedDb(sqLiteDatabase: SQLiteDatabase): SqliteOrgDatabase {
            return wrappedDb ?: run {
                val database = SqliteOrgDatabase(sqLiteDatabase)
                if (password.isNotBlank()) {
                    database.execSQL("PRAGMA key = '$password'")
                }
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