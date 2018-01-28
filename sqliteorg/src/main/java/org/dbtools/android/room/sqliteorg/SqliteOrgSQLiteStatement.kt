package org.dbtools.android.room.sqliteorg

import android.arch.persistence.db.SupportSQLiteStatement
import org.sqlite.database.sqlite.SQLiteStatement

class SqliteOrgSQLiteStatement(
        private val statementDelegate: SQLiteStatement
) : SqliteOrgSQLiteProgram(statementDelegate), SupportSQLiteStatement {

    override fun execute() {
        this.statementDelegate.execute()
    }

    override fun executeInsert(): Long {
        return this.statementDelegate.executeInsert()
    }

    override fun executeUpdateDelete(): Int {
        return this.statementDelegate.executeUpdateDelete()
    }

    override fun simpleQueryForLong(): Long {
        return this.statementDelegate.simpleQueryForLong()
    }

    override fun simpleQueryForString(): String {
        return this.statementDelegate.simpleQueryForString()
    }

//    override fun simpleQueryForBlobFileDescriptor(): ParcelFileDescriptor {
//        return this.delegate.simpleQueryForBlobFileDescriptor()
//    }
}