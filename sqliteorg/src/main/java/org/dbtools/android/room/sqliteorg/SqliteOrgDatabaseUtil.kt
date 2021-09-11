package org.dbtools.android.room.sqliteorg

import org.sqlite.database.sqlite.SQLiteDatabase
import timber.log.Timber
import java.io.File
import java.io.FileFilter
import kotlin.system.measureTimeMillis

object SqliteOrgDatabaseUtil {
    /**
     * Preform a PRAGMA check on the database and optionally check a table for existing data.  This check can be used to check a
     * database prior to hooking it up to Room and running database table upgrades and versioning (useful for pre-existing databases)
     *
     * NOTE: This call will use the org.sqlite.database.sqlite.SQLiteDatabase Library... see also DatabaseUtil.validateDatabaseFile(...)
     *
     * @param path File path to database
     * @param databaseNameTag Optional tag name to help identify database in logging
     * @param tableDataCountCheck Optional check on a table for data. (optional)
     * @param allowZeroCount Optional tableDataCountCheck if false return false if count is zero
     *
     * @return true if validation check is OK
     */
    fun validateDatabaseFile(path: String, databaseNameTag: String = "", tableDataCountCheck: String = "", allowZeroCount: Boolean = true): Boolean {
        Timber.i("Checking database integrity for [%s]", databaseNameTag)
        val totalTimeMs = measureTimeMillis {
            try {
                SQLiteDatabase.openDatabase(path, null, SQLiteDatabase.OPEN_READONLY).use { database ->
                    // pragma check
                    if (!database.isDatabaseIntegrityOk) {
                        Timber.e("validateDatabase - database [%s] isDatabaseIntegrityOk check failed", databaseNameTag)
                        return false
                    }

                    // make sure there is data in the database
                    if (tableDataCountCheck.isNotBlank()) {
                        database.rawQuery("SELECT count(1) FROM $tableDataCountCheck", null).use { cursor ->
                            val count = if (cursor.moveToFirst()) {
                                cursor.getInt(0)
                            } else {
                                null
                            }

                            if (count == null || (!allowZeroCount && count == 0)) {
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
     * Check to see if a column in a database exists, if it does not... alter query will be run
     * @param database Sqlite database
     * @param tableName table for columnName
     * @param columnName column to from tableName to be checked
     * @param alterSql SQL to be run if the column does not exits. Example: alterTableIfColumnDoesNotExist(database, "individual", "middle_name", "ALTER TABLE individual ADD `middle_name` TEXT DEFAULT '' NOT NULL")
     *
     * @return true if there were no failures
     */
    fun alterTableIfColumnDoesNotExist(database: SQLiteDatabase, tableName: String, columnName: String, alterSql: String): Boolean {
        if (!tableExists(database, tableName)) {
            Timber.e("Cannot ALTER table [$tableName] that does not exist in database [${database.path}]")
            return false
        }

        if (!columnExists(database, tableName, columnName)) {
            Timber.i("Adding column [$columnName] to table [$tableName]")
            database.execSQL(alterSql)
        }

        return true
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
     * Remove the room master table and set the database version (default 0)
     *
     * @param database SQLite Database
     * @param newVersion version to be set on database (default to 0)
     */
    fun resetRoom(database: SQLiteDatabase, newVersion: Int = 0) {
        database.rawQuery("DROP TABLE IF EXISTS room_master_table", null)
        database.version = newVersion
    }

    /**
     * If the database should NOT have a migration and is a pre-populated database that should not be managed by Room... make sure Room migration is never needed.
     *
     * @param database SQLite Database
     * @param expectedVersion SQLite Database version (PRAGMA user_version)
     * @param expectedIdentityHash Hash that is expected.  If the expectedIdentityHash does not match the existing identity hash (currently in the room_master_table), then just delete the table
     */
    fun checkAndFixRoomIdentityHash(database: SQLiteDatabase, expectedVersion: Int, expectedIdentityHash: String) {
        if (expectedIdentityHash.isBlank()) {
            Timber.e("checkAndFixRoomIdentityHash -- expectedIdentityHash is blank")
            return
        }

        // set database version
        if (database.version != expectedVersion) {
            database.version = expectedVersion
        }

        // if we already have the correct identity hash
        if (tableExists(database, "room_master_table") && findRoomIdentityHash(database) == expectedIdentityHash) {
            // we are OK
            return
        }

        Timber.w("checkAndFixRoomIdentityHash -- updating expectedIdentityHash: [$expectedIdentityHash]")
        database.beginTransaction()
        try {
            database.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)", emptyArray())
            database.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '$expectedIdentityHash')", emptyArray())
            database.setTransactionSuccessful()
        } finally {
            database.endTransaction()
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

    private const val CORRUPTION_CHECK_PASSED = "ok"
}