package org.dbtools.android.room.sqliteorg

import androidx.sqlite.db.SupportSQLiteProgram
import org.sqlite.database.sqlite.SQLiteProgram

open class SqliteOrgSQLiteProgram(
        private val delegate: SQLiteProgram
) : SupportSQLiteProgram {

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

    override fun close() {
        this.delegate.close()
    }
}