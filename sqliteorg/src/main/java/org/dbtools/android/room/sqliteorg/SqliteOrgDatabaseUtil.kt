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

    private const val CORRUPTION_CHECK_PASSED = "ok"
}