@file:Suppress("unused")

package org.dbtools.android.room.ext

import android.util.Pair
import androidx.sqlite.db.SupportSQLiteDatabase
import co.touchlab.kermit.Logger
import org.dbtools.android.room.DatabaseViewQuery
import org.dbtools.android.room.util.DatabaseUtil
import org.dbtools.android.room.util.MergeDatabaseUtil
import java.io.File

/**
 * Preform a PRAGMA check on the database and optionally check a table for existing data
 *
 * @param databaseNameTag Optional tag name to help identify database in logging
 * @param tableDataCountCheck Optional check on a table for data. (optional)
 * @param allowZeroCount Optional tableDataCountCheck if false return false if count is zero
 *
 * @return true if validation check is OK
 */
fun SupportSQLiteDatabase.validateDatabaseFile(databaseNameTag: String = "", tableDataCountCheck: String = "", allowZeroCount: Boolean = true): Boolean {
    return DatabaseUtil.validateDatabaseFile(this, databaseNameTag, tableDataCountCheck, allowZeroCount)
}

/**
 * Attach a database
 * @param toDatabasePath Path to attach database
 * @param toDatabaseName Alias name for attached database
 */
fun SupportSQLiteDatabase.attachDatabase(toDatabasePath: String, toDatabaseName: String) {
    val sql = "ATTACH DATABASE '$toDatabasePath' AS $toDatabaseName"
    execSQL(sql)
}

/**
 * Detach a database
 * @param databaseName Alias name for attached database
 */
fun SupportSQLiteDatabase.detachDatabase(databaseName: String) {
    val sql = "DETACH DATABASE '$databaseName'"
    execSQL(sql)
}

/**
 * List each database attached to the current database connection.
 *      first column (name) - "main" for the main database file, "temp" for the database file used to store TEMP objects, or the name of the ATTACHed database for other database files.
 *      second column (file) - name of the database file itself, or an empty string if the database is not associated with a file.
 * @return ArrayList of pairs of (database name, database file path) or null if the database is not open.
 */
fun SupportSQLiteDatabase.getAttachedDatabases(): List<Pair<String, String>>? {
    return attachedDbs
}

/**
 * Find names of tables in this database
 * @param databaseName Alias name for database (such as an attached database) (optional)
 */
