package org.dbtools.android.room.jdbc

import android.database.DatabaseErrorHandler
import androidx.sqlite.db.SupportSQLiteOpenHelper

@Suppress("unused")
/**
 * JdbcSQLiteOpenHelperFactory - JDBC implementation of SupportSQLiteOpenHelper.Factory
 *
 * @property path Path to database
 * @property password Database password
 * @property databaseErrorHandler Not yet implemented
 * @property enableJdbcTransactionSupport Enable/Disable jdbc support via autoCommit (default = true).
 * NOTE: known issue as of Room 2.4.0 - bulk insert needs to be fixed (because inserts get put into multiple threads, this sometimes causes the jdbc driver to throw: "database in auto-commit mode")
 * @property onDatabaseConfigureBlock Block of code that is executed AFTER initial database connection and BEFORE database validation * @param enableJdbcTransactionSupport
 */
class JdbcSQLiteOpenHelperFactory(
    val path: String = "",
    private val password: String = "",
    private val databaseErrorHandler: DatabaseErrorHandler? = null,
    private val enableJdbcTransactionSupport: Boolean = true,
    private val onDatabaseConfigureBlock: (sqliteDatabase: JdbcSqliteDatabase) -> Unit = {}
) : SupportSQLiteOpenHelper.Factory {

    override fun create(configuration: SupportSQLiteOpenHelper.Configuration): SupportSQLiteOpenHelper {
        return JdbcSQLiteOpenHelper(path, configuration.name, configuration.callback, password, enableJdbcTransactionSupport, databaseErrorHandler, onDatabaseConfigureBlock)
    }
}