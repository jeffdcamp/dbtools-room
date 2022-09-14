package org.dbtools.sample.roomsqlite.model.db.main

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import org.dbtools.android.room.DatabaseViewQuery
import org.dbtools.android.room.ext.createView
import org.dbtools.android.room.ext.dropAllViews
import org.dbtools.android.room.ext.dropView
import org.dbtools.android.room.ext.findViewNames
import org.dbtools.android.room.ext.mergeDatabase
import org.dbtools.android.room.ext.recreateAllViews
import org.dbtools.android.room.ext.recreateView
import org.dbtools.android.room.ext.viewExists
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

    fun mergeDataFromOtherDatabase(
            fromDatabaseFile: File,
            includeTables: List<String> = emptyList(),
            excludeTables: List<String> = emptyList(),
            tableNameMap: Map<String, String> = emptyMap()
    ) {
        // merge database
        val database = openHelper.writableDatabase
        database.mergeDatabase(fromDatabaseFile, includeTables, excludeTables, tableNameMap)
    }

    fun viewTests() {
        val database = openHelper.writableDatabase
        val viewName1 = "TestView1"
        val viewName2 = "TestView2"

        // make sure there are no views from this test
        database.dropAllViews(listOf(viewName1, viewName2))
        val originalViewCount = database.findViewNames().size

        // create 1
        createTestView(database, viewName1)
        check(database.viewExists(viewName1))

        // drop 1
        database.dropView(viewName1)
        check(!database.viewExists(viewName1))

        // create 2
        createTestView(database, viewName1)
        createTestView(database, viewName2)
        checkViewCount(database, originalViewCount + 2)

        // recreate 1
        database.recreateView(viewName1, "SELECT * FROM individual")
        checkViewCount(database, originalViewCount + 2)

        // recreate 2
        val databaseViewQueryList = listOf(
                DatabaseViewQuery(viewName1, "SELECT * FROM individual"),
                DatabaseViewQuery(viewName2, "SELECT * FROM individual")
        )
        database.recreateAllViews(databaseViewQueryList)
        checkViewCount(database, originalViewCount + 2)

        // cleanup
        database.dropAllViews(databaseViewQueryList.map { it.viewName })
        checkViewCount(database, originalViewCount)

        // delete all (leave this commented out because it will leave the database in a broken state (missing expected view))
//        database.dropAllViews()
//        checkViewCount(database, 0)
    }

    private fun createTestView(database: SupportSQLiteDatabase, viewName: String) {
        database.createView(viewName, "SELECT * FROM individual")
    }

    private fun getCurrentViewCount(database: SupportSQLiteDatabase) = database.findViewNames().size

    private fun checkViewCount(database: SupportSQLiteDatabase, expectedCount: Int) {
        check(getCurrentViewCount(database) == expectedCount) { "Count = ${database.findViewNames().size} (${database.findViewNames()})" }
    }

    companion object {
        const val DATABASE_NAME: String = "main.db"

        val DATABASE_VIEW_QUERIES = listOf(
                DatabaseViewQuery(IndividualView.VIEW_NAME, IndividualView.VIEW_QUERY)
        )
    }
}