package org.dbtools.room.ext

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteDriver

/**
 * If the database should NOT have a migration and is a pre-populated database that should not be managed by Room... make sure Room migration is never needed.
 *
 * NOTE: this SHOULD be called BEFORE room has a chance to open the database and verify the database
 *
 * Example Usage:
 *     val driver = BundledSQLiteDriver()
 *     driver.checkAndFixRoomIdentityHash(mySqliteDbFileName, MyDatabase.DATABASE_VERSION, MyDatabase.ROOM_DATABASE_IDENTITY_HASH)
 *
 * @param fileName File name to SQLite Database
 * @param expectedVersion SQLite Database version (PRAGMA user_version)
 * @param expectedIdentityHash Hash that is expected.  If the expectedIdentityHash does not match the existing identity hash (currently in the room_master_table), then just delete the table
 * @param extraSetupBlock This may be useful for doing any additional setup that is needed after the identity hash is checked and fixed.
 */
fun SQLiteDriver.checkAndFixRoomIdentityHash(fileName: String, expectedVersion: Int, expectedIdentityHash: String, extraSetupBlock: (SQLiteConnection) -> Unit = {  }) {
    val connection = open(fileName)
    connection.checkAndFixRoomIdentityHash(expectedVersion, expectedIdentityHash)
    extraSetupBlock(connection)
    connection.close()
}

/**
 * Preform a PRAGMA check on the database and optionally check a table for existing data.
 *
 * @param fileName File name to SQLite Database
 * @param tag Optional tag name to help identify database in logging
 * @param tableDataCountCheck Optional check on a table for data. (optional)
 * @param allowZeroCount Optional tableDataCountCheck if false return false if count is zero
 *
 * @return true if validation check is OK
 */
fun SQLiteDriver.validateDatabase(fileName: String, tag: String = "", tableDataCountCheck: String? = null, allowZeroCount: Boolean = true): Boolean {
    val connection = open(fileName)
    val success = connection.validateDatabase(tag, tableDataCountCheck, allowZeroCount)
    connection.close()

    return success
}

