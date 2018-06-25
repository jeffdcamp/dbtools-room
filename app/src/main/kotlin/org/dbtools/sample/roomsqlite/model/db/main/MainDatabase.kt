package org.dbtools.sample.roomsqlite.model.db.main

import android.arch.persistence.room.Database
import android.arch.persistence.room.RoomDatabase
import android.arch.persistence.room.TypeConverters
import org.dbtools.android.room.ext.mergeDatabase
import org.dbtools.sample.roomsqlite.model.db.main.converter.DateTimeTextConverter
import org.dbtools.sample.roomsqlite.model.db.main.converter.MainDatabaseConverters
import org.dbtools.sample.roomsqlite.model.db.main.individual.Individual
import org.dbtools.sample.roomsqlite.model.db.main.individual.IndividualDao
import java.io.File


@Database(entities = [Individual::class], version = 1)
@TypeConverters(MainDatabaseConverters::class, DateTimeTextConverter::class)
abstract class MainDatabase : RoomDatabase() {

    abstract fun individualDao(): IndividualDao

    fun mergeDataFromOtherDatabase(fromDatabaseFile: File, includeTables: List<String> = emptyList(), excludeTables: List<String> = emptyList()) {
        // make sure database is open
        if (!isOpen) {
            openHelper.writableDatabase
        }

        // merge database
        mDatabase.mergeDatabase(fromDatabaseFile, includeTables, excludeTables)
    }

    companion object {
        const val DATABASE_NAME: String = "Main"
    }
}