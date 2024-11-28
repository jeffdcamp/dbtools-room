package org.dbtools.room.ext

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
 */
fun SQLiteDriver.checkAndFixRoomIdentityHash(fileName: String, expectedVersion: Int, expectedIdentityHash: String) {
    val connection = open(fileName)
    connection.checkAndFixRoomIdentityHash(expectedVersion, expectedIdentityHash)
    connection.close()
}
