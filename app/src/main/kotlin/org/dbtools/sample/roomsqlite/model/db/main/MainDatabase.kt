package org.dbtools.sample.roomsqlite.model.db.main

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import org.dbtools.android.room.DatabaseViewQuery
import org.dbtools.android.room.ext.mergeDatabase
import org.dbtools.sample.roomsqlite.model.db.main.converter.DateTimeTextConverter
import org.dbtools.sample.roomsqlite.model.db.main.converter.MainDatabaseConverters
import org.dbtools.sample.roomsqlite.model.db.main.individual.Individual
import org.dbtools.sample.roomsqlite.model.db.main.individual.IndividualDao
import org.dbtools.sample.roomsqlite.model.db.main.individualview.IndividualView
import java.io.File


@Database(
    entities = [
        Individual::class
    ],
    views = [
        IndividualView::class
    ],
    version = 1
)
@TypeConverters(MainDatabaseConverters::class, DateTimeTextConverter::class)
abstract class MainDatabase : RoomDatabase() {

    abstract val individualDao: IndividualDao

    fun mergeDataFromOtherDatabase(fromDatabaseFile: File, includeTables: List<String> = emptyList(), excludeTables: List<String> = emptyList()) {
        // merge database
        val database = openHelper.writableDatabase
        database.mergeDatabase(fromDatabaseFile, includeTables, excludeTables)
    }

    companion object {
        const val DATABASE_NAME: String = "main.db"

        val DATABASE_VIEW_QUERIES = listOf(
            DatabaseViewQuery(IndividualView.VIEW_NAME, IndividualView.VIEW_QUERY)
        )
    }
}