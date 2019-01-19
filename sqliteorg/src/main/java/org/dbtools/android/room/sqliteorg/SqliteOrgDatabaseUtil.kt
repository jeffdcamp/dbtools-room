package org.dbtools.android.room.sqliteorg

import org.sqlite.database.sqlite.SQLiteDatabase
import timber.log.Timber
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
        database.rawQuery("DROP TABLE IF EXISTS room_master_table", null)
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
            Timber.w("checkAndFixRoomIdentityHash -- expectedIdentityHash: [$expectedIdentityHash] != room_master_table.identity_hash [$identityHash]... removing room_master_table")
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

    private const val CORRUPTION_CHECK_PASSED = "ok"
}