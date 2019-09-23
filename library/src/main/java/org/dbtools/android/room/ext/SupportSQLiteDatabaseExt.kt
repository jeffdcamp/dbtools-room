@file:Suppress("unused")

package org.dbtools.android.room.ext

import android.util.Pair
import androidx.sqlite.db.SupportSQLiteDatabase
import org.dbtools.android.room.util.DatabaseUtil
import org.dbtools.android.room.util.MergeDatabaseUtil
import timber.log.Timber
import java.io.File

/**
 * Preform a PRAGMA check on the database and optionally check a table for existing data
 *
 * @param databaseNameTag Optional check on a table for data. (optional)
 * @param databaseNameTag Optional tag name to help identify database in logging
 *
 * @return true if validation check is OK
 */
fun SupportSQLiteDatabase.validDatabaseFile(databaseNameTag: String = "", tableDataCountCheck: String = ""): Boolean {
    return DatabaseUtil.validDatabaseFile(this, databaseNameTag, tableDataCountCheck)
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
fun SupportSQLiteDatabase.getAttachedDatabases(): MutableList<Pair<String, String>>? {
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
 * Merge database tables from other databases.
 *
 * By default all tables (except Room system tables) will be merged)
 *
 * @param fromDatabaseFile Sqlite file that will be opened and attached to this database... then data will be copied from this database File
 * @param includeTables Only table names in this list will be merged. Table names are source database table names.  default: emptyList()
 * @param excludeTables All tables except the table names in this list will be merged. Table names are source database table names.  default: emptyList()
 * @param tableNameMap Map of name changes in target database (Example: copy table data from databaseA.foo to databaseB.bar).  Key is the source table name, value is the target table name
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
    mergeBlock: (database: SupportSQLiteDatabase, sourceTableName: String, targetTableName: String) -> Unit = { database, sourceTableName, targetTableName ->
        MergeDatabaseUtil.defaultMerge(database, sourceTableName, targetTableName)
    }
): Boolean {
    return MergeDatabaseUtil.mergeDatabase(this, fromDatabaseFile, includeTables, excludeTables, tableNameMap, mergeBlock)
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
        Timber.e("Failed to apply sql file. File: [%s] does NOT exist", sqlFile.absolutePath)
        return false
    }

    try {
        beginTransaction()
        var statement = ""
        sqlFile.forEachLine { line ->
            statement += line
            if (statement.endsWith(';')) {
                this.execSQL(statement)
                statement = ""
            } else {
                // If the statement currently does not end with [;] then there must be multiple lines to the full statement.
                // Make sure to keep the newline character (some text columns may have multiple lines of data)
                statement += '\n'
            }
        }
        setTransactionSuccessful()
    } catch (e: Exception) {
        Timber.e(e, "Failed to apply sql file. File: [%s] Error: [%s]", sqlFile.absolutePath, e.message)
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
 * @param alterSql SQL to be run if the column does not exits. Example: alterTableIfColumnDoesNotExist(database, "individual", "middle_name", "ALTER TABLE individual ADD `middle_name` TEXT DEFAULT '' NOT NULL")
 */
fun SupportSQLiteDatabase.alterTableIfColumnDoesNotExist(tableName: String, columnName: String, alterSql: String) {
    if (!this.columnExists(tableName, columnName)) {
        Timber.i("Adding column [$columnName] to table [$tableName]")
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
            Timber.w("Query: [PRAGMA table_info($tableName)] returned NO data")
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