package org.dbtools.android.room.android

import android.database.sqlite.SQLiteStatement
import androidx.sqlite.db.SupportSQLiteStatement

/**
 * Delegates all calls to a [SQLiteStatement].
 */
class AndroidSQLiteStatement(private val delegate: SQLiteStatement) : AndroidSQLiteProgram(delegate), SupportSQLiteStatement {
    override fun execute() {
        delegate.execute()
    }

    override fun executeUpdateDelete(): Int {
        return delegate.executeUpdateDelete()
    }

    override fun executeInsert(): Long {
        return delegate.executeInsert()
    }

    override fun simpleQueryForLong(): Long {
        return delegate.simpleQueryForLong()
    }

    override fun simpleQueryForString(): String {
        return delegate.simpleQueryForString()
    }
}