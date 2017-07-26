package org.dbtools.android.room.jdbc

import android.content.ContentResolver
import android.database.CharArrayBuffer
import android.database.ContentObserver
import android.database.Cursor
import android.database.DataSetObserver
import android.net.Uri
import android.os.Bundle

import java.math.BigDecimal
import java.sql.ResultSet
import java.sql.SQLException
import java.util.ArrayList

class JdbcMemoryCursor : Cursor {
    private var currentPosition = -1
    private val data: MutableList<List<Any>>
    private var columnCount: Int = 0
    private var columnNames = arrayOf<String>()
    private var closed = false // state variable

    constructor(resultSet: ResultSet) {
        try {
            data = mutableListOf()
            readMetaData(resultSet)
            readData(resultSet)
            resultSet.close()
        } catch (e: SQLException) {
            throw IllegalStateException(e)
        }

    }

    @Suppress("unused")
    constructor(columnNames: Array<String>, data: MutableList<List<Any>>) {
        this.columnNames = columnNames
        this.columnCount = columnNames.size
        this.data = data
    }

    @Throws(SQLException::class)
    private fun readMetaData(resultSet: ResultSet) {
        val metaData = resultSet.metaData
        columnCount = metaData.columnCount
        columnNames = Array(columnCount) { "" }

        for (i in 0..columnCount - 1) {
            val sqlIndex = i + 1
            columnNames[i] = metaData.getColumnName(sqlIndex)
        }
    }

    @Throws(SQLException::class)
    private fun readData(resultSet: ResultSet) {
        while (resultSet.next()) {
            val rowData = (0..columnCount - 1)
                    .map { it + 1 }
                    .mapTo(ArrayList<Any>()) { resultSet.getObject(it) }
            data.add(rowData)
        }
    }

    override fun getCount(): Int {
        return data.size
    }

    override fun getPosition(): Int {
        return currentPosition
    }

    override fun move(offset: Int): Boolean {
        return moveToPosition(currentPosition + offset)
    }

    override fun moveToPosition(position: Int): Boolean {
        val count = count
        if (position < 0) {
            currentPosition = -1
        } else if (position >= count) {
            currentPosition = count
        } else {
            currentPosition = position
        }
        return currentPosition in 0..(count - 1)
    }

    override fun moveToFirst(): Boolean {
        return moveToPosition(0)
    }

    override fun moveToLast(): Boolean {
        return moveToPosition(count - 1)
    }

    override fun moveToNext(): Boolean {
        return move(1)
    }

    override fun moveToPrevious(): Boolean {
        return move(-1)
    }

    override fun isFirst(): Boolean {
        return currentPosition == 0 && currentPosition < count
    }

    override fun isLast(): Boolean {
        return currentPosition >= 0 && currentPosition == count - 1
    }

    override fun isBeforeFirst(): Boolean {
        if (count == 0) {
            return true
        }
        return currentPosition == -1
    }

    override fun isAfterLast(): Boolean {
        if (count == 0) {
            return true
        }
        return currentPosition == count
    }

    override fun getColumnIndex(columnName: String): Int {
        return (0..columnCount - 1).firstOrNull { columnNames[it].equals(columnName, ignoreCase = true) } ?: -1
    }

    @Throws(IllegalArgumentException::class)
    override fun getColumnIndexOrThrow(columnName: String): Int {
        val index = getColumnIndex(columnName)
        if (index < 0) {
            throw IllegalArgumentException("Cannot find column [$columnName]")
        }

        return index
    }

    override fun getColumnName(index: Int): String {
        return columnNames[index]
    }

    override fun getColumnNames(): Array<String> {
        return columnNames
    }

    override fun getColumnCount(): Int {
        return columnCount
    }

    private val rowData: List<Any?>
        get() = data[currentPosition]

    override fun getBlob(i: Int): ByteArray {
        return rowData[i] as ByteArray
    }

    override fun getString(i: Int): String {
        return rowData[i] as String
    }

    override fun copyStringToBuffer(i: Int, charArrayBuffer: CharArrayBuffer) {

    }

    override fun getShort(i: Int): Short {
        return rowData[i] as Short
    }

    override fun getInt(i: Int): Int {
        return rowData[i] as Int
    }

    override fun getLong(i: Int): Long {
        val data = rowData[i]
        if (data is Int) {
            return (rowData[i] as Int).toLong()
        } else {
            return rowData[i] as Long
        }
    }

    override fun getFloat(i: Int): Float {
        val data = rowData[i]

        if (data is Double) {
            return BigDecimal(rowData[i] as Double).toFloat()
        } else {
            return rowData[i] as Float
        }
    }

    override fun getDouble(i: Int): Double {
        return rowData[i] as Double
    }

    override fun getType(i: Int): Int {
        return getInt(i)
    }

    override fun isNull(i: Int): Boolean {
        return rowData[i] == null
    }

    @Deprecated(message = "Use Close", replaceWith = ReplaceWith("close()"), level = DeprecationLevel.ERROR)
    override fun deactivate() {
        throw UnsupportedOperationException("unsupported")
    }

    @Deprecated(message = "Create a new Cursor", level = DeprecationLevel.ERROR, replaceWith = ReplaceWith(""))
    override fun requery(): Boolean {
        throw UnsupportedOperationException("unsupported")
    }

    override fun close() {
        closed = true
    }

    override fun isClosed(): Boolean {
        return closed
    }

    override fun registerContentObserver(contentObserver: ContentObserver) {

    }

    override fun unregisterContentObserver(contentObserver: ContentObserver) {

    }

    override fun registerDataSetObserver(dataSetObserver: DataSetObserver) {

    }

    override fun unregisterDataSetObserver(dataSetObserver: DataSetObserver) {

    }

    override fun setNotificationUri(contentResolver: ContentResolver, uri: Uri) {

    }

    override fun getNotificationUri(): Uri? {
        return null
    }

    override fun getWantsAllOnMoveCalls(): Boolean {
        return true
    }

    override fun setExtras(extras: Bundle) {

    }

    override fun getExtras(): Bundle {
        throw UnsupportedOperationException("unsupported")
    }

    override fun respond(bundle: Bundle): Bundle {
        throw UnsupportedOperationException("unsupported")
    }
}