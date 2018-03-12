package org.dbtools.android.room.jdbc

import android.arch.persistence.db.SimpleSQLiteQuery
import android.arch.persistence.db.SupportSQLiteDatabase
import android.arch.persistence.db.SupportSQLiteQuery
import android.arch.persistence.db.SupportSQLiteStatement
import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteTransactionListener
import android.os.CancellationSignal
import android.util.Pair
import org.sqlite.JDBC
import java.sql.Connection
import java.sql.DriverManager
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger

class JdbcSqliteDatabase(
        private val dbPath: String
) : SupportSQLiteDatabase {

    private val dbUrl = "jdbc:sqlite:$dbPath"
    private val conn: Connection
    private var commitTransaction = false
    private var transactionListener: SQLiteTransactionListener? = null
    private val transactionCounter = AtomicInteger(0)

    init {
        try {
            DriverManager.registerDriver(JDBC())
        } catch (e: Exception) {
            throw IllegalStateException("Could load sqlite-jdbc driver... is the sqlite-jdbc driver included in the dependencies?", e)
        }

        conn = DriverManager.getConnection(dbUrl)
        conn?.let {
            println("Connected to the database")
            val dm = it.metaData
            System.out.println("Driver name: " + dm.driverName)
            System.out.println("Driver version: " + dm.driverVersion)
            System.out.println("Product name: " + dm.databaseProductName)
            System.out.println("Product version: " + dm.databaseProductVersion)
        }
    }

    override fun isDatabaseIntegrityOk(): Boolean {
        val rs = conn.prepareCall("PRAGMA integrity_check(1)").executeQuery()
        return rs.getString(1).equals("ok", ignoreCase = true)
    }

    override fun isDbLockedByCurrentThread(): Boolean {
        return true
    }

    override fun close() {
        conn.close()
    }

    override fun isOpen(): Boolean {
        return !conn.isClosed
    }

    override fun beginTransaction() {
        beginTransactionWithListenerNonExclusive(null)
    }

    override fun beginTransactionNonExclusive() {
        beginTransactionWithListenerNonExclusive(null)
    }

    override fun beginTransactionWithListener(transactionListener: SQLiteTransactionListener?) {
        beginTransactionWithListenerNonExclusive(transactionListener)
    }

    override fun beginTransactionWithListenerNonExclusive(transactionListener: SQLiteTransactionListener?) {
        if (conn.autoCommit) {
            this.transactionListener = transactionListener
            commitTransaction = false
            conn.autoCommit = false
            this.transactionListener?.onBegin()
            transactionCounter.set(0)
        }
        transactionCounter.incrementAndGet()
    }

    override fun inTransaction(): Boolean {
        return !conn.autoCommit
    }

    override fun endTransaction() {
        if (transactionCounter.decrementAndGet() == 0) {
            try {
                when {
                    commitTransaction -> {
                        conn.commit()
                        transactionListener?.onCommit()
                    }
                    else -> {
                        conn.rollback()
                        transactionListener?.onCommit()
                    }
                }
            } finally {
                conn.autoCommit = true
                commitTransaction = false
                transactionListener = null
            }
        }
    }

    override fun setTransactionSuccessful() {
        if (transactionCounter.get() == 1) {
            commitTransaction = true
        }
    }

    override fun yieldIfContendedSafely(): Boolean {
        return false
    }

    override fun yieldIfContendedSafely(sleepAfterYieldDelay: Long): Boolean {
        return false
    }

    override fun compileStatement(sql: String?): SupportSQLiteStatement {
        return compileJdbcStatement(sql)
    }

    override fun insert(table: String?, conflictAlgorithm: Int, values: ContentValues?): Long {
        val initialValues = checkNotNull(values) { "values must not be null" }
        check(initialValues.size() > 0) { "values must not be empty" }
        val tableName = checkNotNull(table) { "table must not be null" }
        val size = initialValues.size()
        val bindArgs = arrayOfNulls<Any?>(size)
        val sql = buildString {
            append("INSERT")
            append(CONFLICT_VALUES[conflictAlgorithm])
            append(" INTO ")
            append(tableName)
            append('(')
            initialValues.keySet().forEachIndexed { i, key ->
                if (i > 0) {
                    append(",")
                }
                append(key)
                bindArgs[i] = initialValues.get(key)
            }

            append(')')
            append(" VALUES (")
            repeat(size) {
                append(when (it) {
                    0 -> "?"
                    else -> ",?"
                })
            }
            append(')')
        }
        val statement = JdbcSQLiteStatement(conn, sql)
        try {
            statement.bindArguments(bindArgs)
            return statement.executeInsert()
        } finally {
            statement.close()
        }
    }

    override fun query(query: String?): Cursor {
        return query(SimpleSQLiteQuery(query))
    }

    override fun query(query: String?, bindArgs: Array<out Any?>?): Cursor {
        return query(SimpleSQLiteQuery(query, bindArgs))
    }

    override fun query(query: SupportSQLiteQuery?): Cursor {
        val sqlQuery = checkNotNull(query) { "Query must not be null" }
        val sql = checkNotNull(sqlQuery.sql) { "Query SQL must be null" }
        val statement = compileJdbcStatement(sql)
        sqlQuery.bindTo(statement)
        return statement.executeQuery()
    }

    override fun query(query: SupportSQLiteQuery?, cancellationSignal: CancellationSignal?): Cursor {
        return query(query)
    }

    override fun update(table: String?, conflictAlgorithm: Int, values: ContentValues?, whereClause: String?, whereArgs: Array<out Any?>?): Int {
        val initialValues = checkNotNull(values) { "Values must not be null" }
        check(initialValues.size() > 0) { "Values must not be empty" }
        val setValuesSize = initialValues.size()
        val bindArgSize = when(whereArgs) {
            null -> setValuesSize
            else -> setValuesSize + whereArgs.size
        }
        val bindArgs = arrayOfNulls<Any>(bindArgSize)

        val sql = buildString {
            append("UPDATE ")
            append(CONFLICT_VALUES[conflictAlgorithm])
            append(table)
            append(" SET ")

            initialValues.keySet().forEachIndexed { i, key ->
                if (i > 0) {
                    append(",")
                }
                append(key)
                bindArgs[i] = initialValues.get(key)
            }
            if (whereArgs != null) {
                var i = setValuesSize
                while (i < bindArgSize) {
                    bindArgs[i] = whereArgs[i - setValuesSize]
                    i++
                }
            }
            if (!whereClause.isNullOrBlank()) {
                append(" WHERE ")
                append(whereClause)
            }
        }

        val stmt = compileStatement(sql)
        SimpleSQLiteQuery.bind(stmt, bindArgs)
        return stmt.executeUpdateDelete()
    }

    override fun delete(table: String?, whereClause: String?, whereArgs: Array<out Any?>?): Int {
        val query = "DELETE FROM $table ${when {
            whereClause.isNullOrBlank() -> ""
            else -> "WHERE $whereClause"
        }}"
        val statement = compileStatement(query)
        SimpleSQLiteQuery.bind(statement, whereArgs)
        return statement.executeUpdateDelete()
    }

    override fun execSQL(sql: String?) = compileStatement(sql).execute()

    override fun execSQL(sql: String?, bindArgs: Array<out Any>?) {
        val stmt = compileStatement(sql)
        SimpleSQLiteQuery.bind(stmt, bindArgs)
        stmt.execute()
    }

    override fun setLocale(locale: Locale?) {
        // NO OP
    }

    override fun getAttachedDbs(): List<Pair<String, String>> {
        return listOf()
    }

    override fun setMaxSqlCacheSize(cacheSize: Int) {
        // NO OP
    }

    override fun setForeignKeyConstraintsEnabled(enable: Boolean) {
        conn.createStatement().execute(when {
            enable -> "PRAGMA foreign_keys = ON"
            else -> "PRAGMA foreign_keys = OFF"
        })
    }

    override fun getVersion(): Int {
        return conn.prepareCall("PRAGMA user_version").executeQuery().use {
            it.getInt(1)
        }
    }

    override fun setVersion(version: Int) {
        execSQL("PRAGMA user_version = $version")
    }

    override fun needUpgrade(newVersion: Int): Boolean {
        return newVersion > version
    }

    override fun isReadOnly(): Boolean {
        return conn.isReadOnly
    }

    override fun getPath(): String {
        return dbPath
    }

    override fun getMaximumSize(): Long {
        return -1L
    }

    override fun setMaximumSize(numBytes: Long): Long {
        return -1L
    }

    override fun setPageSize(numBytes: Long) {
        // No Op
    }

    override fun getPageSize(): Long {
        return -1L
    }

    override fun enableWriteAheadLogging(): Boolean {
        return false
    }

    override fun isWriteAheadLoggingEnabled(): Boolean {
        return false
    }

    override fun disableWriteAheadLogging() {
        // NO OP
    }

    private fun compileJdbcStatement(sql: String?): JdbcSQLiteStatement {
        val safeSql = checkNotNull(sql) { "sql must not be null" }
        return JdbcSQLiteStatement(conn, safeSql)
    }

    companion object {
        private val CONFLICT_VALUES = arrayOf("", " OR ROLLBACK ", " OR ABORT ", " OR FAIL ", " OR IGNORE ", " OR REPLACE ")
    }
}