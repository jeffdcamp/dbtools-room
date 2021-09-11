package org.dbtools.android.room.jdbc

import android.database.DatabaseErrorHandler
import androidx.sqlite.db.SupportSQLiteOpenHelper

@Suppress("unused")
class JdbcSQLiteOpenHelperFactory(
    val path: String = "",
    private val password: String = "",
    private val databaseErrorHandler: DatabaseErrorHandler? = null,
    private val onDatabaseConfigureBlock: (sqliteDatabase: JdbcSqliteDatabase) -> Unit = {}
) : SupportSQLiteOpenHelper.Factory {

    override fun create(configuration: SupportSQLiteOpenHelper.Configuration): SupportSQLiteOpenHelper {
        return JdbcSQLiteOpenHelper(path, configuration.name, configuration.callback, password, databaseErrorHandler, onDatabaseConfigureBlock)
    }
}