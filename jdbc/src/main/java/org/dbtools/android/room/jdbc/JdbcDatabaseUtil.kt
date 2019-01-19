package org.dbtools.android.room.jdbc

object JdbcDatabaseUtil {
    /**
     * Check to see if a column in a database exists, if it does not... alter query will be run
     * @param database Sqlite database
     * @param tableName table for columnName
     * @param columnName column to from tableName to be checked
     * @param alterSql SQL to be run if the column does not exits. Example: alterTableIfColumnDoesNotExist(database, "individual", "middle_name", "ALTER TABLE individual ADD `middle_name` TEXT DEFAULT '' NOT NULL")
     */
    fun alterTableIfColumnDoesNotExist(database: JdbcSqliteDatabase, tableName: String, columnName: String, alterSql: String) {
        if (!columnExists(database, tableName, columnName)) {
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
    fun tableExists(database: JdbcSqliteDatabase, tableName: String): Boolean {
        var tableExists = false

        database.query("SELECT count(1) FROM sqlite_master WHERE type='table' AND name='$tableName'", null).use { cursor ->
            if (cursor.moveToFirst()) {
                val count = cursor.getInt(0)
                if (count > 0) {
                    tableExists = true
                }
            } else {
                println("Query: [SELECT count(1) FROM sqlite_master WHERE type='table' AND name='$tableName'] returned NO data")
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
    fun columnExists(database: JdbcSqliteDatabase, tableName: String, columnName: String): Boolean {
        var columnExists = false

        database.query("PRAGMA table_info($tableName)", null).use { cursor ->
            if (cursor.moveToFirst()) {
                do {
                    val currentColumn = cursor.getString(cursor.getColumnIndexOrThrow("name"))
                    if (currentColumn == columnName) {
                        columnExists = true
                    }
                } while (!columnExists && cursor.moveToNext())
            } else {
                println("Query: [PRAGMA table_info($tableName)] returned NO data")
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
    fun resetRoom(database: JdbcSqliteDatabase, newVersion: Int = 0) {
        database.execSQL("DROP TABLE IF EXISTS room_master_table", null)
        database.version = newVersion
    }

    /**
     * If the database should NOT have a migration and is a pre-populated database that should not be managed by Room... make sure Room migration is never needed.
     *
     * @param database SQLite Database
     * @param expectedIdentityHash Hash that is expected.  If the expectedIdentityHash does not match the existing identity hash (currently in the room_master_table), then just delete the table
     */
    fun checkAndFixRoomIdentityHash(database: JdbcSqliteDatabase, expectedIdentityHash: String) {
        if (!tableExists(database, "room_master_table")) {
            // We are OK... the table does not exist
            return
        }

        val identityHash = findRoomIdentityHash(database)
        if (identityHash.isNullOrBlank()) {
            println("checkAndFixRoomIdentityHash -- room_master_table.identity_hash was null or blank... making sure room_master_table does not exist")
            resetRoom(database)
        } else if (identityHash != expectedIdentityHash) {
            println("checkAndFixRoomIdentityHash -- expectedIdentityHash: [$expectedIdentityHash] !=  room_master_table.identity_hash [$identityHash]... removing room_master_table")
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
    fun findRoomIdentityHash(database: JdbcSqliteDatabase): String? {
        var identityHash: String? = null

        // NOTE: the id column for this table always seems to be 42 and there is always only 1 row... so lets just find the first row
        database.query("SELECT identity_hash FROM room_master_table LIMIT 1", null).use { cursor ->
            if (cursor.moveToFirst()) {
                val columnIndex = cursor.getColumnIndex("identity_hash")
                identityHash = if (columnIndex == -1) {
                    // does not exist
                    null
                } else {
                    cursor.getString(columnIndex)
                }
            } else {
                println("Query: [SELECT identity_hash FROM room_master_table LIMIT 1] returned NO data")
            }
        }

        return identityHash
    }
}