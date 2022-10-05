package org.dbtools.android.room.jdbc

object JdbcDatabaseUtil {
    /**
     * Check to see if a column in a database exists, if it does not... alter query will be run
     * @param database Sqlite database
     * @param tableName table for columnName
     * @param columnName column to from tableName to be checked
     * @param alterSql SQL to be run if the column does not exits.
     * Example: alterTableIfColumnDoesNotExist(database, "individual", "middle_name", "ALTER TABLE individual ADD `middle_name` TEXT DEFAULT '' NOT NULL")
     *
     * @return true if there were no failures
     */
    fun alterTableIfColumnDoesNotExist(database: JdbcSqliteDatabase, tableName: String, columnName: String, alterSql: String): Boolean {
        if (!tableExists(database, tableName)) {
            println("Cannot ALTER table [$tableName] that does not exist in database [${database.path}]")
            return false
        }

        if (!columnExists(database, tableName, columnName)) {
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
    fun tableExists(database: JdbcSqliteDatabase, tableName: String): Boolean {
        var tableExists = false

        database.query("SELECT count(1) FROM sqlite_master WHERE type='table' AND name='$tableName'").use { cursor ->
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
    @Suppress("NestedBlockDepth")
    fun columnExists(database: JdbcSqliteDatabase, tableName: String, columnName: String): Boolean {
        var columnExists = false

        database.query("PRAGMA table_info($tableName)").use { cursor ->
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
     * Remove the room master table and set the database version (default 0)
     *
     * @param database SQLite Database
     * @param newVersion version to be set on database (default to 0)
     */
    fun resetRoom(database: JdbcSqliteDatabase, newVersion: Int = 0) {
        database.execSQL("DROP TABLE IF EXISTS room_master_table")
        database.version = newVersion
    }

    /**
     * If the database should NOT have a migration and is a pre-populated database that should not be managed by Room... make sure Room migration is never needed.
     *
     * @param database SQLite Database
     * @param expectedVersion SQLite Database version (PRAGMA user_version)
     * @param expectedIdentityHash Hash that is expected.  If the expectedIdentityHash does not match the existing identity hash (currently in the room_master_table), then just delete the table
     */
    fun checkAndFixRoomIdentityHash(database: JdbcSqliteDatabase, expectedVersion: Int, expectedIdentityHash: String) {
        if (expectedIdentityHash.isBlank()) {
            println("checkAndFixRoomIdentityHash -- expectedIdentityHash is blank")
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

        println("checkAndFixRoomIdentityHash -- updating expectedIdentityHash: [$expectedIdentityHash]")
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
    fun findRoomIdentityHash(database: JdbcSqliteDatabase): String? {
        var identityHash: String? = null

        // NOTE: the id column for this table always seems to be 42 and there is always only 1 row... so lets just find the first row
        database.query("SELECT identity_hash FROM room_master_table LIMIT 1").use { cursor ->
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