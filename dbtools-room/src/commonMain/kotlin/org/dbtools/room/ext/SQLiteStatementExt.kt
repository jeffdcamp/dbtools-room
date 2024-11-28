package org.dbtools.room.ext

import androidx.sqlite.SQLiteStatement

/**
 * Returns the zero-based index for the given column name, or throws [IllegalArgumentException] if
 * the column doesn't exist.
 *
 * NOTE Room StatementUtil.kt contains this function, but it is marked INTERNAL
 *
 * @param name Name of column
 * @return index of column
 */
fun SQLiteStatement.getColumnIndexOrThrow(name: String): Int {
    val index = getColumnNames().indexOfFirst { it == name } // Room code uses internal: stmt.columnIndexOf(name)
    if (index >= 0) {
        return index
    }
    val availableColumns = List(getColumnCount()) { getColumnName(it) }.joinToString()
    throw IllegalArgumentException("Column '$name' does not exist. Available columns: [$availableColumns]")
}