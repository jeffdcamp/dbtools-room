package org.dbtools.android.room.util

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import org.dbtools.android.room.DatabaseViewQuery
import timber.log.Timber
import java.io.File
import java.io.FileFilter
import kotlin.system.measureTimeMillis

@Suppress("unused")
object DatabaseUtil {
    private const val CORRUPTION_CHECK_PASSED = "ok"

    /**
     * Preform a PRAGMA check on the database and optionally check a table for existing data.  This check can be used to check a
     * database prior to hooking it up to Room and running database table upgrades and versioning (useful for pre-existing databases)
     *
     * NOTE: This call will use the built-in default Android Sqlite Database Libraries... see also SqliteOrgDatabaseUtil.validateDatabaseFile(...)
     *
     * @param path File path to database
     * @param databaseNameTag Optional tag name to help identify database in logging
     * @param tableDataCountCheck Optional check on a table for data. (optional)
     *
     * @return true if validation check is OK
     */
    fun validDatabaseFile(path: String, databaseNameTag: String = "", tableDataCountCheck: String = ""): Boolean {
        Timber.i("Checking database integrity for [%s]", databaseNameTag)
        val totalTimeMs = measureTimeMillis {
            try {
                SQLiteDatabase.openDatabase(path, null, SQLiteDatabase.OPEN_READONLY).use { database ->

                    // pragma check
                    database.rawQuery("pragma quick_check", null).use { pragmaCheckCursor ->
                        if (!pragmaCheckCursor!!.moveToFirst()) {
                            Timber.e("validateDatabase - database [%s] pragma check returned no results", databaseNameTag)
                            return false
                        }
                        if (pragmaCheckCursor.getString(0) != CORRUPTION_CHECK_PASSED) {
                            Timber.e("validateDatabase - database [%s] pragma check failed", databaseNameTag)
                            return false
                        }
                    }

                    // make sure there is data in the database
                    if (tableDataCountCheck.isNotBlank()) {
                        database.rawQuery("SELECT count(1) FROM $tableDataCountCheck", null).use { cursor ->
                            val count = if (cursor.moveToFirst()) {
                                cursor.getInt(0)
                            } else {
                                0
                            }

                            if (count == 0) {
                                Timber.e("validateDatabase - table [%s] is BLANK for database [%s] is blank", tableDataCountCheck, databaseNameTag)
                                return false
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to validate database [$databaseNameTag]")
                return false
            }
        }

        Timber.i("Database integrity for [$databaseNameTag]  OK! (${totalTimeMs}ms)")
        return true
    }

    /**
     * Preform a PRAGMA check on the database and optionally check a table for existing data
     *
     * @param roomDatabase RoomDatabase to be validated
     * @param databaseNameTag Optional tag name to help identify database in logging
     * @param tableDataCountCheck Optional check on a table for data. (optional)
     *
     * @return true if validation check is OK
     */
    fun validDatabaseFile(roomDatabase: RoomDatabase, databaseNameTag: String = "", tableDataCountCheck: String = ""): Boolean {
        val database = roomDatabase.openHelper.readableDatabase
        return validDatabaseFile(database, databaseNameTag, tableDataCountCheck)
    }

    /**
     * Preform a PRAGMA check on the database and optionally check a table for existing data
     *
     * @param database SupportSQLiteDatabase to be validated
     * @param databaseNameTag Optional tag name to help identify database in logging
     * @param tableDataCountCheck Optional check on a table for data. (optional)
     *
     * @return true if validation check is OK
     */
    fun validDatabaseFile(database: SupportSQLiteDatabase, databaseNameTag: String = "", tableDataCountCheck: String = ""): Boolean {
        Timber.i("Checking database integrity for [%s]", databaseNameTag)
        val totalTimeMs = measureTimeMillis {
            try {
                // pragma check
                database.query("pragma quick_check", null).use { pragmaCheckCursor ->
                    if (!pragmaCheckCursor!!.moveToFirst()) {
                        Timber.e("validateDatabase - database [%s] pragma check returned no results", databaseNameTag)
                        return false
                    }
                    if (pragmaCheckCursor.getString(0) != CORRUPTION_CHECK_PASSED) {
                        Timber.e("validateDatabase - database [%s] pragma check failed", databaseNameTag)
                        return false
                    }
                }

                // make sure there is data in the database
                if (tableDataCountCheck.isNotBlank()) {
                    database.query("SELECT count(1) FROM $tableDataCountCheck", null).use { cursor ->
                        val count = if (cursor.moveToFirst()) {
                            cursor.getInt(0)
                        } else {
                            0
                        }

                        if (count == 0) {
                            Timber.e("validateDatabase - table [%s] is BLANK for database [%s] is blank", tableDataCountCheck, databaseNameTag)
                            return false
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to validate database [$databaseNameTag]")
                return false
            }
        }

        Timber.i("Database integrity for [$databaseNameTag]  OK! (${totalTimeMs}ms)")
        return true
    }

    /**
     * Check to see if a column in a database exists, if it does not... alter query will be run
     * @param database Sqlite database
     * @param tableName table for columnName
     * @param columnName column to from tableName to be checked
     * @param alterSql SQL to be run if the column does not exits. Example: alterTableIfColumnDoesNotExist(database, "individual", "middle_name", "ALTER TABLE individual ADD `middle_name` TEXT DEFAULT '' NOT NULL")
     */
    fun alterTableIfColumnDoesNotExist(database: SQLiteDatabase, tableName: String, columnName: String, alterSql: String) {
        if (!columnExists(database, tableName, columnName)) {
            Timber.i("Adding column [$columnName] to table [$tableName]")
            database.execSQL(alterSql)
            resetRoom(database)
        }
    }

    /**
     * Check to see if a table in a database exists
     * @param database Sqlite database
     * @param tableName tableName to from database to be checked
     * @return true if the table exists otherwise false
     */
    fun tableExists(database: SQLiteDatabase, tableName: String): Boolean {
        var tableExists = false

        database.rawQuery("SELECT count(1) FROM sqlite_master WHERE type='table' AND name='$tableName'", null).use { cursor ->
            if (cursor.moveToFirst()) {
                val count = cursor.getInt(0)
                if (count > 0) {
                    tableExists = true
                }
            } else {
                Timber.w("Query: [SELECT count(1) FROM sqlite_master WHERE type='table' AND name='$tableName'] returned NO data")
            }
        }

        return tableExists
    }

    /**
     * Check to see if a column in a database exists
     * @param database Sqlite database
     * @param tableName table for columnName
     * @param columnName column to from tableName to be checked
     * @return true if the column exists otherwise false
     */
    fun columnExists(database: SQLiteDatabase, tableName: String, columnName: String): Boolean {
        var columnExists = false

        database.rawQuery("PRAGMA table_info($tableName)", null).use { cursor ->
            if (cursor.moveToFirst()) {
                do {
                    val currentColumn = cursor.getString(cursor.getColumnIndexOrThrow("name"))
                    if (currentColumn == columnName) {
                        columnExists = true
                    }
                } while (!columnExists && cursor.moveToNext())
            } else {
                Timber.w("Query: [PRAGMA table_info($tableName)] returned NO data")
            }
        }

        return columnExists
    }

    /**
     * If we make a manual change, then we need to reset room so that it does not fail the validation
     *
     * @param database SQLite Database
     * @param newVersion version to be set on database (default to 0)
     */
    fun resetRoom(database: SQLiteDatabase, newVersion: Int = 0) {
        database.rawQuery("DROP TABLE IF EXISTS room_master_table", null).close()
        database.version = newVersion
    }

    /**
     * If the database should NOT have a migration and is a pre-populated database that should not be managed by Room... make sure Room migration is never needed.
     *
     * @param database SQLite Database
     * @param expectedIdentityHash Hash that is expected.  If the expectedIdentityHash does not match the existing identity hash (currently in the room_master_table), then just delete the table
     */
    fun checkAndFixRoomIdentityHash(database: SQLiteDatabase, expectedIdentityHash: String) {
        if (!tableExists(database, "room_master_table")) {
            // We are OK... the table does not exist
            return
        }

        val identityHash = findRoomIdentityHash(database)
        if (identityHash.isNullOrBlank()) {
            Timber.w("checkAndFixRoomIdentityHash -- room_master_table.identity_hash was null or blank... making sure room_master_table does not exist")
            resetRoom(database)
        } else if (identityHash != expectedIdentityHash) {
            Timber.w("checkAndFixRoomIdentityHash -- expectedIdentityHash: [$expectedIdentityHash] !=  room_master_table.identity_hash [$identityHash]... removing room_master_table")
            resetRoom(database)
        }
    }

    /**
     * Find the Room Identity Hash
     * Note: if you are not sure if the room_master_table exists, check first with tableExists(database, "room_master_table")
     *
     * @param database SQLite Database
     * @return identity_hash for this database OR null if it does exist
     */
    fun findRoomIdentityHash(database: SQLiteDatabase): String? {
        var identityHash: String? = null

        // NOTE: the id column for this table always seems to be 42 and there is always only 1 row... so lets just find the first row
        database.rawQuery("SELECT identity_hash FROM room_master_table LIMIT 1", null).use { cursor ->
            if (cursor.moveToFirst()) {
                val columnIndex = cursor.getColumnIndex("identity_hash")
                identityHash = if (columnIndex == -1) {
                    // does not exist
                    null
                } else {
                    cursor.getString(columnIndex)
                }
            } else {
                Timber.w("Query: [SELECT identity_hash FROM room_master_table LIMIT 1] returned NO data")
            }
        }

        return identityHash
    }

    /**
     * Deletes a database including its journal file and other auxiliary files
     * that may have been created by the database engine.
     *
     * @param file The database file path.
     * @return true if the database was successfully deleted.
     */
    fun deleteDatabaseFiles(file: File): Boolean {
        var deleted: Boolean
        deleted = file.delete()
        deleted = deleted or File("${file.path}-journal").delete()
        deleted = deleted or File("${file.path}-shm").delete()
        deleted = deleted or File("${file.path}-wal").delete()

        val dir = file.parentFile
        if (dir != null && dir.exists()) {
            val prefix = "${file.name}-mj"
            val filter = FileFilter { candidate -> candidate.name.startsWith(prefix) }
            val files = dir.listFiles(filter) ?: emptyArray()
            for (masterJournal in files) {
                deleted = deleted or masterJournal.delete()
            }
        }
        return deleted
    }

    /**
     * Renames a database including its journal file and other auxiliary files
     * that may have been created by the database engine.
     *
     * @param srcFile The database file path.
     * @return true if the database was successfully rename.
     */
    fun renameDatabaseFiles(srcFile: File, targetFile: File): Boolean {
        var renamed: Boolean
        renamed = srcFile.renameTo(File(targetFile.path))
        renamed = renamed or File("${srcFile.path}-journal").renameTo(File("${targetFile.path}-journal"))
        renamed = renamed or File("${srcFile.path}-shm").renameTo(File("${targetFile.path}-shm"))
        renamed = renamed or File("${srcFile.path}-wal").renameTo(File("${targetFile.path}-wal"))

        // delete srcFile -mj files
        val dir = srcFile.parentFile
        if (dir != null && dir.exists()) {
            val prefix = "${srcFile.name}-mj"
            val filter = FileFilter { candidate -> candidate.name.startsWith(prefix) }
            val files = dir.listFiles(filter) ?: emptyArray()
            for (masterJournal in files) {
                renamed = renamed or masterJournal.delete()
            }
        }
        return renamed
    }

    /**
     * Copy database from assets to <app>/database
     *
     * @param context Android context
     * @param databaseFilename Name of file in assets
     * @param overwrite Overwrite the target file
     *
     * @return File of the copied database
     */
    fun copyDatabaseFromAssets(context: Context, databaseFilename: String, overwrite: Boolean = false): File {
        val newDatabaseFile = context.getDatabasePath(databaseFilename)
        val assetDatabaseInputStream = context.assets.open(databaseFilename)

        if (newDatabaseFile.exists() && overwrite) {
            deleteDatabaseFiles(newDatabaseFile)
        }

        assetDatabaseInputStream.use { input ->
            newDatabaseFile.outputStream().use { fileOut ->
                input.copyTo(fileOut)
            }
        }

        return newDatabaseFile
    }

    fun dropView(database: SupportSQLiteDatabase, viewName: String) {
        database.execSQL("DROP VIEW IF EXISTS $viewName")
    }

    fun dropAllViews(database: SupportSQLiteDatabase, views: List<DatabaseViewQuery>) {
        views.forEach { dropView(database, it.viewName) }
    }

    fun createView(database: SupportSQLiteDatabase, viewName: String, viewQuery: String) {
        database.execSQL("CREATE VIEW `$viewName` AS $viewQuery")
    }

    fun createAllViews(database: SupportSQLiteDatabase, views: List<DatabaseViewQuery>) {
        views.forEach { createView(database, it.viewName, it.viewQuery) }
    }

    fun recreateView(database: SupportSQLiteDatabase, viewName: String, viewQuery: String) {
        dropView(database, viewName)
        createView(database, viewName, viewQuery)
    }

    fun recreateAllViews(database: SupportSQLiteDatabase, views: List<DatabaseViewQuery>) {
        views.forEach { recreateView(database, it.viewName, it.viewQuery) }
    }
}