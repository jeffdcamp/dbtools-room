@file:Suppress("unused", "DuplicatedCode")

package org.dbtools.room.ext

import androidx.room.RoomDatabase
import androidx.room.TransactionScope
import androidx.room.immediateTransaction
import androidx.room.useReaderConnection
import androidx.room.useWriterConnection
import okio.FileSystem
import okio.Path
import org.dbtools.room.DatabaseViewQuery
import org.dbtools.room.data.AttachedDatabaseInfo

/**
 * Preform a PRAGMA check on the database and optionally check a table for existing data.
 *
 * @param tag Optional tag name to help identify database in logging
 * @param tableDataCountCheck Optional check on a table for data. (optional)
 * @param allowZeroCount Optional tableDataCountCheck if false return false if count is zero
 *
 * @return true if validation check is OK
 */
@Suppress("NestedBlockDepth")
suspend fun RoomDatabase.validateDatabase(tag: String = "", tableDataCountCheck: String? = null, allowZeroCount: Boolean = true): Boolean {
    return useReaderConnection { it.validateDatabase(tag, tableDataCountCheck, allowZeroCount) }
}

/**
 * Attach a database
 * @param toDatabasePath Path to attach database
 * @param toDatabaseName Alias name for attached database
 */
suspend fun RoomDatabase.attachDatabase(toDatabasePath: String, toDatabaseName: String) {
    return useWriterConnection { it.attachDatabase(toDatabasePath, toDatabaseName) }
}

/**
 * Detach a database
 * @param databaseName Alias name for attached database
 */
suspend fun RoomDatabase.detachDatabase(databaseName: String) {
    return useWriterConnection { it.detachDatabase(databaseName) }
}

/**
 * Get list each database attached to the current database connection.
 *
 * @return List of AttachedDatabaseInfo containing the database name and file path
 */
suspend fun RoomDatabase.getAttachedDatabases(): List<AttachedDatabaseInfo> {
    return useReaderConnection { it.getAttachedDatabases() }
}

/**
 * Find names of tables in this database
 * @param databaseName Alias name for database (such as an attached database) (optional)
 */
suspend fun RoomDatabase.findTableNames(databaseName: String = ""): List<String> {
    return useReaderConnection { it.findTableNames(databaseName) }
}

/**
 * Determines if table exist
 *
 * @param tableName Table name to check for
 * @param databaseName Alias name for database (such as an attached database) (optional)
 *
 * @return true If tableName exist
 */
suspend fun RoomDatabase.tableExists(tableName: String, databaseName: String = ""): Boolean {
    return tablesExists(listOf(tableName), databaseName)
}

/**
 * Determines if tables exist
 * @param tableNames Table names to check for
 * @param databaseName Alias name for database (such as an attached database) (optional)
 *
 * @return true If ALL tableNames exist
 */
suspend fun RoomDatabase.tablesExists(tableNames: List<String>, databaseName: String = ""): Boolean {
    return useReaderConnection { it.tablesExists(tableNames, databaseName) }
}

/**
 * Find names of views in this database
 * @param databaseName Alias name for database (such as an attached database) (optional)
 */
suspend fun RoomDatabase.findViewNames(databaseName: String = ""): List<String> {
    return useReaderConnection { it.findViewNames(databaseName) }
}

/**
 * Determines if view exist
 *
 * @param viewName View name to check for
 * @param databaseName Alias name for database (such as an attached database) (optional)
 *
 * @return true If viewName exist
 */
suspend fun RoomDatabase.viewExists(viewName: String, databaseName: String = ""): Boolean {
    return viewExists(listOf(viewName), databaseName)
}

/**
 * Determines if views exist
 * @param viewNames View names to check for
 * @param databaseName Alias name for database (such as an attached database) (optional)
 *
 * @return true If ALL viewNames exist
 */
suspend fun RoomDatabase.viewExists(viewNames: List<String>, databaseName: String = ""): Boolean {
    return useReaderConnection { it.viewExists(viewNames, databaseName) }
}

internal suspend fun RoomDatabase.execIntResultSql(sql: String, columnIndex: Int = 0): Int? {
    return useReaderConnection { it.execIntResultSql(sql, columnIndex) }
}

internal suspend fun RoomDatabase.execTextResultSql(sql: String, columnIndex: Int = 0): String? {
    return useReaderConnection { it.execTextResultSql(sql, columnIndex) }
}

/**
 * Drops view in a database
 *
 * @param viewName Name of view to drop
 */
