@file:Suppress("unused", "DuplicatedCode")

package org.dbtools.room.ext

import androidx.room.Transactor
import androidx.room.execSQL
import androidx.room.immediateTransaction
import androidx.sqlite.SQLiteStatement
import co.touchlab.kermit.Logger
import okio.FileSystem
import okio.Path
import org.dbtools.room.DatabaseViewQuery
import org.dbtools.room.data.AttachedDatabaseInfo
import kotlin.time.TimeSource.Monotonic.markNow

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
suspend fun Transactor.validateDatabase(tag: String = "", tableDataCountCheck: String? = null, allowZeroCount: Boolean = true): Boolean {
    Logger.i { "Checking database integrity for [$tag]" }
    val mark = markNow()
    try {
        // pragma check
        if (!isIntegrityOk()) {
            Logger.e { "validateDatabase - database [$tag] isDatabaseIntegrityOk check failed" }
            return false
        }

        // make sure there is data in the database
        if (!tableDataCountCheck.isNullOrBlank()) {
            usePrepared("SELECT count(1) FROM $tableDataCountCheck") { statement ->
                val count = if (statement.step()) {
                    statement.getInt(0)
                } else {
                    null
                }

                if (count == null || (!allowZeroCount && count == 0)) {
                    Logger.e { "validateDatabase - table [$tableDataCountCheck] is BLANK for database [$tag] is blank" }
                    return@usePrepared false
                }
            }
        }
    } catch (expected: Exception) {
        Logger.e(expected) { "Failed to validate database [$tag]" }
        return false
    }

    Logger.i { "Database integrity for [$tag]  OK! (${mark.elapsedNow()})" }
    return true
}

/**
 * Attach a database
 * @param toDatabasePath Path to attach database
 * @param toDatabaseName Alias name for attached database
 */
suspend fun Transactor.attachDatabase(toDatabasePath: String, toDatabaseName: String) {
    val sql = "ATTACH DATABASE '$toDatabasePath' AS $toDatabaseName"
    execSQL(sql)
}

/**
 * Detach a database
 * @param databaseName Alias name for attached database
 */
suspend fun Transactor.detachDatabase(databaseName: String) {
    val sql = "DETACH DATABASE '$databaseName'"
    execSQL(sql)
}

/**
 * Get list each database attached to the current database connection.
 *
 * @return List of AttachedDatabaseInfo containing the database name and file path
 */
suspend fun Transactor.getAttachedDatabases(): List<AttachedDatabaseInfo> {
    val databaseInfoList = mutableListOf<AttachedDatabaseInfo>()

    usePrepared("SELECT * FROM pragma_database_list") { statement ->
        val nameColumnIndex = statement.getColumnIndexOrThrow("name")
        val fileColumnIndex = statement.getColumnIndexOrThrow("file")
        while (statement.step()) {
            val name = statement.getText(nameColumnIndex)
            val file = statement.getText(fileColumnIndex)
            databaseInfoList.add(AttachedDatabaseInfo(name, file))
        }
    }

    return databaseInfoList
}

/**
 * Find names of tables in this database
 * @param databaseName Alias name for database (such as an attached database) (optional)
 */
suspend fun Transactor.findTableNames(databaseName: String = ""): List<String> {
    val tableNames = mutableListOf<String>()
    if (databaseName.isNotBlank()) {
        usePrepared("SELECT tbl_name FROM $databaseName.sqlite_master where type='table'") {
            while (it.step()) {
                tableNames.add(it.getText(0))
            }
        }
    } else {
        usePrepared("SELECT tbl_name FROM sqlite_master where type='table'")  {
            while (it.step()) {
                tableNames.add(it.getText(0))
            }
        }
    }

    return tableNames
}

/**
 * Determines if table exist
 *
 * @param tableName Table name to check for
 * @param databaseName Alias name for database (such as an attached database) (optional)
 *
 * @return true If tableName exist
 */
suspend fun Transactor.tableExists(tableName: String, databaseName: String = ""): Boolean {
    return tablesExists(listOf(tableName), databaseName)
}

