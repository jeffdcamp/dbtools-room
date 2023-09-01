package org.dbtools.android.room.jdbc

import android.database.Cursor
import androidx.sqlite.db.SupportSQLiteStatement
import java.sql.Connection

class JdbcSQLiteStatement(
    private val conn: Connection,
    sql: String
) : JdbcSQLiteProgram(conn, sql), SupportSQLiteStatement {

    override fun execute() {
        statement.execute()
    }

    override fun executeInsert(): Long {
        statement.executeUpdate()

        // Get the last inserted Id
        // generatedKeys is no longer supported (https://github.com/xerial/sqlite-jdbc/issues/329)
        //
        // Optimal solution would use sqlite RETURNING statement on original query, but the original query is not available here
        //
        // Next best solution is to call last_insert_rowid()
        // (seems to be more reliable than generatedKeys (per discussion in above ticket, and the reason why generatedKeys was removed))
        val results = conn.createStatement().executeQuery("SELECT last_insert_rowid()")

        return if (results.next()) {
            results.getLong(1)
        } else {
            -1L
        }
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