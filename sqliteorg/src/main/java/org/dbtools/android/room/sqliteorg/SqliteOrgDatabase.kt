package org.dbtools.android.room.sqliteorg

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteTransactionListener
import android.os.CancellationSignal
import android.util.Pair
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteQuery
import androidx.sqlite.db.SupportSQLiteStatement
import org.sqlite.database.SQLException
import org.sqlite.database.sqlite.SQLiteCursor
import org.sqlite.database.sqlite.SQLiteDatabase
import java.io.IOException
import java.util.Locale

/**
 * Mirrored from: https://github.com/ivanovsuper/android-architecture-components/blob/master/BasicSample/app/src/main/java/com/example/android/persistence/db/cipher/CipherSQLiteDatabase.java
 */
class SqliteOrgDatabase(
    private val delegate: SQLiteDatabase
) : SupportSQLiteDatabase {

    override fun compileStatement(sql: String): SupportSQLiteStatement {
        return SqliteOrgSQLiteStatement(delegate.compileStatement(sql))
    }

    override fun beginTransaction() {
        delegate.beginTransaction()
    }

    override fun beginTransactionNonExclusive() {
        delegate.beginTransaction()
    }

    override fun beginTransactionWithListener(transactionListener: SQLiteTransactionListener) {
        delegate.beginTransactionWithListener(object : org.sqlite.database.sqlite.SQLiteTransactionListener {
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

    override val isDbLockedByCurrentThread: Boolean
        get() {
            return delegate.isDbLockedByCurrentThread
        }

    override fun yieldIfContendedSafely(): Boolean {
        return delegate.yieldIfContendedSafely()
    }

    override fun yieldIfContendedSafely(sleepAfterYieldDelayMillis: Long): Boolean {
        return delegate.yieldIfContendedSafely(sleepAfterYieldDelayMillis)
    }

    override var version: Int
        get() {
            return delegate.version
        }
        set(value) {
            delegate.version = value
        }

    override val maximumSize: Long
        get() {
            return delegate.maximumSize
        }

    override fun setMaximumSize(numBytes: Long): Long {
        return delegate.setMaximumSize(numBytes)
    }

    override var pageSize: Long
        get() {
            return delegate.pageSize
        }
        set(value) {
            delegate.pageSize = value
        }

    override fun query(query: String): Cursor {
        return query(SimpleSQLiteQuery(query))
    }

    override fun query(query: String, bindArgs: Array<out Any?>): Cursor {
        return query(SimpleSQLiteQuery(query, bindArgs))
    }

    override fun query(query: SupportSQLiteQuery): Cursor {
        return delegate.rawQueryWithFactory({ _, masterQuery, editTable, query2 ->

            // todo needed??? sqlcipher only???
//            var count = 0
//            try {
//                if (supportQuery is RoomSQLiteQuery) {
//                    val argCount = RoomSQLiteQuery::class.java.getDeclaredField("mArgCount")
//                    argCount.isAccessible = true
//                    count = argCount.getInt(supportQuery)
//                } else if (supportQuery is SimpleSQLiteQuery) {
//                    val bindArgs = SimpleSQLiteQuery::class.java.getDeclaredField("mBindArgs")
//                    bindArgs.isAccessible = true
//                    val bindArgsValue = bindArgs.get(supportQuery) as Array<Any?>?
//                    count = bindArgsValue?.size ?: 0
//                }
//                val args = SQLiteQuery::class.java.getDeclaredField("mBindArgs")
//                args.setAccessible(true)
//                args.set(query, arrayOfNulls<String>(count))
//            } catch (e: NoSuchFieldException) {
//                e.printStackTrace()
//            } catch (e: IllegalAccessException) {
//                e.printStackTrace()
//            }

            query.bindTo(SqliteOrgSQLiteProgram(query2))
            SQLiteCursor(masterQuery, editTable, query2)
        }, query.sql, EMPTY_STRING_ARRAY, null)
    }

    override fun query(query: SupportSQLiteQuery, cancellationSignal: CancellationSignal?): Cursor {
        return query(query)
    }

    @Throws(SQLException::class)
    override fun insert(table: String, conflictAlgorithm: Int, values: ContentValues): Long {
        return delegate.insertWithOnConflict(table, null, values, conflictAlgorithm)
    }

    override fun delete(table: String, whereClause: String?, whereArgs: Array<out Any?>?): Int {
        val query = "DELETE FROM " + table + if (isEmpty(whereClause)) "" else " WHERE $whereClause"
        val statement = compileStatement(query)
        SimpleSQLiteQuery.bind(statement, whereArgs)
        return statement.executeUpdateDelete()
    }

    override fun update(table: String, conflictAlgorithm: Int, values: ContentValues, whereClause: String?, whereArgs: Array<out Any?>?): Int {
        // taken from SQLiteDatabase class.
        require (values.size() != 0) { "Empty values" }

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

    override fun execSQL(sql: String, bindArgs: Array<out Any?>) {
        delegate.execSQL(sql, bindArgs)
    }

    override val isReadOnly: Boolean
        get() {
            return delegate.isReadOnly
        }

    override val isOpen: Boolean
        get() {
            return delegate.isOpen
        }

    override fun needUpgrade(newVersion: Int): Boolean {
        return delegate.needUpgrade(newVersion)
    }

    override val path: String
        get() {
            return delegate.path
        }

    override fun setLocale(locale: Locale) {
        delegate.setLocale(locale)
    }

    override fun setMaxSqlCacheSize(cacheSize: Int) {
        delegate.setMaxSqlCacheSize(cacheSize)
    }

    override fun setForeignKeyConstraintsEnabled(enabled: Boolean) {
        delegate.setForeignKeyConstraintsEnabled(enabled)
    }

    override fun enableWriteAheadLogging(): Boolean {
        return delegate.enableWriteAheadLogging()
    }

    override fun disableWriteAheadLogging() {
        delegate.disableWriteAheadLogging()
    }

    override val isWriteAheadLoggingEnabled: Boolean
        get() {
            return delegate.isWriteAheadLoggingEnabled
        }

    override val attachedDbs: List<Pair<String, String>>?
        get() {
            return delegate.attachedDbs
        }

    override val isDatabaseIntegrityOk: Boolean
        get() {
            return delegate.isDatabaseIntegrityOk
        }

    @Throws(IOException::class)
    override fun close() {
        delegate.close()
    }

    private fun isEmpty(input: String?): Boolean {
        return input.isNullOrEmpty()
    }

    companion object {
        private val CONFLICT_VALUES = arrayOf("", " OR ROLLBACK ", " OR ABORT ", " OR FAIL ", " OR IGNORE ", " OR REPLACE ")
        private val EMPTY_STRING_ARRAY = arrayOfNulls<String>(0)
    }
}