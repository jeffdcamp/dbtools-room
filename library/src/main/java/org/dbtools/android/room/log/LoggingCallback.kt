package org.dbtools.android.room.log

import android.util.Log
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import timber.log.Timber

class LoggingCallback(
    private val logCreate: Boolean = false,
    private val logOpen: Boolean = false,
    private val logDestructiveMigration: Boolean = true,
    private val logPriorityLevel: Int = Log.DEBUG
) : RoomDatabase.Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) {
        if (logCreate) {
            Timber.log(logPriorityLevel, "Created ${db.path}")
        }
    }

    override fun onOpen(db: SupportSQLiteDatabase) {
        if (logOpen) {
            Timber.log(logPriorityLevel, "Opened ${db.path}")
        }
    }

    override fun onDestructiveMigration(db: SupportSQLiteDatabase) {
        if (logDestructiveMigration) {
            Timber.log(logPriorityLevel, "Migrated Destructively ${db.path}")
        }
    }
}