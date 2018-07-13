package org.dbtools.android.room.sqliteorg

import android.arch.persistence.db.SupportSQLiteOpenHelper
import org.sqlite.database.sqlite.SQLiteDatabase

/**
 * @param path Custom path to database
 * @param password Passord field if using Sqlite SEE or other encryption library
 * @param libraryLoaderBlock Load custom libraries here.  Default: System.loadLibrary("sqliteX")
 * @param postDatabaseCreateBlock After creation of the org.sqlite.database.sqlite.SQLiteDatabase object, this block will be called with the created instance, allowing further customizations
 */
@Suppress("unused")
class SqliteOrgSQLiteOpenHelperFactory(
    private val path: String = "",
    private val password: String = "",
    private val libraryLoaderBlock: () -> Unit = loadSqliteLibrary,
    private val postDatabaseCreateBlock: (sqliteDatabase: SQLiteDatabase) -> Unit = {}
) : SupportSQLiteOpenHelper.Factory {

    override fun create(configuration: SupportSQLiteOpenHelper.Configuration): SupportSQLiteOpenHelper {
        return SqliteOrgSQLiteOpenHelper(configuration.context, path, configuration.name, configuration.callback, password, libraryLoaderBlock, postDatabaseCreateBlock)
    }

    companion object {
        // https://sqlite.org/android/doc/trunk/www/index.wiki OR https://github.com/JeremiahStephenson/SQLite_Custom
        val loadSqliteLibrary = { System.loadLibrary("sqliteX") }
    }
}