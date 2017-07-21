package org.dbtools.android.room.jdbc

import android.content.ContentResolver
import android.database.CharArrayBuffer
import android.database.ContentObserver
import android.database.Cursor
import android.database.DataSetObserver
import android.net.Uri
import android.os.Bundle
import java.sql.ResultSet

@Deprecated(message = "Cannot be used since sqlite jdbc is TYPE_FORWARD_ONLY", replaceWith = ReplaceWith("JdbcMemoryCursor", imports = "org.dbtools.android.room.jdbc"))
class JdbcCursor(private val rs: ResultSet) : Cursor {

    private var extras: Bundle? = null
    private val count: Int

    init {
        rs.last()
        count = rs.row
        rs.beforeFirst()
    }

    override fun getPosition() = rs.row
    override fun moveToFirst() = rs.first()
    override fun moveToLast() = rs.last()
    override fun moveToPrevious() = rs.previous()
    override fun moveToNext() = rs.next()
    override fun moveToPosition(position: Int) = move(position - rs.row)

    override fun move(offset: Int): Boolean {
        return when {
            offset < 0 -> move(Math.abs(offset)) { rs.previous() }
            else -> move(Math.abs(offset)) { rs.next() }
        }
    }

    private fun move(offset: Int, moveOperation: () -> Boolean): Boolean {
        var result = true
        repeat(offset) {
            result = result && moveOperation()
        }
        return result
    }

    override fun getWantsAllOnMoveCalls() = false

    override fun isFirst() = rs.isFirst
    override fun isBeforeFirst() = rs.isBeforeFirst
    override fun isLast() = rs.isLast
    override fun isAfterLast() = rs.isAfterLast

    override fun getBlob(columnIndex: Int): ByteArray = rs.getBytes(columnIndex)
    override fun getDouble(columnIndex: Int) = rs.getDouble(columnIndex)
    override fun getFloat(columnIndex: Int) = rs.getFloat(columnIndex)
    override fun getInt(columnIndex: Int) = rs.getInt(columnIndex)
    override fun getLong(columnIndex: Int) = rs.getLong(columnIndex)
    override fun getShort(columnIndex: Int) = rs.getShort(columnIndex)
    override fun getString(columnIndex: Int): String? = rs.getString(columnIndex)

    override fun isNull(columnIndex: Int) = rs.getObject(columnIndex) == null

    override fun copyStringToBuffer(columnIndex: Int, buffer: CharArrayBuffer?) {
        buffer ?: return
        val string = rs.getString(columnIndex)
        val sizeCopied = Math.min(buffer.data.size, string.length)
        System.arraycopy(string.toCharArray(), 0, buffer, 0, sizeCopied)
        buffer.sizeCopied = sizeCopied
    }


    override fun close() = rs.close()
    override fun isClosed() = rs.isClosed

    override fun getCount() = count


    override fun getColumnCount() = rs.metaData.columnCount
    override fun getColumnIndex(columnName: String?) = rs.findColumn(columnName)
    override fun getColumnIndexOrThrow(columnName: String?) = rs.findColumn(columnName)
    override fun getColumnName(columnIndex: Int): String = rs.metaData.getColumnName(columnIndex)

    override fun getColumnNames(): Array<out String> {
        val metadata = rs.metaData
        return Array(metadata.columnCount) {
            metadata.getColumnName(it + 1) // 1 Based
        }
    }

    override fun getType(columnIndex: Int): Int {
        val metadata = rs.metaData
        val type = metadata.getColumnType(columnIndex)
        return when (type) {
            java.sql.Types.NULL -> Cursor.FIELD_TYPE_NULL

            java.sql.Types.BIT,
            java.sql.Types.TINYINT,
            java.sql.Types.SMALLINT,
            java.sql.Types.INTEGER,
            java.sql.Types.BIGINT,
            java.sql.Types.ROWID -> Cursor.FIELD_TYPE_INTEGER

            java.sql.Types.FLOAT,
            java.sql.Types.REAL,
            java.sql.Types.DOUBLE,
            java.sql.Types.NUMERIC,
            java.sql.Types.DECIMAL -> Cursor.FIELD_TYPE_FLOAT

            java.sql.Types.CHAR,
            java.sql.Types.VARCHAR,
            java.sql.Types.LONGVARCHAR,
            java.sql.Types.NCHAR,
            java.sql.Types.NVARCHAR,
            java.sql.Types.LONGNVARCHAR -> Cursor.FIELD_TYPE_STRING
            java.sql.Types.BINARY,
            java.sql.Types.VARBINARY,
            java.sql.Types.LONGVARBINARY,
            java.sql.Types.BLOB,
            java.sql.Types.CLOB,
            java.sql.Types.NCLOB -> Cursor.FIELD_TYPE_BLOB
            else -> error("Invalid Type $type")
        }
    }

    override fun respond(extras: Bundle?) : Bundle = extras ?: Bundle.EMPTY
    override fun getExtras(): Bundle = extras ?: Bundle.EMPTY
    override fun setExtras(extras: Bundle?) { this.extras = extras }

    // UNSUPPORTED OPERATIONS
    @Deprecated(message = "Create a new Cursor", level = DeprecationLevel.ERROR, replaceWith = ReplaceWith(""))
    override fun requery() = error("Not Supported")
    @Deprecated(message = "Use Close", replaceWith = ReplaceWith("close()"), level = DeprecationLevel.ERROR)
    override fun deactivate() = error("Not Supported")

    override fun setNotificationUri(cr: ContentResolver?, uri: Uri?) = error("Not Supported")
    override fun registerDataSetObserver(observer: DataSetObserver?) = error("Not Supported")
    override fun registerContentObserver(observer: ContentObserver?) = error("Not Supported")
    override fun getNotificationUri(): Uri = error("Not Supported")
    override fun unregisterDataSetObserver(observer: DataSetObserver?) = error("Not Supported")
    override fun unregisterContentObserver(observer: ContentObserver?) = error("Not Supported")
}