/**
 * Determines if tables exist
 * @param tableNames Table names to check for
 * @param databaseName Alias name for database (such as an attached database) (optional)
 *
 * @return true If ALL tableNames exist
 */
suspend fun Transactor.tablesExists(tableNames: List<String>, databaseName: String = ""): Boolean {
    val inClaus = tableNames.joinToString(",", prefix = "(", postfix = ")") { "'$it'" }

    val tableCount = if (databaseName.isNotBlank()) {
        execIntResultSql("SELECT count(1) FROM $databaseName.sqlite_master WHERE type='table' AND tbl_name IN $inClaus")
    } else {
        execIntResultSql("SELECT count(1) FROM sqlite_master WHERE type='table' AND tbl_name IN $inClaus")
    }

    return tableCount == tableNames.size
}

/**
 * Find names of views in this database
 * @param databaseName Alias name for database (such as an attached database) (optional)
 */
suspend fun Transactor.findViewNames(databaseName: String = ""): List<String> {
    val viewNames = mutableListOf<String>()
    if (databaseName.isNotBlank()) {
        usePrepared("SELECT tbl_name FROM $databaseName.sqlite_master where type='view'") {
            while (it.step()) {
                viewNames.add(it.getText(0))
            }
        }
    } else {
        usePrepared("SELECT tbl_name FROM sqlite_master where type='view'") {
            while (it.step()) {
                viewNames.add(it.getText(0))
            }
        }
    }

    return viewNames
}

/**
 * Determines if view exist
 *
 * @param viewName View name to check for
 * @param databaseName Alias name for database (such as an attached database) (optional)
 *
 * @return true If viewName exist
 */
suspend fun Transactor.viewExists(viewName: String, databaseName: String = ""): Boolean {
    return viewExists(listOf(viewName), databaseName)
}

/**
 * Determines if views exist
 * @param viewNames View names to check for
 * @param databaseName Alias name for database (such as an attached database) (optional)
 *
 * @return true If ALL viewNames exist
 */
suspend fun Transactor.viewExists(viewNames: List<String>, databaseName: String = ""): Boolean {
    val inClaus = viewNames.joinToString(",", prefix = "(", postfix = ")") { "'$it'" }

    val viewCount = if (databaseName.isNotBlank()) {
        execIntResultSql("SELECT count(1) FROM $databaseName.sqlite_master WHERE type='view' AND tbl_name IN $inClaus")
    } else {
        execIntResultSql("SELECT count(1) FROM sqlite_master WHERE type='view' AND tbl_name IN $inClaus")
    }

    return viewCount == viewNames.size
}

internal suspend fun Transactor.execIntResultSql(sql: String, columnIndex: Int = 0): Int? {
    this.usePrepared(sql) { statement ->
        if (statement.step()) {
            return@usePrepared statement.getInt(columnIndex)
        } else {
            Logger.w { "Failed to get Int for [$sql] (returned NO data)" }
        }
    }

    return null
}

internal suspend fun Transactor.execTextResultSql(sql: String, columnIndex: Int = 0): String? {
    this.usePrepared(sql) { statement ->
        if (statement.step()) {
            return@usePrepared statement.getText(columnIndex)
        } else {
            Logger.w { "Failed to get Text for [$sql] (returned NO data)" }
        }
    }

    return null
}

/**
 * Drops view in a database
 *
 * @param viewName Name of view to drop
 */
suspend fun Transactor.dropView(viewName: String) {
    execSQL("DROP VIEW IF EXISTS $viewName")
}

/**
 * Drops all views in a database
 *
 * @param views List of view names... if this list is empty then all views in the database will be dropped
 */
suspend fun Transactor.dropAllViews(views: List<String> = emptyList()) {
    val viewNames: List<String> = views.ifEmpty { findViewNames() }
    viewNames.forEach { dropView(it) }
}

suspend fun Transactor.createView(viewName: String, viewQuery: String) {
    execSQL("CREATE VIEW `$viewName` AS $viewQuery")
}

