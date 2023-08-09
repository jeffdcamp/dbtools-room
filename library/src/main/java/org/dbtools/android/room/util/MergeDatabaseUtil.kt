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
     * @param targetDatabase mDatabase variable from the RoomDatabase class
     * @param sourceDatabaseFile Sqlite file that will be opened and attached to the targetDatabase... then data will be copied from this database File
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
    fun mergeDatabase(
        targetDatabase: SupportSQLiteDatabase,
        sourceDatabaseFile: File,
        includeTables: List<String> = emptyList(),
        excludeTables: List<String> = emptyList(),
        sourceTableNameMap: Map<String, String> = emptyMap(),
        onFailBlock: ((e: Exception, targetDatabase: SupportSQLiteDatabase, sourceDatabaseFile: File) -> Unit)? = null,
        mergeBlock: (database: SupportSQLiteDatabase, sourceTableName: String, targetTableName: String) -> Unit = { db, sourceTableName, targetTableName ->
                defaultMerge(db, sourceTableName, targetTableName)
            }
    ): Boolean {
        if (!sourceDatabaseFile.exists()) {
            Timber.e("Failed to merged [${sourceDatabaseFile.absolutePath}] into targetDatabase :: sourceDatabaseFile database does not exist")
            return false
        }

        val mergeDbName = "merge_db"

        if (!targetDatabase.isOpen) {
            Timber.e("Failed to merge [targetDatabase is not open]")
            return false
        }

        try {
            // Attach sourceDatabase with primary
            targetDatabase.attachDatabase(sourceDatabaseFile.absolutePath, mergeDbName)

            // Get a list of tables to merge
            val sourceTableNames = targetDatabase.findTableNames(mergeDbName)
            val targetTableNames = targetDatabase.findTableNames()

            val tableNamesToMerge = createTableNamesToMerge(sourceTableNames, includeTables, excludeTables, sourceTableNameMap)

            // verify the remaining tables actually exist in the target database
            tableNamesToMerge.forEach {
                if (!targetTableNames.contains(it.targetTableName)) {
                    Timber.e("Table does not exist in target database: [${it.targetTableName}]")
                    return false
                }
            }

            // Merge table content
            targetDatabase.beginTransaction()

            try {
                tableNamesToMerge.forEach { mergeTable ->
                    if (sourceTableNames.contains(mergeTable.sourceTableName)) {
                        val sourceTableName = "$mergeDbName.${mergeTable.sourceTableName}"

                        Timber.i("Merging [$sourceTableName] INTO [${mergeTable.targetTableName}]")
                        mergeBlock(targetDatabase, sourceTableName, mergeTable.targetTableName) // default: database.execSQL("INSERT OR IGNORE INTO $tableName SELECT * FROM $sourceTableName")
                    } else {
                        Timber.w("WARNING: Cannot merge table [${mergeTable.sourceTableName}]... it does not exist in sourceDatabaseFile... skipping...")
                    }
                }

                targetDatabase.setTransactionSuccessful()
            } catch (expected: Exception) {
                Timber.e(expected, "Failed to merge database tables (inner) (sourceDatabaseFile: [${sourceDatabaseFile.name}] targetDatabase: [${targetDatabase.path}]")
                onFailBlock?.invoke(expected, targetDatabase, sourceDatabaseFile)
                return false
            } finally {
                targetDatabase.endTransaction()
            }
        } catch (expected: Exception) {
            Timber.e(expected, "Failed to merge database tables (outer) (sourceDatabaseFile: [${sourceDatabaseFile.name}] targetDatabase: [${targetDatabase.path}]")
            onFailBlock?.invoke(expected, targetDatabase, sourceDatabaseFile)
            return false
        } finally {
            try {
                // Detach databases
                targetDatabase.detachDatabase(mergeDbName)
            } catch (expected: Exception) {
                Timber.e(expected, "Failed detach database (merge database tables)... may have never been attached")
                onFailBlock?.invoke(expected, targetDatabase, sourceDatabaseFile)
            }
        }

        return true
    }

    fun createTableNamesToMerge(
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
            Timber.e("WARNING one or more of the tables in the include list was not found in this database")
        }

        // remove excluded tableNames
        tableNamesToMerge = tableNamesToMerge.filter { !excludeTables.contains(it.sourceTableName) }

        // remove system tableNames
        tableNamesToMerge = tableNamesToMerge.filter { !SYSTEM_TABLES.contains(it.targetTableName) }

        return tableNamesToMerge
    }

    fun defaultMerge(database: SupportSQLiteDatabase, sourceTableName: String, targetTableName: String) {
        database.execSQL("INSERT OR IGNORE INTO $targetTableName SELECT * FROM $sourceTableName")
    }

    private fun getTargetTableName(sourceToTargetTableMap: Map<String, String>, sourceTableName: String): String {
        return sourceToTargetTableMap[sourceTableName] ?: sourceTableName
    }

    private val SYSTEM_TABLES = listOf("room_master_table", "sqlite_sequence", "android_metadata")
}

data class MergeTable(val sourceTableName: String, val targetTableName: String)


