package org.dbtools.android.room.jdbc

import androidx.sqlite.db.SupportSQLiteProgram
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.Types

open class JdbcSQLiteProgram(
        conn: Connection,
        sql: String
) : SupportSQLiteProgram {

    protected val statement: PreparedStatement = conn.prepareStatement(sql)

    override fun bindBlob(index: Int, value: ByteArray) {
        statement.setBytes(index, value)
    }

    override fun bindDouble(index: Int, value: Double) {
        statement.setDouble(index, value)
    }

    override fun bindLong(index: Int, value: Long) {
        statement.setLong(index, value)
    }

    override fun bindNull(index: Int) {
        statement.setNull(index, Types.VARCHAR)
    }

    override fun bindString(index: Int, value: String) {
        statement.setString(index, value)
    }

    override fun clearBindings() {
        statement.clearParameters()
    }

    override fun close() {
        statement.close()
    }

    fun bindArguments(bindArgs: Array<Any?>) {
        bindArgs.forEachIndexed { index, arg ->
            when (arg) {
                null -> bindNull(index)
                is ByteArray -> bindBlob(index, arg)
                is Float -> bindDouble(index, arg.toDouble())
                is Double -> bindDouble(index, arg)
                is Byte -> bindLong(index, arg.toLong())
                is Short -> bindLong(index, arg.toLong())
                is Int -> bindLong(index, arg.toLong())
                is Long -> bindLong(index, arg)
                is String -> bindString(index, arg)
                is Boolean -> bindLong(index, if (arg) 1 else 0)
                else -> bindString(index, arg.toString())
            }

        }
    }
}