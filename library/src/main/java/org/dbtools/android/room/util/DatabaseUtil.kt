package org.dbtools.android.room.util

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import org.dbtools.android.room.DatabaseViewQuery
import timber.log.Timber
import java.io.File
import java.io.FileFilter
import kotlin.system.measureTimeMillis

@Suppress("unused")
object DatabaseUtil {
    private const val CORRUPTION_CHECK_PASSED = "ok"

    /**
     * Preform a PRAGMA check on the database and optionally check a table for existing data.  This check can be used to check a
     * database prior to hooking it up to Room and running database table upgrades and versioning (useful for pre-existing databases)
     *
     * NOTE: This call will use the built-in default Android Sqlite Database Libraries... see also SqliteOrgDatabaseUtil.validateDatabaseFile(...)
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

    /**
     * Preform a PRAGMA check on the database and optionally check a table for existing data
     *
     * @param roomDatabase RoomDatabase to be validated
     * @param databaseNameTag Optional tag name to help identify database in logging
     * @param tableDataCountCheck Optional check on a table for data. (optional)
     *
     * @return true if validation check is OK
     */
    fun validDatabaseFile(roomDatabase: RoomDatabase, databaseNameTag: String = "", tableDataCountCheck: String = ""): Boolean {
        val database = roomDatabase.openHelper.readableDatabase
        return validDatabaseFile(database, databaseNameTag, tableDataCountCheck)
    }

    /**
     * Preform a PRAGMA check on the database and optionally check a table for existing data
     *
     * @param database SupportSQLiteDatabase to be validated
     * @param databaseNameTag Optional tag name to help identify database in logging
     * @param tableDataCountCheck Optional check on a table for data. (optional)
     *
     * @return true if validation check is OK
     */
    fun validDatabaseFile(database: SupportSQLiteDatabase, databaseNameTag: String = "", tableDataCountCheck: String = ""): Boolean {
        Timber.i("Checking database integrity for [%s]", databaseNameTag)
        val totalTimeMs = measureTimeMillis {
            try {
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

    fun dropView(database: SupportSQLiteDatabase, viewName: String) {
        database.execSQL("DROP VIEW IF EXISTS $viewName")
    }

    fun dropAllViews(database: SupportSQLiteDatabase, views: List<DatabaseViewQuery>) {
        views.forEach { dropView(database, it.viewName) }
    }

    fun createView(database: SupportSQLiteDatabase, viewName: String, viewQuery: String) {
        database.execSQL("CREATE VIEW `$viewName` AS $viewQuery")
    }

    fun createAllViews(database: SupportSQLiteDatabase, views: List<DatabaseViewQuery>) {
        views.forEach { createView(database, it.viewName, it.viewQuery) }
    }

    fun recreateView(database: SupportSQLiteDatabase, viewName: String, viewQuery: String) {
        dropView(database, viewName)
        createView(database, viewName, viewQuery)
    }

    fun recreateAllViews(database: SupportSQLiteDatabase, views: List<DatabaseViewQuery>) {
        views.forEach { recreateView(database, it.viewName, it.viewQuery) }
    }
}