@file:Suppress("unused", "DuplicatedCode")

package org.dbtools.room.ext

import androidx.room.Room
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteStatement
import androidx.sqlite.execSQL
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
fun SQLiteConnection.validateDatabase(tag: String = "", tableDataCountCheck: String? = null, allowZeroCount: Boolean = true): Boolean {
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
            prepare("SELECT count(1) FROM $tableDataCountCheck").use { statement ->
                val count = if (statement.step()) {
                    statement.getInt(0)
                } else {
                    null
                }

                if (count == null || (!allowZeroCount && count == 0)) {
                    Logger.e { "validateDatabase - table [$tableDataCountCheck] is BLANK for database [$tag] is blank" }
                    return false
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
fun SQLiteConnection.attachDatabase(toDatabasePath: String, toDatabaseName: String) {
    val sql = "ATTACH DATABASE '$toDatabasePath' AS $toDatabaseName"
    execSQL(sql)
}

/**
 * Detach a database
 * @param databaseName Alias name for attached database
 */
fun SQLiteConnection.detachDatabase(databaseName: String) {
    val sql = "DETACH DATABASE '$databaseName'"
    execSQL(sql)
}

/**
 * Get list each database attached to the current database connection.
 *
 * @return List of AttachedDatabaseInfo containing the database name and file path
 */
fun SQLiteConnection.getAttachedDatabases(): List<AttachedDatabaseInfo> {
    val databaseInfoList = mutableListOf<AttachedDatabaseInfo>()

    prepare("SELECT * FROM pragma_database_list").use { statement ->
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
fun SQLiteConnection.findTableNames(databaseName: String = ""): List<String> {
    val statement = if (databaseName.isNotBlank()) {
        prepare("SELECT tbl_name FROM $databaseName.sqlite_master where type='table'")
    } else {
        prepare("SELECT tbl_name FROM sqlite_master where type='table'")
    }


    val tableNames = mutableListOf<String>()
    statement.use {
        while (it.step()) {
            tableNames.add(statement.getText(0))
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
fun SQLiteConnection.tableExists(tableName: String, databaseName: String = ""): Boolean {
    return tablesExists(listOf(tableName), databaseName)
}

/**
 * Determines if tables exist
 * @param tableNames Table names to check for
 * @param databaseName Alias name for database (such as an attached database) (optional)
 *
 * @return true If ALL tableNames exist
 */
fun SQLiteConnection.tablesExists(tableNames: List<String>, databaseName: String = ""): Boolean {
    val inClaus = tableNames.joinToString(",", prefix = "(", postfix = ")") { "'$it'" }

    val statement = if (databaseName.isNotBlank()) {
        prepare("SELECT count(1) FROM $databaseName.sqlite_master WHERE type='table' AND tbl_name IN $inClaus")
    } else {
        prepare("SELECT count(1) FROM sqlite_master WHERE type='table' AND tbl_name IN $inClaus")
    }

    var tableCount = 0
    statement.use {
        while (it.step()) {
            tableCount = it.getInt(0)
        }
    }

    return tableCount == tableNames.size
}

/**
 * Find names of views in this database
 * @param databaseName Alias name for database (such as an attached database) (optional)
 */
fun SQLiteConnection.findViewNames(databaseName: String = ""): List<String> {
    val statement = if (databaseName.isNotBlank()) {
        prepare("SELECT tbl_name FROM $databaseName.sqlite_master where type='view'")
    } else {
        prepare("SELECT tbl_name FROM sqlite_master where type='view'")
    }

    val viewNames = mutableListOf<String>()
    statement.use {
        while (it.step()) {
            viewNames.add(it.getText(0))
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
fun SQLiteConnection.viewExists(viewName: String, databaseName: String = ""): Boolean {
    return viewExists(listOf(viewName), databaseName)
}

/**
 * Determines if views exist
 * @param viewNames View names to check for
 * @param databaseName Alias name for database (such as an attached database) (optional)
 *
 * @return true If ALL viewNames exist
 */
fun SQLiteConnection.viewExists(viewNames: List<String>, databaseName: String = ""): Boolean {
    val inClaus = viewNames.joinToString(",", prefix = "(", postfix = ")") { "'$it'" }

    val statement = if (databaseName.isNotBlank()) {
        prepare("SELECT count(1) FROM $databaseName.sqlite_master WHERE type='view' AND tbl_name IN $inClaus")
    } else {
        prepare("SELECT count(1) FROM sqlite_master WHERE type='view' AND tbl_name IN $inClaus")
    }

    var viewCount = 0
    statement.use {
        while (it.step()) {
            viewCount = it.getInt(0)
        }
    }

    return viewCount == viewNames.size
}

internal fun SQLiteConnection.execIntResultSql(sql: String, columnIndex: Int = 0): Int? {
    return this.prepare(sql).use { statement ->
        if (statement.step()) {
            statement.getInt(columnIndex)
        } else {
            Logger.w { "Failed to get Int for [$sql] (returned NO data)" }
            null
        }
    }
}

internal fun SQLiteConnection.execTextResultSql(sql: String, columnIndex: Int = 0): String? {
    return this.prepare(sql).use { statement ->
        if (statement.step()) {
            statement.getText(columnIndex)
        } else {
            Logger.w { "Failed to get Text for [$sql] (returned NO data)" }
            null
        }
    }
}

/**
 * Drops view in a database
 *
 * @param viewName Name of view to drop
 */
fun SQLiteConnection.dropView(viewName: String) {
    execSQL("DROP VIEW IF EXISTS $viewName")
}

/**
 * Drops all views in a database
 *
 * @param views List of view names... if this list is empty then all views in the database will be dropped
 */
fun SQLiteConnection.dropAllViews(views: List<String> = emptyList()) {
    val viewNames: List<String> = views.ifEmpty { findViewNames() }
    viewNames.forEach { dropView(it) }
}

fun SQLiteConnection.createView(viewName: String, viewQuery: String) {
    execSQL("CREATE VIEW `$viewName` AS $viewQuery")
}

fun SQLiteConnection.createAllViews(views: List<DatabaseViewQuery>) {
    views.forEach { createView(it.viewName, it.viewQuery.trim()) }
}

fun SQLiteConnection.recreateView(viewName: String, viewQuery: String) {
    dropView(viewName)
    createView(viewName, viewQuery)
}

/**
 * Drops all existing views and then recreates them in a database
 *
 * @param views List of Views to recreate
 */
fun SQLiteConnection.recreateAllViews(views: List<DatabaseViewQuery>) {
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
suspend fun SQLiteConnection.applySqlFile(fileSystem: FileSystem, sqlPath: Path): Boolean {
    if (!fileSystem.exists(sqlPath)) {
        // Can't apply if there is no file
        Logger.e { "Failed to apply sql file. File: [$sqlPath] does NOT exist" }
        return false
    }
    
    return runInTransaction {
        fileSystem.parseAndExecuteSqlStatements(sqlPath) { statement ->
            this.execSQL(statement)
        }
    }
}

/**
 * Check to see if a column in a database exists, if it does not... alter query will be run
 * @param tableName table for columnName
 * @param columnName column to from tableName to be checked
 * @param alterSql SQL to be run if the column does not exist.
 * Example: alterTableIfColumnDoesNotExist(database, "individual", "middle_name", "ALTER TABLE individual ADD `middle_name` TEXT DEFAULT '' NOT NULL")
 */
fun SQLiteConnection.alterTableIfColumnDoesNotExist(tableName: String, columnName: String, alterSql: String) {
    if (!this.columnExists(tableName, columnName)) {
        Logger.i { "Adding column [$columnName] to table [$tableName]" }
        execSQL(alterSql)
    }
}

/**
 * Check to see if a column in a database exists
 * @param tableName table for columnName
 * @param columnName column to from tableName to be checked
 * @return true if the column exists otherwise false
 */
@Suppress("NestedBlockDepth")
fun SQLiteConnection.columnExists(tableName: String, columnName: String): Boolean {
    var columnExists = false

    this.prepare("PRAGMA table_info($tableName)").use { statement: SQLiteStatement ->
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
fun SQLiteConnection.getDatabaseVersion(): Int {
    return execIntResultSql("PRAGMA user_version") ?: 0
}

/**
 * Set database version
 *
 * @param newVersion version to be set on database
 */
fun SQLiteConnection.setDatabaseVersion(newVersion: Int) {
    execSQL("PRAGMA user_version = $newVersion")
}

/**
 * If we make a manual change, then we need to reset room so that it does not fail the validation
 *
 * @param newVersion version to be set on database (default to 0)
 */
fun SQLiteConnection.resetRoom(newVersion: Int = 0) {
    execSQL("DROP TABLE IF EXISTS room_master_table")
    setDatabaseVersion(newVersion)
}

/**
 * Begin Transaction
 */
fun SQLiteConnection.beginTransaction() {
    execSQL("BEGIN IMMEDIATE TRANSACTION")
}

/**
 * End Transaction
 */
fun SQLiteConnection.endTransaction() {
    execSQL("END TRANSACTION")
}

/**
 * Rollback Transaction
 */
fun SQLiteConnection.rollbackTransaction() {
    execSQL("ROLLBACK TRANSACTION")
}

/**
 * Check integrity of a database connection using PRAGMA command
 * @return true if integrity_check == "ok"
 */
fun SQLiteConnection.isIntegrityOk(): Boolean {
    var resultText: String? = ""
    this.prepare("PRAGMA integrity_check").use { statement: SQLiteStatement ->
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

/**
 * Executes the specified block in a database transaction. The transaction will be
 * marked as successful unless an exception is thrown in the block.
 */
@Suppress("TooGenericExceptionCaught")
inline fun SQLiteConnection.runInTransaction(block: () -> Unit): Boolean {
    beginTransaction()
    return try {
        block()
        endTransaction()
        true
    } catch (transactionException: Throwable) {
        Logger.i(transactionException) { "Failed to execute transaction.  Message: ${transactionException.message}" }
        rollbackTransaction()
        false
    }
}

/**
 * If the database should NOT have a migration and is a pre-populated database that should not be managed by Room... make sure Room migration is never needed.
 *
 * NOTE: this SHOULD be called BEFORE room has a chance to open the database and verify the database
 *
 * Example Usage:
 *     val driver = BundledSQLiteDriver()
 *     val connection = driver.open(mySqliteDbFileName)
 *     connection.checkAndFixRoomIdentityHash(MyDatabase.DATABASE_VERSION, MyDatabase.ROOM_DATABASE_IDENTITY_HASH)
 *     connection.close()
 *
 * OR Example using SQLiteDriverExt
 *     val driver = BundledSQLiteDriver()
 *     driver.checkAndFixRoomIdentityHash(mySqliteDbFileName, MyDatabase.DATABASE_VERSION, MyDatabase.ROOM_DATABASE_IDENTITY_HASH)
 *
 * @param expectedVersion SQLite Database version (PRAGMA user_version)
 * @param expectedIdentityHash Hash that is expected.  If the expectedIdentityHash does not match the existing identity hash (currently in the room_master_table), then just delete the table
 * @return true if the identity hash was changed
 */
fun SQLiteConnection.checkAndFixRoomIdentityHash(expectedVersion: Int, expectedIdentityHash: String): Boolean {
    if (expectedIdentityHash.isBlank()) {
        Logger.e { "checkAndFixRoomIdentityHash -- expectedIdentityHash is blank" }
        return false
    }

    // set database version at PRAGMA level (user_version)
    val actualDatabaseVersion = getDatabaseVersion()
    if (actualDatabaseVersion != expectedVersion) {
        setDatabaseVersion(expectedVersion)
    }

    // check if we already have the correct identity hash in the room_master_table
    if (tableExists(Room.MASTER_TABLE_NAME) && findRoomIdentityHash() == expectedIdentityHash) {
        // we are OK
        return false
    }

    Logger.i { "checkAndFixRoomIdentityHash -- updating expectedIdentityHash from [$actualDatabaseVersion] to [$expectedIdentityHash]" }
    // set database version at for Room (room_master_table)
    runInTransaction {
        execSQL("CREATE TABLE IF NOT EXISTS ${Room.MASTER_TABLE_NAME} (id INTEGER PRIMARY KEY,identity_hash TEXT)")
        execSQL("INSERT OR REPLACE INTO ${Room.MASTER_TABLE_NAME} (id,identity_hash) VALUES(42, '$expectedIdentityHash')")
    }

    return true
}

/**
 * Find the Room Identity Hash
 * Note: if you are not sure if the room_master_table exists, check first with tableExists(database, "room_master_table")
 *
 * @return identity_hash for this database OR null if it does exist
 */
fun SQLiteConnection.findRoomIdentityHash(): String? {
    // NOTE: the id column for this table always seems to be 42 and there is always only 1 row... so lets just find the first row
    return execTextResultSql("SELECT identity_hash FROM room_master_table LIMIT 1")
}

fun SQLiteConnection.execSQLWithArgs(sql: String, args: List<Any?>) {
    this.prepare(sql).use { statement ->
        statement.bindArgs(args)
        statement.step() // Execute the statement
    }
}