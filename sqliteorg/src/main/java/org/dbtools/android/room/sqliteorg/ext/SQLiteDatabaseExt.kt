@file:Suppress("unused")

package org.dbtools.android.room.sqliteorg.ext

import org.dbtools.android.room.sqliteorg.SqliteOrgDatabaseUtil
import org.sqlite.database.sqlite.SQLiteDatabase

/**
 * Check to see if a column in a database exists, if it does not... alter query will be run
 * @param tableName table for columnName
 * @param columnName column to from tableName to be checked
 * @param alterSql SQL to be run if the column does not exits. Example: alterTableIfColumnDoesNotExist(database, "individual", "middle_name", "ALTER TABLE individual ADD `middle_name` TEXT DEFAULT '' NOT NULL")
 */
fun SQLiteDatabase.alterTableIfColumnDoesNotExist(tableName: String, columnName: String, alterSql: String) {
    SqliteOrgDatabaseUtil.alterTableIfColumnDoesNotExist(this, tableName, columnName, alterSql)
}

/**
 * Check to see if a column in a database exists
 * @param tableName table for columnName
 * @param columnName column to from tableName to be checked
 * @return true if the column exists otherwise false
 */
fun SQLiteDatabase.columnExists(tableName: String, columnName: String): Boolean {
    return SqliteOrgDatabaseUtil.columnExists(this, tableName, columnName)
}

/**
 * If we make a manual change, then we need to reset room so that it does not fail the validation
 *
 * @param newVersion version to be set on database (default to 0)
 */
fun SQLiteDatabase.resetRoom(newVersion: Int = 0) {
    SqliteOrgDatabaseUtil.resetRoom(this, newVersion)
}
