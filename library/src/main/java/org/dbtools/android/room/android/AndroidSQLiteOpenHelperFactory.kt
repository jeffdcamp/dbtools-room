package org.dbtools.android.room.android

import android.database.sqlite.SQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper

/**
 * @param path Custom path to database
 * @param onDatabaseConfigureBlock After creation of the SQLiteDatabase object, this block will be called with the created instance, allowing further customizations
 */
@Suppress("unused")
class AndroidSQLiteOpenHelperFactory(
    private val path: String = "",
    private val onDatabaseConfigureBlock: (sqliteDatabase: SQLiteDatabase) -> Unit = {}
) : SupportSQLiteOpenHelper.Factory {

    override fun create(configuration: SupportSQLiteOpenHelper.Configuration): SupportSQLiteOpenHelper {
        return AndroidSQLiteOpenHelper(configuration.context, path, configuration.name, configuration.callback, onDatabaseConfigureBlock)
    }
}