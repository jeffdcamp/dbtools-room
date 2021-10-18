package org.dbtools.android.room.ext

import android.database.Cursor

/**
 * Set of extension functions on Cursor to make it simpler to get and test data
 *
 * Example:
 *
 *  cursor.assertString("status", "ACTIVE")
 *  cursor.assertBoolean("dirty", false)
 */
fun Cursor.requireColumnIndex(columnName: String): Int {
    val columnIndex = getColumnIndex(columnName)
    require(columnIndex >= 0) { "columnName [$columnName] could not be found"}
    return columnIndex
}

fun Cursor.getBoolean(columnName: String): Boolean = getInt(requireColumnIndex(columnName)) != 0
fun Cursor.getInt(columnName: String): Int = getInt(requireColumnIndex(columnName))
fun Cursor.getDouble(columnName: String): Double = getDouble(requireColumnIndex(columnName))
fun Cursor.getFloat(columnName: String): Float = getFloat(requireColumnIndex(columnName))
fun Cursor.getString(columnName: String): String? = getString(requireColumnIndex(columnName))

fun Cursor.assertBoolean(columnName: String, expectedValue: Boolean) = assert(getBoolean(columnName) == expectedValue) { "expected: $expectedValue\nactual:   ${getBoolean(columnName)}"}
fun Cursor.assertInt(columnName: String, expectedValue: Int?) = assert(getInt(columnName) == expectedValue) { "expected: $expectedValue\nactual:   ${getInt(columnName)}"}
fun Cursor.assertDouble(columnName: String, expectedValue: Double?) = assert(getDouble(columnName) == expectedValue) { "expected: $expectedValue\nactual:   ${getDouble(columnName)}"}
fun Cursor.assertFloat(columnName: String, expectedValue: Float?) = assert(getFloat(columnName) == expectedValue) { "expected: $expectedValue\nactual:   ${getFloat(columnName)}"}
fun Cursor.assertString(columnName: String, expectedValue: String?) = assert(getString(columnName) == expectedValue) { "expected: $expectedValue\nactual:   ${getString(columnName)}"}