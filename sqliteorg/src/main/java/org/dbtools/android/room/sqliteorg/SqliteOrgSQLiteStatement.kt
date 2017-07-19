package org.dbtools.android.room.sqliteorg

import android.arch.persistence.db.SupportSQLiteStatement
import org.sqlite.database.sqlite.SQLiteStatement

class SqliteOrgSQLiteStatement(private val delegate: SQLiteStatement) : SupportSQLiteStatement {
    override fun bindNull(index: Int) {
        this.delegate.bindNull(index)
    }

    override fun bindLong(index: Int, value: Long) {
        this.delegate.bindLong(index, value)
    }

    override fun bindDouble(index: Int, value: Double) {
        this.delegate.bindDouble(index, value)
    }

    override fun bindString(index: Int, value: String) {
        this.delegate.bindString(index, value)
    }

    override fun bindBlob(index: Int, value: ByteArray) {
        this.delegate.bindBlob(index, value)
    }

    override fun clearBindings() {
        this.delegate.clearBindings()
    }

    override fun execute() {
        this.delegate.execute()
    }

    override fun executeUpdateDelete(): Int {
        return this.delegate.executeUpdateDelete()
    }

    override fun executeInsert(): Long {
        return this.delegate.executeInsert()
    }

    override fun simpleQueryForLong(): Long {
        return this.delegate.simpleQueryForLong()
    }

    override fun simpleQueryForString(): String {
        return this.delegate.simpleQueryForString()
    }

//    override fun simpleQueryForBlobFileDescriptor(): ParcelFileDescriptor {
//        return this.delegate.simpleQueryForBlobFileDescriptor()
//    }
}