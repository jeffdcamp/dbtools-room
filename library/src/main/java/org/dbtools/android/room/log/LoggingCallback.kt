package org.dbtools.android.room.log

import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity

class LoggingCallback(
    private val logCreate: Boolean = false,
    private val logOpen: Boolean = false,
    private val logDestructiveMigration: Boolean = true,
    private val logPriorityLevel: Severity = Severity.Debug,
) : RoomDatabase.Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) {
        if (logCreate) {
            Logger.log(logPriorityLevel, "LoggingCallback", null, "Created ${db.path}")
        }
    }

    override fun onOpen(db: SupportSQLiteDatabase) {
        if (logOpen) {
            Logger.log(logPriorityLevel, "LoggingCallback", null, "Opened ${db.path}")
        }
    }

    override fun onDestructiveMigration(db: SupportSQLiteDatabase) {
        if (logDestructiveMigration) {
            Logger.log(logPriorityLevel, "LoggingCallback", null, "Migrated Destructively ${db.path}")
        }
    }
}