suspend fun RoomDatabase.dropView(viewName: String) {
    return useWriterConnection { it.dropView(viewName) }
}

/**
 * Drops all views in a database
 *
 * @param views List of view names... if this list is empty then all views in the database will be dropped
 */
suspend fun RoomDatabase.dropAllViews(views: List<String> = emptyList()) {
    return useWriterConnection { it.dropAllViews(views) }
}

suspend fun RoomDatabase.createView(viewName: String, viewQuery: String) {
    return useWriterConnection { it.createView(viewName, viewQuery) }
}

suspend fun RoomDatabase.createAllViews(views: List<DatabaseViewQuery>) {
    views.forEach { createView(it.viewName, it.viewQuery.trim()) }
}

suspend fun RoomDatabase.recreateView(viewName: String, viewQuery: String) {
    dropView(viewName)
    createView(viewName, viewQuery)
}

/**
 * Drops all existing views and then recreates them in a database
 *
 * @param views List of Views to recreate
 */
suspend fun RoomDatabase.recreateAllViews(views: List<DatabaseViewQuery>) {
    views.forEach { recreateView(it.viewName, it.viewQuery.trim()) }
}

/**
 * Apply many SQL statements from a file. File must contain SQL statements separated by ;
 * All statements are executed in a single transaction
 *
 * @param fileSystem FileSystem containing statements
 * @param sqlPath File path within fileSystem containing statements
 *
 * @return true If all SQL statements successfully were applied
 */
suspend fun RoomDatabase.applySqlFile(fileSystem: FileSystem, sqlPath: Path): Boolean {
    return useWriterConnection { it.applySqlFile(fileSystem, sqlPath) }
}

/**
 * Check to see if a column in a database exists, if it does not... alter query will be run
 * @param tableName table for columnName
 * @param columnName column to from tableName to be checked
 * @param alterSql SQL to be run if the column does not exist.
 * Example: alterTableIfColumnDoesNotExist(database, "individual", "middle_name", "ALTER TABLE individual ADD `middle_name` TEXT DEFAULT '' NOT NULL")
 */
suspend fun RoomDatabase.alterTableIfColumnDoesNotExist(tableName: String, columnName: String, alterSql: String) {
    return useWriterConnection { it.alterTableIfColumnDoesNotExist(tableName, columnName, alterSql) }
}

/**
 * Check to see if a column in a database exists
 * @param tableName table for columnName
 * @param columnName column to from tableName to be checked
 * @return true if the column exists otherwise false
 */
@Suppress("NestedBlockDepth")
suspend fun RoomDatabase.columnExists(tableName: String, columnName: String): Boolean {
    return useReaderConnection { it.columnExists(tableName, columnName) }
}

/**
 * Get database version
 *
 * @return version of the database
 */
suspend fun RoomDatabase.getDatabaseVersion(): Int {
    return useReaderConnection { it.getDatabaseVersion() }
}

/**
 * Set database version
 *
 * @param newVersion version to be set on database
 */
suspend fun RoomDatabase.setDatabaseVersion(newVersion: Int) {
    return useWriterConnection { it.setDatabaseVersion(newVersion) }
}

/**
 * If we make a manual change, then we need to reset room so that it does not fail the validation
 *
 * @param newVersion version to be set on database (default to 0)
 */
suspend fun RoomDatabase.resetRoom(newVersion: Int = 0) {
    return useWriterConnection { it.resetRoom(newVersion) }
}

/**
 * Check integrity of a database connection using PRAGMA command
 * @return true if integrity_check == "ok"
 */
suspend fun RoomDatabase.isIntegrityOk(): Boolean {
    return useReaderConnection { it.isIntegrityOk() }
}

/**
 * Get the filename of the database
 *
 * @param name Name of the database (default is "main").  If you have attached databases, you can specify the alias name of the attached database.
 * @return The file path of the database, or null if not found
 */
suspend fun RoomDatabase.getDatabaseFilename(name: String = "main"): String? {
    return useReaderConnection { transactor ->
        transactor.getAttachedDatabases().firstOrNull { it.name == name }?.file
    }
}

/**
 * Performs a SQLiteTransactionType.IMMEDIATE within the block.
 *
 * @param T The return type of the transaction block.
 * @param block The block to execute in the transaction.
 */
suspend fun <T> RoomDatabase.withImmediateTransaction(block: suspend TransactionScope<T>.() -> T): T {
    return useWriterConnection { transactor ->
        transactor.immediateTransaction(block)
    }
}
