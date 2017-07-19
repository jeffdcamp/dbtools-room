
package org.dbtools.android.room.sqliteorg

import android.arch.persistence.db.SupportSQLiteOpenHelper

@Suppress("unused")
class SqliteOrgSQLiteOpenHelperFactory(val path: String = "", val password: String = "") : SupportSQLiteOpenHelper.Factory {

    override fun create(configuration: SupportSQLiteOpenHelper.Configuration): SupportSQLiteOpenHelper {
        return SqliteOrgSQLiteOpenHelper(configuration.context, path, configuration.name, configuration.version, configuration.callback, password)
    }
}