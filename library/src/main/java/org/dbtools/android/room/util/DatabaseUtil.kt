package org.dbtools.android.room.util

import android.arch.persistence.room.RoomDatabase
import android.content.Context
import timber.log.Timber
import java.io.File
import java.io.FileFilter
import kotlin.system.measureTimeMillis

@Suppress("unused")
object DatabaseUtil {
    private const val CORRUPTION_CHECK_PASSED = "ok"

    /**
     * Preform a PRAGMA check on the database and optionally check a table for existing data
     *
     * @param roomDatabase Database to be validated
     * @param databaseNameTag Optional tag name to help identify database in logging
     * @param tableDataCountCheck Optional check on a table for data. (optional)
     *
     * @return true if validation check is OK
     */
    fun validDatabaseFile(roomDatabase: RoomDatabase, databaseNameTag: String = "", tableDataCountCheck: String = ""): Boolean {
        Timber.i("Checking database integrity for [%s]", databaseNameTag)
        val totalTimeMs = measureTimeMillis {
            try {
                val database = roomDatabase.openHelper.readableDatabase

                // pragma check
                database.query("pragma quick_check", null).use { pragmaCheckCursor ->
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
                    database.query("SELECT count(1) FROM $tableDataCountCheck", null).use { cursor ->
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
            } catch (e: Exception) {
                Timber.e(e, "Failed to validate database [$databaseNameTag]")
                return false
            }
        }

        Timber.i("Database integrity for [$databaseNameTag]  OK! (${totalTimeMs}ms)")
        return true
    }

    /**
     * Deletes a database including its journal file and other auxiliary files
     * that may have been created by the database engine.
     *
     * @param file The database file path.
     * @return true if the database was successfully deleted.
     */
    fun deleteDatabaseFiles(file: File): Boolean {
        var deleted: Boolean
        deleted = file.delete()
        deleted = deleted or File("${file.path}-journal").delete()
        deleted = deleted or File("${file.path}-shm").delete()
        deleted = deleted or File("${file.path}-wal").delete()

        val dir = file.parentFile
        if (dir != null) {
            val prefix = "${file.name}-mj"
            val filter = FileFilter { candidate -> candidate.name.startsWith(prefix) }
            for (masterJournal in dir.listFiles(filter)) {
                deleted = deleted or masterJournal.delete()
            }
        }
        return deleted
    }

    /**
     * Renames a database including its journal file and other auxiliary files
     * that may have been created by the database engine.
     *
     * @param srcFile The database file path.
     * @return true if the database was successfully rename.
     */
    fun renameDatabaseFiles(srcFile: File, targetFile: File): Boolean {
        var renamed: Boolean
        renamed = srcFile.renameTo(File(targetFile.path))
        renamed = renamed or File("${srcFile.path}-journal").renameTo(File("${targetFile.path}-journal"))
        renamed = renamed or File("${srcFile.path}-shm").renameTo(File("${targetFile.path}-shm"))
        renamed = renamed or File("${srcFile.path}-wal").renameTo(File("${targetFile.path}-wal"))

        // delete srcFile -mj files
        val dir = srcFile.parentFile
        if (dir != null) {
            val prefix = "${srcFile.name}-mj"
            val filter = FileFilter { candidate -> candidate.name.startsWith(prefix) }
            for (masterJournal in dir.listFiles(filter)) {
                renamed = renamed or masterJournal.delete()
            }
        }
        return renamed
    }

    /**
     * Copy database from assets to <app>/database
     *
     * @param context Android context
     * @param databaseFilename Name of file in assets
     * @param overwrite Overwrite the target file
     *
     * @return File of the copied database
     */
    fun copyDatabaseFromAssets(context: Context, databaseFilename: String, overwrite: Boolean = false): File {
        val newDatabaseFile = context.getDatabasePath(databaseFilename)
        val assetDatabaseInputStream = context.assets.open(databaseFilename)

        if (newDatabaseFile.exists() && overwrite) {
            DatabaseUtil.deleteDatabaseFiles(newDatabaseFile)
        }

        assetDatabaseInputStream.use { input ->
            newDatabaseFile.outputStream().use { fileOut ->
                input.copyTo(fileOut)
            }
        }

        return newDatabaseFile
    }
}