package org.dbtools.android.room.ext

import android.arch.persistence.room.RoomDatabase
import org.dbtools.android.room.util.DatabaseUtil

/**
 * Preform a PRAGMA check on the database and optionally check a table for existing data
 *
 * @param databaseNameTag Optional check on a table for data. (optional)
 * @param databaseNameTag Optional tag name to help identify database in logging
 *
 * @return true if validation check is OK
 */
fun RoomDatabase.validDatabaseFile(databaseNameTag: String = "", tableDataCountCheck: String = ""): Boolean {
    return DatabaseUtil.validDatabaseFile(this, databaseNameTag, tableDataCountCheck)
}

/**
 * Attach a database
 * @param toDatabasePath Path to attach database
 * @param toDatabaseName Alias name for attached database
 */
fun RoomDatabase.attachDatabase(toDatabasePath: String, toDatabaseName: String) {
    val sql = "ATTACH DATABASE '$toDatabasePath' AS $toDatabaseName"
    query(sql, null)
}

/**
 * Detach a database
 * @param databaseName Alias name for attached database
 */
fun RoomDatabase.detachDatabase(databaseName: String) {
    val sql = "DETACH DATABASE '$databaseName'"
    query(sql, null)
}

/**
 * Find names of tables in this database
 */
fun RoomDatabase.findTableNames(): List<String> {
    val tableNamesCursor = query("SELECT tbl_name FROM sqlite_master where type='table'", null)

    val tableNames = ArrayList<String>(tableNamesCursor.count)
    if (tableNamesCursor.moveToFirst()) {
        do {
            tableNames.add(tableNamesCursor.getString(0))
        } while (tableNamesCursor.moveToNext())
    }
    tableNamesCursor.close()

    return tableNames
}