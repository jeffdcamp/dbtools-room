package org.dbtools.room.ext

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import co.touchlab.kermit.Logger

/**
 * Merge database tables from other databases.
 *
 * By default all tables (except Room system tables) will be merged
 *
 * @param otherDatabasePath Sqlite file that will be opened and attached to the targetDatabase... then data will be copied from this database File
 * @param includeTables Only table names in this list will be merged. Table names are source database table names.  default: emptyList()
 * @param excludeTables All tables except the table names in this list will be merged. Table names are source database table names.  default: emptyList()
 * @param sourceTableNameMap Map of name changes from sourceTable to targetTable (Example: copy table data from sourceDatabase.foo to targetDatabase.bar).
 * @param onFailBlock Code to execute if there is a failure during merge
 * Key is the source table name, value is the target table name
 * @param mergeBlock Code to execute to perform merge.  default: database.execSQL("INSERT OR IGNORE INTO $tableName SELECT * FROM $sourceTableName")
 *
 * @return true if merge was successful
 *
 * NOTE:  Room system tables are automatically excluded from the merge
 *
 *
 * Add the following function to the RoomDatabase class (mDatabase is ONLY accessible from inside the RoomDatabase class)
 *     fun mergeDataFromOtherDatabase(sourceDatabaseFile: File, includeTables: List<String> = emptyList(), excludeTables: List<String> = emptyList()) {
 *         // make sure database is open
 *         if (!isOpen) {
 *             openHelper.writableDatabase
 *         }
 *
 *         // merge database
 *         mDatabase.mergeDatabase(sourceDatabaseFile, includeTables, excludeTables)
 *     }
 *
 */
@Suppress("NestedBlockDepth")
fun SQLiteConnection.mergeDatabase(
    otherDatabasePath: String,
    includeTables: List<String> = emptyList(),
    excludeTables: List<String> = emptyList(),
    sourceTableNameMap: Map<String, String> = emptyMap(),
    onFailBlock: ((e: Exception, targetConnection: SQLiteConnection) -> Unit)? = null,
    mergeBlock: (sqLiteConnection: SQLiteConnection, sourceTableName: String, targetTableName: String) -> Unit = { sqLiteConnection, sourceTableName, targetTableName ->
        sqLiteConnection.defaultMerge(sourceTableName, targetTableName)
    }
): Boolean {
    val mergeDbName = "merge_db"

    try {
        // Attach sourceDatabase with primary
        attachDatabase(otherDatabasePath, mergeDbName)

        // Get a list of tables to merge
        val sourceTableNames = findTableNames(mergeDbName)
        val targetTableNames = findTableNames()

        val tableNamesToMerge = createTableNamesToMerge(sourceTableNames, includeTables, excludeTables, sourceTableNameMap)

        // verify the remaining tables actually exist in the target database
        tableNamesToMerge.forEach {
            if (!targetTableNames.contains(it.targetTableName)) {
                Logger.e { "Table does not exist in target database: [${it.targetTableName}]" }
                return false
            }
        }

        // Merge table content
        beginTransaction()

        try {
            tableNamesToMerge.forEach { mergeTable ->
                if (sourceTableNames.contains(mergeTable.sourceTableName)) {
                    val sourceTableName = "$mergeDbName.${mergeTable.sourceTableName}"

                    Logger.i { "Merging [$sourceTableName] INTO [${mergeTable.targetTableName}]" }
                    mergeBlock(this, sourceTableName, mergeTable.targetTableName) // default: database.execSQL("INSERT OR IGNORE INTO $tableName SELECT * FROM $sourceTableName")
                } else {
                    Logger.w { "WARNING: Cannot merge table [${mergeTable.sourceTableName}]... it does not exist in sourceDatabaseFile... skipping..." }
                }
            }

            endTransaction()
        } catch (expected: Exception) {
            Logger.e(expected) { "Failed to merge database tables (inner) (sourceDatabaseFile: [${otherDatabasePath}]" }
            onFailBlock?.invoke(expected, this)
            return false
        } finally {
            rollbackTransaction()
        }
    } catch (expected: Exception) {
        Logger.e(expected) { "Failed to merge database tables (outer) (sourceDatabaseFile: [${otherDatabasePath}]" }
        onFailBlock?.invoke(expected, this)
        return false
    } finally {
        try {
            // Detach databases
            detachDatabase(mergeDbName)
        } catch (expected: Exception) {
            Logger.e(expected) { "Failed detach database (merge database tables)... may have never been attached" }
            onFailBlock?.invoke(expected, this)
        }
    }

    return true
}

private fun createTableNamesToMerge(
    sourceTableNames: List<String>,
    includeTables: List<String> = emptyList(),
    excludeTables: List<String> = emptyList(),
    sourceTableNameMap: Map<String, String> = emptyMap()
): List<MergeTable> {
    // only include tables that are in "includeTables"... if empty use ALL tableNames
    var tableNamesToMerge = if (includeTables.isNotEmpty()) {
        sourceTableNames.filter { includeTables.contains(it) }.map { sourceTableName ->
            MergeTable(sourceTableName, getTargetTableName(sourceTableNameMap, sourceTableName))
        }
    } else {
        // add all target tableNames
        sourceTableNames.map { sourceTableName ->
            MergeTable(sourceTableName, getTargetTableName(sourceTableNameMap, sourceTableName))
        }
    }

    if (includeTables.isNotEmpty() && tableNamesToMerge.size != includeTables.size) {
        Logger.e { "WARNING one or more of the tables in the include list was not found in this database" }
    }

    // remove excluded tableNames
    tableNamesToMerge = tableNamesToMerge.filter { !excludeTables.contains(it.sourceTableName) }

    // remove system tableNames
    tableNamesToMerge = tableNamesToMerge.filter { !SYSTEM_TABLES.contains(it.targetTableName) }

    return tableNamesToMerge
}

private fun SQLiteConnection.defaultMerge(sourceTableName: String, targetTableName: String) {
    execSQL("INSERT OR IGNORE INTO $targetTableName SELECT * FROM $sourceTableName")
}

private fun getTargetTableName(sourceToTargetTableMap: Map<String, String>, sourceTableName: String): String {
    return sourceToTargetTableMap[sourceTableName] ?: sourceTableName
}

private val SYSTEM_TABLES = listOf("room_master_table", "sqlite_sequence", "android_metadata")

data class MergeTable(val sourceTableName: String, val targetTableName: String)
