package org.dbtools.android.room.util

import androidx.sqlite.db.SupportSQLiteDatabase
import org.dbtools.android.room.ext.attachDatabase
import org.dbtools.android.room.ext.detachDatabase
import org.dbtools.android.room.ext.findTableNames
import timber.log.Timber
import java.io.File

/**
 *  Tools to attach and merge data between databases
 */
object MergeDatabaseUtil {

    /**
     * Merge database tables from other databases.
     *
     * By default all tables (except Room system tables) will be merged)
     *
     * @param database mDatabase variable from the RoomDatabase class
     * @param fromDatabaseFile Sqlite file that will be opened and attached to this database... then data will be copied from this database File
     * @param includeTables Only table names in this list will be merged.  default: emptyList()
     * @param excludeTables All tables except the table names in this list will be merged.  default: emptyList()
     * @param tableNameMap Map of name changes in target database (Example: copy table data from databaseA.foo to databaseB.bar).  Key is the source table name, value is the target table name
     * @param mergeBlock Code to execute to perform merge.  default: database.execSQL("INSERT OR IGNORE INTO $tableName SELECT * FROM $sourceTableName")
     *
     * NOTE:  Room system tables are automatically excluded from the merge
     *
     *
     * Add the following function to the RoomDatabase class (mDatabase is ONLY accessable from inside the RoomDatabase class)
     *     fun mergeDataFromOtherDatabase(fromDatabaseFile: File, includeTables: List<String> = emptyList(), excludeTables: List<String> = emptyList()) {
     *         // make sure database is open
     *         if (!isOpen) {
     *             openHelper.writableDatabase
     *         }
     *
     *         // merge database
     *         mDatabase.mergeDatabase(fromDatabaseFile, includeTables, excludeTables)
     *     }
     *
     * @return true if merge was successful
     */
    fun mergeDatabase(
        database: SupportSQLiteDatabase,
        fromDatabaseFile: File,
        includeTables: List<String> = emptyList(),
        excludeTables: List<String> = emptyList(),
        tableNameMap: Map<String, String> = emptyMap(),
        mergeBlock: (database: SupportSQLiteDatabase, sourceTableName: String, targetTableName: String) -> Unit = { db, sourceTableName, targetTableName ->
            defaultMerge(db, sourceTableName, targetTableName)
        }
    ): Boolean {
        if (!fromDatabaseFile.exists()) {
            Timber.e("Failed to merged [${fromDatabaseFile.absolutePath}] into [$database] :: fromDatabaseFile database does not exist")
            return false
        }

        val mergeDbName = "merge_db"

        if (!database.isOpen) {
            Timber.e("Failed to merge [Database is not open]")
            return false
        }

        try {
            // Attach fromDatabase with primary
            database.attachDatabase(fromDatabaseFile.absolutePath, mergeDbName)

            // Get a list of tables to merge
            val tableNames = database.findTableNames()
            val otherTableNames = database.findTableNames(mergeDbName)

            // only include tables that are in "includeTables"... if empty use ALL tableNames
            var tableNamesToMerge = if (includeTables.isNotEmpty()) {
                tableNames.filter { includeTables.contains(it) }
            } else {
                tableNames
            }

            if (includeTables.isNotEmpty() && tableNamesToMerge.size != includeTables.size) {
                Timber.e("WARNING one or more of the tables in the include list was not found in this database")
            }

            // remove excluded tableNames
            tableNamesToMerge = tableNamesToMerge.filter { !excludeTables.contains(it) }

            // remove system tableNames
            tableNamesToMerge = tableNamesToMerge.filter { !SYSTEM_TABLES.contains(it) }

            // Merge table content
            database.beginTransaction()

            try {
                tableNamesToMerge.forEach { tableName ->
                    if (otherTableNames.contains(tableName)) {
                        val sourceTableName = "$mergeDbName.$tableName"

                        // check to see if the target database should use a different table name for this table
                        val targetTableName = if (tableNameMap.containsKey(tableName)) {
                            tableNameMap[tableName] ?: tableName
                        } else {
                            tableName
                        }

                        Timber.i("Merging [$sourceTableName] INTO [$targetTableName]")
                        mergeBlock(database, sourceTableName, targetTableName) // default: database.execSQL("INSERT OR IGNORE INTO $tableName SELECT * FROM $sourceTableName")
                    } else {
                        Timber.w("WARNING: Cannot merge table [$tableName]... it does not exist in fromDatabaseFile... skipping...")
                    }
                }

                database.setTransactionSuccessful()
            } catch (e: Exception) {
                Timber.e(e, "Failed to merge database tables")
                return false
            } finally {
                database.endTransaction()
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to merge database tables")
            return false
        } finally {
            try {
                // Detach databases
                database.detachDatabase(mergeDbName)
            } catch (e: Exception) {
                Timber.e(e, "Failed detach database (merge database tables)... may have never been attached")
            }
        }


        return true
    }

    fun defaultMerge(database: SupportSQLiteDatabase, sourceTableName: String, targetTableName: String) {
        database.execSQL("INSERT OR IGNORE INTO $targetTableName SELECT * FROM $sourceTableName")
    }

    private val SYSTEM_TABLES = listOf("room_master_table", "sqlite_sequence")
}


