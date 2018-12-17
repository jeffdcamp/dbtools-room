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
}