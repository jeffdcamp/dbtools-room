
package org.dbtools.android.room.sqliteorg

import android.arch.persistence.db.SupportSQLiteOpenHelper

@Suppress("unused")
class SqliteOrgSQLiteOpenHelperFactory(val path: String = "", val password: String = "", val libraryLoader: () -> Unit = DEFAULT_SQLITEX_LIBRARY_LOADER) : SupportSQLiteOpenHelper.Factory {

    override fun create(configuration: SupportSQLiteOpenHelper.Configuration): SupportSQLiteOpenHelper {
        return SqliteOrgSQLiteOpenHelper(configuration.context, path, configuration.name, configuration.callback, password, libraryLoader)
    }

    companion object {
        val DEFAULT_SQLITEX_LIBRARY_LOADER = { System.loadLibrary("sqliteX") } // https://sqlite.org/android/doc/trunk/www/index.wiki OR https://github.com/JeremiahStephenson/SQLite_Custom
    }
}