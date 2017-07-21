package org.dbtools.android.room.jdbc

import android.arch.persistence.db.SupportSQLiteStatement
import android.database.Cursor
import java.sql.Connection

class JdbcSQLiteStatement(conn: Connection, sql: String) : JdbcSQLiteProgram(conn, sql), SupportSQLiteStatement {

    override fun execute() {
        statement.execute()
    }

    override fun executeInsert(): Long {
        statement.executeUpdate()

        // Get the last inserted Id
        val rs = statement.generatedKeys
        rs.next()
        return rs.getLong(1)
    }

    override fun executeUpdateDelete(): Int {
        return statement.executeUpdate()
    }

    override fun simpleQueryForLong(): Long {
        return statement.executeUpdate().toLong()
    }

    override fun simpleQueryForString(): String {
        val rs = statement.executeQuery()
        return rs.getString(1)
    }

    fun executeQuery(): Cursor {
        return JdbcMemoryCursor(statement.executeQuery())
    }
}