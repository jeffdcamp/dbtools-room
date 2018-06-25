package org.dbtools.android.room.ext

import android.arch.persistence.db.SupportSQLiteDatabase
import org.dbtools.android.room.util.MergeDatabaseUtil
import java.io.File

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
    if (tableNamesCursor.moveToFirst()) {
        do {
            tableNames.add(tableNamesCursor.getString(0))
        } while (tableNamesCursor.moveToNext())
    }
    tableNamesCursor.close()

    return tableNames
}

/**
 * Merge database tables from other databases.
 *
 * By default all tables (except Room system tables) will be merged)
 *
 * @param fromDatabaseFile Sqlite file that will be opened and attached to this database... then data will be copied from this database File
 * @param includeTables Only table names in this list will be merged.  default: emptyList()
 * @param excludeTables All tables except the table names in this list will be merged.  default: emptyList()
 * @param mergeBlock Code to execute to perform merge.  default: database.execSQL("INSERT OR IGNORE INTO $tableName SELECT * FROM $sourceTableName")
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
fun SupportSQLiteDatabase.mergeDatabase(fromDatabaseFile: File, includeTables: List<String> = emptyList(), excludeTables: List<String> = emptyList()): Boolean {
    return MergeDatabaseUtil.mergeDatabase(this, fromDatabaseFile, includeTables, excludeTables)
}