suspend fun Transactor.createAllViews(views: List<DatabaseViewQuery>) {
    views.forEach { createView(it.viewName, it.viewQuery.trim()) }
}

suspend fun Transactor.recreateView(viewName: String, viewQuery: String) {
    dropView(viewName)
    createView(viewName, viewQuery)
}

/**
 * Drops all existing views and then recreates them in a database
 *
 * @param views List of Views to recreate
 */
suspend fun Transactor.recreateAllViews(views: List<DatabaseViewQuery>) {
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
suspend fun Transactor.applySqlFile(fileSystem: FileSystem, sqlPath: Path): Boolean {
    if (!fileSystem.exists(sqlPath)) {
        // Can't apply if there is no file
        Logger.e { "Failed to apply sql file. File: [$sqlPath] does NOT exist" }
        return false
    }

    immediateTransaction {
        fileSystem.parseAndExecuteSqlStatements(sqlPath) { statement ->
            this@applySqlFile.execSQL(statement)
        }
    }

    return true
}

/**
 * Check to see if a column in a database exists, if it does not... alter query will be run
 * @param tableName table for columnName
 * @param columnName column to from tableName to be checked
 * @param alterSql SQL to be run if the column does not exist.
 * Example: alterTableIfColumnDoesNotExist(database, "individual", "middle_name", "ALTER TABLE individual ADD `middle_name` TEXT DEFAULT '' NOT NULL")
 */
suspend fun Transactor.alterTableIfColumnDoesNotExist(tableName: String, columnName: String, alterSql: String) {
    if (!this.columnExists(tableName, columnName)) {
        Logger.i { "Adding column [$columnName] to table [$tableName]" }
        execSQL(alterSql)
        resetRoom()
    }
}

/**
 * Check to see if a column in a database exists
 * @param tableName table for columnName
 * @param columnName column to from tableName to be checked
 * @return true if the column exists otherwise false
 */
@Suppress("NestedBlockDepth")
suspend fun Transactor.columnExists(tableName: String, columnName: String): Boolean {
    var columnExists = false

    this.usePrepared("PRAGMA table_info($tableName)") { statement: SQLiteStatement ->
        if (statement.step()) {
            do {
                val currentColumn = statement.getText(statement.getColumnIndexOrThrow("name"))
                if (currentColumn == columnName) {
                    columnExists = true
                }
            } while (!columnExists && statement.step())
        } else {
            Logger.w { "Query: [PRAGMA table_info($tableName)] returned NO data" }
        }

    }

    return columnExists
}

/**
 * Get database version
 *
 * @return version of the database
 */
suspend fun Transactor.getDatabaseVersion(): Int {
    return execIntResultSql("PRAGMA user_version") ?: 0
}

/**
 * Set database version
 *
 * @param newVersion version to be set on database
 */
suspend fun Transactor.setDatabaseVersion(newVersion: Int) {
    execSQL("PRAGMA user_version = $newVersion")
}

/**
 * If we make a manual change, then we need to reset room so that it does not fail the validation
 *
 * @param newVersion version to be set on database (default to 0)
 */
suspend fun Transactor.resetRoom(newVersion: Int = 0) {
    execSQL("DROP TABLE IF EXISTS room_master_table")
    setDatabaseVersion(newVersion)
}

/**
 * Check integrity of a database connection using PRAGMA command
 * @return true if integrity_check == "ok"
 */
suspend fun Transactor.isIntegrityOk(): Boolean {
    var resultText: String? = ""
    this.usePrepared("PRAGMA integrity_check") { statement: SQLiteStatement ->
        resultText = if (statement.step()) {
            statement.getText(0)
        } else {
            null
        }
    }

    return when(resultText) {
        null -> {
            Logger.w { "Query: [PRAGMA integrity_check] FAILED (returned NO data)" }
            false
        }
        "ok" -> true
        else -> {
            Logger.e { "Query: [PRAGMA integrity_check] returned: [$resultText]" }
            false
        }
    }
}
