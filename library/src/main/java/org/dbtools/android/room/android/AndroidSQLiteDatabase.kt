package org.dbtools.android.room.android

import android.content.ContentValues
import android.database.Cursor
import android.database.SQLException
import android.database.sqlite.SQLiteCursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteTransactionListener
import android.os.CancellationSignal
import android.util.Pair
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteQuery
import androidx.sqlite.db.SupportSQLiteStatement
import java.io.IOException
import java.util.Locale

/**
 * Alternative/Similar to FrameworkSQLiteDatabase, with added support for AndroidSQLiteOpenHelperFactory to specify a onDatabaseConfigureBlock
 * - allow modifications to database before Room validation... (such as DatabaseUtil.checkAndFixRoomIdentityHash(...) for server-side supplied databases)
 * Reference: FrameworkSQLiteXXXX in sqlite-framework aar
 */
class AndroidSQLiteDatabase(
    private val delegate: SQLiteDatabase
) : SupportSQLiteDatabase {

    override fun compileStatement(sql: String): SupportSQLiteStatement {
        return AndroidSQLiteStatement(delegate.compileStatement(sql))
    }

    override fun beginTransaction() {
        delegate.beginTransaction()
    }

    override fun beginTransactionNonExclusive() {
        delegate.beginTransaction()
    }

    override fun beginTransactionWithListener(transactionListener: SQLiteTransactionListener) {
            delegate.beginTransactionWithListener(object : SQLiteTransactionListener {
            override fun onBegin() {
                transactionListener.onBegin()
            }

            override fun onCommit() {
                transactionListener.onCommit()
            }

            override fun onRollback() {
                transactionListener.onRollback()
            }
        })
    }

    override fun beginTransactionWithListenerNonExclusive(transactionListener: SQLiteTransactionListener) {
        beginTransactionWithListener(transactionListener)
    }

    override fun endTransaction() {
        delegate.endTransaction()
    }

    override fun setTransactionSuccessful() {
        delegate.setTransactionSuccessful()
    }

    override fun inTransaction(): Boolean {
        return delegate.inTransaction()
    }

    override fun isDbLockedByCurrentThread(): Boolean {
        return delegate.isDbLockedByCurrentThread
    }

    override fun yieldIfContendedSafely(): Boolean {
        return delegate.yieldIfContendedSafely()
    }

    override fun yieldIfContendedSafely(sleepAfterYieldDelay: Long): Boolean {
        return delegate.yieldIfContendedSafely(sleepAfterYieldDelay)
    }

    override fun getVersion(): Int {
        return delegate.version
    }

    override fun setVersion(version: Int) {
        delegate.version = version
    }

    override fun getMaximumSize(): Long {
        return delegate.maximumSize
    }

    override fun setMaximumSize(numBytes: Long): Long {
        return delegate.setMaximumSize(numBytes)
    }

    override fun getPageSize(): Long {
        return delegate.pageSize
    }

    override fun setPageSize(numBytes: Long) {
        delegate.pageSize = numBytes
    }

    override fun query(query: String): Cursor {
        return query(SimpleSQLiteQuery(query))
    }

    override fun query(query: String, bindArgs: Array<Any?>): Cursor {
        return query(SimpleSQLiteQuery(query, bindArgs))
    }

    override fun query(supportQuery: SupportSQLiteQuery): Cursor {
        return delegate.rawQueryWithFactory({ _, masterQuery, editTable, query ->
            supportQuery.bindTo(AndroidSQLiteProgram(query))
            SQLiteCursor(masterQuery, editTable, query)
        }, supportQuery.sql, EMPTY_STRING_ARRAY, null)
    }

    override fun query(supportQuery: SupportSQLiteQuery, cancellationSignal: CancellationSignal?): Cursor {
        return query(supportQuery)
    }

    @Throws(SQLException::class)
    override fun insert(table: String, conflictAlgorithm: Int, values: ContentValues): Long {
        return delegate.insertWithOnConflict(table, null, values, conflictAlgorithm)
    }

    override fun delete(table: String, whereClause: String?, whereArgs: Array<Any?>?): Int {
        val query = "DELETE FROM " + table + if (isEmpty(whereClause)) "" else " WHERE $whereClause"
        val statement = compileStatement(query)
        SimpleSQLiteQuery.bind(statement, whereArgs)
        return statement.executeUpdateDelete()
    }

    override fun update(table: String, conflictAlgorithm: Int, values: ContentValues, whereClause: String?, whereArgs: Array<Any?>?): Int {
        // taken from SQLiteDatabase class.
        if (values.size() == 0) {
            throw IllegalArgumentException("Empty values")
        }

        val sql = StringBuilder(120)
        sql.append("UPDATE ")
        sql.append(CONFLICT_VALUES[conflictAlgorithm])
        sql.append(table)
        sql.append(" SET ")

        // move all bind args to one array
        val setValuesSize = values.size()
        val bindArgsSize = if (whereArgs == null) setValuesSize else setValuesSize + whereArgs.size
        val bindArgs = arrayOfNulls<Any>(bindArgsSize)
        var i = 0
        for (colName in values.keySet()) {
            sql.append(if (i > 0) "," else "")
            sql.append(colName)
            bindArgs[i++] = values.get(colName)
            sql.append("=?")
        }
        if (whereArgs != null) {
            i = setValuesSize
            while (i < bindArgsSize) {
                bindArgs[i] = whereArgs[i - setValuesSize]
                i++
            }
        }
        if (!isEmpty(whereClause)) {
            sql.append(" WHERE ")
            sql.append(whereClause)
        }
        val stmt = compileStatement(sql.toString())
        SimpleSQLiteQuery.bind(stmt, bindArgs)
        return stmt.executeUpdateDelete()
    }

    @Throws(SQLException::class)
    override fun execSQL(sql: String) {
        delegate.execSQL(sql)
    }

    @Throws(SQLException::class)
    override fun execSQL(sql: String, bindArgs: Array<Any?>) {
        delegate.execSQL(sql, bindArgs)
    }

    override fun isReadOnly(): Boolean {
        return delegate.isReadOnly
    }

    override fun isOpen(): Boolean {
        return delegate.isOpen
    }

    override fun needUpgrade(newVersion: Int): Boolean {
        return delegate.needUpgrade(newVersion)
    }

    override fun getPath(): String {
        return delegate.path
    }

    override fun setLocale(locale: Locale) {
        delegate.setLocale(locale)
    }

    override fun setMaxSqlCacheSize(cacheSize: Int) {
        delegate.setMaxSqlCacheSize(cacheSize)
    }

    override fun setForeignKeyConstraintsEnabled(enable: Boolean) {
        delegate.setForeignKeyConstraintsEnabled(enable)
    }

    override fun enableWriteAheadLogging(): Boolean {
        return delegate.enableWriteAheadLogging()
    }

    override fun disableWriteAheadLogging() {
        delegate.disableWriteAheadLogging()
    }

    override fun isWriteAheadLoggingEnabled(): Boolean {
        return delegate.isWriteAheadLoggingEnabled
    }

    override fun getAttachedDbs(): List<Pair<String, String>>? {
        return delegate.attachedDbs
    }

    override fun isDatabaseIntegrityOk(): Boolean {
        return delegate.isDatabaseIntegrityOk
    }

    @Throws(IOException::class)
    override fun close() {
        delegate.close()
    }

    private fun isEmpty(input: String?): Boolean {
        return input == null || input.isEmpty()
    }

    companion object {
        private val CONFLICT_VALUES = arrayOf("", " OR ROLLBACK ", " OR ABORT ", " OR FAIL ", " OR IGNORE ", " OR REPLACE ")
        private val EMPTY_STRING_ARRAY = arrayOfNulls<String>(0)
    }
}