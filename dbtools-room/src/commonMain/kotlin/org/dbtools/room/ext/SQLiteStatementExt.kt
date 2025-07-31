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

/**
 * Binds the provided arguments to the SQLiteStatement.
 *
 * @param args Variable number of arguments to bind to the statement.
 * @throws IllegalArgumentException if an unsupported argument type is encountered.
 */
fun SQLiteStatement.bindArgs(args: List<Any?>) {
    var index = 1
    args.forEach { arg ->
        when (arg) {
            is String -> bindText(index, arg)
            is Int -> bindInt(index, arg)
            is Long -> bindLong(index, arg)
            is Double -> bindDouble(index, arg)
            is Float -> bindFloat(index, arg)
            is Boolean -> bindBoolean(index, arg)
            is ByteArray -> bindBlob(index, arg)
            null -> bindNull(index)
            else -> throw IllegalArgumentException("Unsupported argument type: ${arg::class}")
        }
        index++
    }
}

fun SQLiteStatement.getTextOrNull(index: Int): String? = if (isNull(index)) null else getText(index)
fun SQLiteStatement.getIntOrNull(index: Int): Int? = if (isNull(index)) null else getInt(index)
fun SQLiteStatement.getLongOrNull(index: Int): Long? = if (isNull(index)) null else getLong(index)
fun SQLiteStatement.getDoubleOrNull(index: Int): Double? = if (isNull(index)) null else getDouble(index)
fun SQLiteStatement.getFloatOrNull(index: Int): Float? = if (isNull(index)) null else getFloat(index)
fun SQLiteStatement.getBooleanOrNull(index: Int): Boolean? = if (isNull(index)) null else getBoolean(index)
