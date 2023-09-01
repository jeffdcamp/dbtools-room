package org.dbtools.android.room.android

import android.database.DatabaseErrorHandler
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.util.Pair
import co.touchlab.kermit.Logger
import org.dbtools.android.room.log.DbToolsRoomCrashLogException
import org.dbtools.android.room.util.DatabaseUtil
import java.io.File

/**
 * Copied from android.database.DefaultDatabaseErrorHandler
 *
 * Changed Log.e -> Logger.e so that app can post to error logs
 */
@Suppress("NestedBlockDepth")
object AndroidDefaultDatabaseErrorHandler : DatabaseErrorHandler {
    override fun onCorruption(dbObj: SQLiteDatabase?) {
        Logger.e(DbToolsRoomCrashLogException()) { "Corruption reported by sqlite on database: [${dbObj?.path}]" }
//        SQLiteDatabase.wipeDetected(dbObj.path, "corruption")

        // is the corruption detected even before database could be 'opened'?
        if (dbObj?.isOpen == false) {
            // database files are not even openable. delete this database file.
            // NOTE if the database has attached databases, then any of them could be corrupt.
            // and not deleting all of them could cause corrupted database file to remain and
            // make the application crash on database open operation. To avoid this problem,
            // the application should provide its own {@link DatabaseErrorHandler} impl class
            // to delete ALL files of the database (including the attached databases).
            DatabaseUtil.deleteDatabaseFiles(File(dbObj.path))
            return
        }

        var attachedDbs: List<Pair<String?, String?>>? = null
        try {
            // Close the database, which will cause subsequent operations to fail.
            // before that, get the attached database list first.
            try {
                attachedDbs = dbObj?.attachedDbs
            } catch (_: SQLiteException) {
                /* ignore */
            }
            try {
                dbObj?.close()
            } catch (_: SQLiteException) {
                /* ignore */
            }
        } finally {
            // Delete all files of this corrupt database and/or attached databases
            if (attachedDbs != null) {
                for (p in attachedDbs) {
                    p.second?.let { DatabaseUtil.deleteDatabaseFiles(File(it)) }
                }
            } else {
                // attachedDbs = null is possible when the database is so corrupt that even
                // "PRAGMA database_list;" also fails. delete the main database file
                dbObj?.let { DatabaseUtil.deleteDatabaseFiles(File(it.path)) }
            }
        }
    }
}
