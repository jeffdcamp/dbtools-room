
package org.dbtools.android.room.jdbc

import android.arch.persistence.db.SupportSQLiteOpenHelper

@Suppress("unused")
class JdbcSQLiteOpenHelperFactory(val path: String = "") : SupportSQLiteOpenHelper.Factory {

    override fun create(configuration: SupportSQLiteOpenHelper.Configuration): SupportSQLiteOpenHelper {
        return JdbcSQLiteOpenHelper(path, configuration.name, configuration.callback)
    }
}