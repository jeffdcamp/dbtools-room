package org.dbtools.android.room.jdbctest.extensions.testdata

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

// Sample database migration for testing the migration extension and base class
class Migrate12Good : Migration(1, 2) {

    override fun migrate(database: SupportSQLiteDatabase) {
        database.beginTransaction()
        try {
            database.execSQL("ALTER TABLE `testEntity` ADD COLUMN `coolFact` TEXT")
            database.setTransactionSuccessful()
        } finally {
            database.endTransaction()
        }
    }
}

// Sample migration intended to create a failure when testing if the migration was successful
class Migrate12Bad : Migration(1, 2) {

    override fun migrate(database: SupportSQLiteDatabase) {
        database.beginTransaction()
        try {
            database.execSQL("ALTER TABLE `testEntity` ADD COLUMN `coolFact2` TEXT")
            database.setTransactionSuccessful()
        } finally {
            database.endTransaction()
        }
    }
}