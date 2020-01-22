@file:Suppress("unused")

package org.dbtools.android.room.ext

import android.util.Pair
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import org.dbtools.android.room.util.DatabaseUtil
import org.dbtools.android.room.util.MergeDatabaseUtil
import timber.log.Timber
import java.io.File

/**
 * Preform a PRAGMA check on the database and optionally check a table for existing data
 *
 * @param databaseNameTag Optional check on a table for data. (optional)
 * @param tableDataCountCheck Optional check on a table for data. (optional)
 * @param allowZeroCount Optional tableDataCountCheck if false return false if count is zero
 *
 * @return true if validation check is OK
 */
fun RoomDatabase.validateDatabaseFile(databaseNameTag: String = "", tableDataCountCheck: String = "", allowZeroCount: Boolean = true): Boolean {
    return DatabaseUtil.validateDatabaseFile(this, databaseNameTag, tableDataCountCheck, allowZeroCount)
}

/**
 * Attach a database
 * @param toDatabasePath Path to attach database
 * @param toDatabaseName Alias name for attached database
 */
fun RoomDatabase.attachDatabase(toDatabasePath: String, toDatabaseName: String) {
    val database = openHelper.readableDatabase
    database.attachDatabase(toDatabasePath, toDatabaseName)
}

/**
 * Detach a database
 * @param databaseName Alias name for attached database
 */
fun RoomDatabase.detachDatabase(databaseName: String) {
    val database = openHelper.readableDatabase
    database.detachDatabase(databaseName)
}

/**
 * List each database attached to the current database connection.
 *      first column (name) - "main" for the main database file, "temp" for the database file used to store TEMP objects, or the name of the ATTACHed database for other database files.
 *      second column (file) - name of the database file itself, or an empty string if the database is not associated with a file.
 *
 * @return ArrayList of pairs of (database name, database file path) or null if the database is not open.
 */
fun RoomDatabase.getAttachedDatabases(): MutableList<Pair<String, String>>? {
    return openHelper.readableDatabase.attachedDbs
}

private fun RoomDatabase.getSqliteVersion(): String {
    val cursor = query("select sqlite_version()", null)
    var version = ""
    cursor.use {
        if (it.moveToFirst()) {
            version = it.getString(0)
        }
    }

    return version
}

private fun RoomDatabase.getVersion(): Int {
    return openHelper.readableDatabase.version
}

/**
 * Find names of tables in this database
 */
fun RoomDatabase.findTableNames(): List<String> {
    val tableNamesCursor = query("SELECT tbl_name FROM sqlite_master where type='table'", null)

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
 * Find count of rows for tableName
 * @param tableName Table name
 * @return row count or 0 if not found
 */
fun RoomDatabase.findTableRowCount(tableName: String): Long {
    val cursor = query("SELECT count(1) FROM $tableName", null)
    var rowCount = 0L
    cursor.use {
        if (it.moveToFirst()) {
            rowCount = it.getLong(0)
        }
    }

    return rowCount
}

/**
 * Determines if table exist
 *
 * @param tableName Table name to check for
 * @param databaseName Alias name for database (such as an attached database) (optional)
 *
 * @return true If tableName exist
 */
fun RoomDatabase.tableExists(tableName: String, databaseName: String = ""): Boolean {
    return tablesExists(listOf(tableName), databaseName)
}

/**
 * Determines if tables exist
 *
 * @param tableNames Table names to check for
 * @param databaseName Alias name for database (such as an attached database) (optional)
 *
 * @return true If ALL tableNames exist
 */
fun RoomDatabase.tablesExists(tableNames: List<String>, databaseName: String = ""): Boolean {
    val inClaus = tableNames.joinToString(",", prefix = "(", postfix = ")") { "'$it'" }

    val tableNamesCursor = if (databaseName.isNotBlank()) {
        query("SELECT count(1) FROM $databaseName.sqlite_master WHERE type='table' AND tbl_name IN $inClaus", null)
    } else {
        query("SELECT count(1) FROM sqlite_master WHERE type='table' AND tbl_name IN $inClaus", null)
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
 * Add the following function to the RoomDatabase class (mDatabase is ONLY accessable from inside the RoomDatabase class)
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
fun RoomDatabase.mergeDatabase(
    fromDatabaseFile: File,
    includeTables: List<String> = emptyList(),
    excludeTables: List<String> = emptyList(),
    tableNameMap: Map<String, String> = emptyMap(),
    mergeBlock: (database: SupportSQLiteDatabase, sourceTableName: String, targetTableName: String) -> Unit = { database, sourceTableName, targetTableName ->
        MergeDatabaseUtil.defaultMerge(database, sourceTableName, targetTableName)
    }
): Boolean {
    return MergeDatabaseUtil.mergeDatabase(openHelper.writableDatabase, fromDatabaseFile, includeTables, excludeTables, tableNameMap, mergeBlock)
}

/**
 * Apply many SQL statements from a file. File must contain SQL statements separated by ;
 * All statements are executed in a single transaction
 *
 * @param sqlFile File containing statements
 *
 * @return true If all SQL statements successfully were applied
 */
fun RoomDatabase.applySqlFile(sqlFile: File): Boolean {
    if (!sqlFile.exists()) {
        // Can't apply if there is no file
        Timber.e("Failed to apply sql file. File: [%s] does NOT exist", sqlFile.absolutePath)
        return false
    }

    // get the SupportSQLiteDatabase so that execSQL(statement) may be called
    val database = openHelper.writableDatabase

    try {
        @Suppress("DEPRECATION") // mirroring RoomDatabase.kt
        beginTransaction()

        var statement = ""
        sqlFile.forEachLine { line ->
            // Prepare the line
            // - Remove any trailing comments (sqldiff may add comments to a line (Example: 'DROP TABLE speaker; -- due to schema mismatch'))
            // - Remove any trailing spaces (we check for a line ending with ';')

            // check for sqldiff comment
            val formattedLine = if (line.contains("; -- ")) {
                val lineWithoutComment = line.substringBefore("; -- ") + ";" // be sure to add the ; back in
                lineWithoutComment.trim()
            } else {
                line.trim()
            }

            statement += formattedLine
            if (statement.endsWith(';')) {
                database.execSQL(statement)
                statement = ""
            } else {
                // If the statement currently does not end with [;] then there must be multiple lines to the full statement.
                // Make sure to keep the newline character (some text columns may have multiple lines of data)
                statement += '\n'
            }
        }
        @Suppress("DEPRECATION") // mirroring RoomDatabase.kt
        setTransactionSuccessful()
    } catch (e: Exception) {
        Timber.e(e, "Failed to apply sql file. File: [%s] Error: [%s]", sqlFile.absolutePath, e.message)
        return false
    } finally {
        @Suppress("DEPRECATION") // mirroring RoomDatabase.kt
        endTransaction()
    }
    return true
}