fun SupportSQLiteDatabase.findTableNames(databaseName: String = ""): List<String> {
    val tableNamesCursor = if (databaseName.isNotBlank()) {
        query("SELECT tbl_name FROM $databaseName.sqlite_master where type='table'")
    } else {
        query("SELECT tbl_name FROM sqlite_master where type='table'")
    }

    val tableNames = ArrayList<String>(tableNamesCursor.count)
    tableNamesCursor.use {
        if (it.moveToFirst()) {
            do {
                tableNames.add(it.getString(0))
            } while (it.moveToNext())
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
fun SupportSQLiteDatabase.tableExists(tableName: String, databaseName: String = ""): Boolean {
    return tablesExists(listOf(tableName), databaseName)
}

/**
 * Determines if tables exist
 * @param tableNames Table names to check for
 * @param databaseName Alias name for database (such as an attached database) (optional)
 *
 * @return true If ALL tableNames exist
 */
fun SupportSQLiteDatabase.tablesExists(tableNames: List<String>, databaseName: String = ""): Boolean {
    val inClaus = tableNames.joinToString(",", prefix = "(", postfix = ")") { "'$it'" }

    val tableNamesCursor = if (databaseName.isNotBlank()) {
        query("SELECT count(1) FROM $databaseName.sqlite_master WHERE type='table' AND tbl_name IN $inClaus")
    } else {
        query("SELECT count(1) FROM sqlite_master WHERE type='table' AND tbl_name IN $inClaus")
    }

    var tableCount = 0
    tableNamesCursor.use {
        if (it.moveToFirst()) {
            do {
                tableCount = it.getInt(0)
            } while (it.moveToNext())
        }
    }

    return tableCount == tableNames.size
}

/**
 * Find names of views in this database
 * @param databaseName Alias name for database (such as an attached database) (optional)
 */
fun SupportSQLiteDatabase.findViewNames(databaseName: String = ""): List<String> {
    val viewNamesCursor = if (databaseName.isNotBlank()) {
        query("SELECT tbl_name FROM $databaseName.sqlite_master where type='view'")
    } else {
        query("SELECT tbl_name FROM sqlite_master where type='view'")
    }

    val viewNames = ArrayList<String>(viewNamesCursor.count)
    viewNamesCursor.use {
        if (it.moveToFirst()) {
            do {
                viewNames.add(it.getString(0))
            } while (it.moveToNext())
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
fun SupportSQLiteDatabase.viewExists(viewName: String, databaseName: String = ""): Boolean {
    return viewExists(listOf(viewName), databaseName)
}

/**
 * Determines if views exist
 * @param viewNames View names to check for
 * @param databaseName Alias name for database (such as an attached database) (optional)
 *
 * @return true If ALL viewNames exist
 */
fun SupportSQLiteDatabase.viewExists(viewNames: List<String>, databaseName: String = ""): Boolean {
    val inClaus = viewNames.joinToString(",", prefix = "(", postfix = ")") { "'$it'" }

    val viewNamesCursor = if (databaseName.isNotBlank()) {
        query("SELECT count(1) FROM $databaseName.sqlite_master WHERE type='view' AND tbl_name IN $inClaus")
    } else {
        query("SELECT count(1) FROM sqlite_master WHERE type='view' AND tbl_name IN $inClaus")
    }

    var viewCount = 0
    viewNamesCursor.use {
        if (it.moveToFirst()) {
            do {
                viewCount = it.getInt(0)
            } while (it.moveToNext())
        }
    }

    return viewCount == viewNames.size
}

/**
 * Drops view in a database
 *
 * @param viewName Name of view to drop
 */
fun SupportSQLiteDatabase.dropView(viewName: String) {
    DatabaseUtil.dropView(this, viewName)
}

/**
 * Drops all views in a database
 *
 * @param views List of view names... if this list is empty then all views in the database will be dropped
 */
fun SupportSQLiteDatabase.dropAllViews(views: List<String> = emptyList()) {
    DatabaseUtil.dropAllViews(this, views)
}

fun SupportSQLiteDatabase.createView(viewName: String, viewQuery: String) {
    DatabaseUtil.createView(this, viewName, viewQuery)
}

fun SupportSQLiteDatabase.createAllViews(views: List<DatabaseViewQuery>) {
    DatabaseUtil.createAllViews(this, views)
}

fun SupportSQLiteDatabase.recreateView(viewName: String, viewQuery: String) {
    DatabaseUtil.recreateView(this, viewName, viewQuery)
}

/**
 * Drops all existing views and then recreates them in a database
 *
 * @param views List of Views to recreate
 */
fun SupportSQLiteDatabase.recreateAllViews(views: List<DatabaseViewQuery>) {
    DatabaseUtil.recreateAllViews(this, views)
}

/**
 * Merge database tables from other databases.
 *
 * By default all tables (except Room system tables) will be merged)
 *
 * @param fromDatabaseFile Sqlite file that will be opened and attached to this database... then data will be copied from this database File
 * @param includeTables Only table names in this list will be merged. Table names are source database table names.  default: emptyList()
 * @param excludeTables All tables except the table names in this list will be merged. Table names are source database table names.  default: emptyList()
 * @param tableNameMap Map of name changes in target database (Example: copy table data from databaseA.foo to databaseB.bar).  Key is the source table name, value is the target table name
 * @param onFailBlock Code to execute if there is a failure during merge
 * @param mergeBlock Code to execute to perform merge.  default: database.execSQL("INSERT OR IGNORE INTO $tableName SELECT * FROM $sourceTableName")
 *
 * @return true if merge was successful
 *
 * NOTE:  Room system tables are automatically excluded from the merge
 *
 *
 * Add the following function to the RoomDatabase class (mDatabase is ONLY accessible from inside the RoomDatabase class)
 *     fun mergeDataFromOtherDatabase(fromDatabaseFile: File, includeTables: List<String> = emptyList(), excludeTables: List<String> = emptyList()) {
 *         // make sure database is open
 *         if (!isOpen) {
 *             openHelper.writableDatabase
 *         }
 *
 *         // merge database
 *         mDatabase.mergeDatabase(fromDatabaseFile, includeTables, excludeTables)
 *     }
 */
fun SupportSQLiteDatabase.mergeDatabase(
    fromDatabaseFile: File,
    includeTables: List<String> = emptyList(),
    excludeTables: List<String> = emptyList(),
    tableNameMap: Map<String, String> = emptyMap(),
    onFailBlock: ((e: Exception, targetDatabase: SupportSQLiteDatabase, sourceDatabaseFile: File) -> Unit)? = null,
    mergeBlock: (database: SupportSQLiteDatabase, sourceTableName: String, targetTableName: String) -> Unit = { database, sourceTableName, targetTableName ->
        MergeDatabaseUtil.defaultMerge(database, sourceTableName, targetTableName)
    }
): Boolean {
    return MergeDatabaseUtil.mergeDatabase(this, fromDatabaseFile, includeTables, excludeTables, tableNameMap, onFailBlock, mergeBlock)
}

/**
 * Apply many SQL statements from a file. File must contain SQL statements separated by ;
 * All statements are executed in a single transaction
 *
 * @param sqlFile File containing statements
 *
 * @return true If all SQL statements successfully were applied
 */
fun SupportSQLiteDatabase.applySqlFile(sqlFile: File): Boolean {
    if (!sqlFile.exists()) {
        // Can't apply if there is no file
        Logger.e { "Failed to apply sql file. File: [${sqlFile.absolutePath}] does NOT exist" }
        return false
    }

    try {
        beginTransaction()

        sqlFile.parseAndExecuteSqlStatements { statement ->
            this.execSQL(statement)
        }

        setTransactionSuccessful()
    } catch (expected: Exception) {
        Logger.e(expected) { "Failed to apply sql file. File: [${sqlFile.absolutePath}] Error: [${expected.message}]" }
        return false
    } finally {
        endTransaction()
    }
    return true
}

/**
 * Check to see if a column in a database exists, if it does not... alter query will be run
 * @param tableName table for columnName
 * @param columnName column to from tableName to be checked
 * @param alterSql SQL to be run if the column does not exits.
 * Example: alterTableIfColumnDoesNotExist(database, "individual", "middle_name", "ALTER TABLE individual ADD `middle_name` TEXT DEFAULT '' NOT NULL")
 */
fun SupportSQLiteDatabase.alterTableIfColumnDoesNotExist(tableName: String, columnName: String, alterSql: String) {
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
fun SupportSQLiteDatabase.columnExists(tableName: String, columnName: String): Boolean {
    var columnExists = false

    this.query("PRAGMA table_info($tableName)").use { cursor ->
        if (cursor.moveToFirst()) {
            do {
                val currentColumn = cursor.getString(cursor.getColumnIndexOrThrow("name"))
                if (currentColumn == columnName) {
                    columnExists = true
                }
            } while (!columnExists && cursor.moveToNext())
        } else {
            Logger.w { "Query: [PRAGMA table_info($tableName)] returned NO data" }
        }

    }

    return columnExists
}

/**
 * If we make a manual change, then we need to reset room so that it does not fail the validation
 *
 * @param newVersion version to be set on database (default to 0)
 */
fun SupportSQLiteDatabase.resetRoom(newVersion: Int = 0) {
    execSQL("DROP TABLE IF EXISTS room_master_table")
    version = newVersion
}

/**
 * Executes the specified block in a database transaction. The transaction will be
 * marked as successful unless an exception is thrown in the block.
 */
inline fun SupportSQLiteDatabase.runInTransaction(block: () -> Unit) {
    beginTransaction()
    try {
        block()
        setTransactionSuccessful()
    } finally {
        endTransaction()
    }
}

/**
 * Executes the specified suspend block in a database transaction. The transaction will be
 * marked as successful unless an exception is thrown in the suspend block.
 */
suspend fun SupportSQLiteDatabase.withTransaction(block: suspend () -> Unit) {
    beginTransaction()
    try {
        block()
        setTransactionSuccessful()
    } finally {
        endTransaction()
    }
}