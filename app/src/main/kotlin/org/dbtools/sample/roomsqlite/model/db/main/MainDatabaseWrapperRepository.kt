package org.dbtools.sample.roomsqlite.model.db.main

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import org.dbtools.android.room.CloseableDatabaseWrapperRepository
import org.dbtools.android.room.log.LoggingCallback
import org.dbtools.android.room.log.setLoggingQueryCallback
import org.dbtools.android.room.sqliteorg.SqliteOrgSQLiteOpenHelperFactory
import org.dbtools.android.room.util.DatabaseUtil

class MainDatabaseWrapperRepository
constructor(
    context: Context
) : CloseableDatabaseWrapperRepository<MainDatabase>(context) {

    override fun createDatabase(filename: String): MainDatabase {
        return Room.databaseBuilder(context, MainDatabase::class.java, filename)
            .openHelperFactory(SqliteOrgSQLiteOpenHelperFactory())
            .addCallback(LoggingCallback())
            .addMigrations(object : Migration(1, 2) {
                override fun migrate(database: SupportSQLiteDatabase) {
                    // ONLY views are changed

                    // drop and recreate views
                    DatabaseUtil.recreateAllViews(database, MainDatabase.DATABASE_VIEW_QUERIES)
                }
            })
            .addMigrations(object : Migration(2, 3) {
                override fun migrate(database: SupportSQLiteDatabase) {
                    // BOTH views and tables are changed

                    // drop views
                    DatabaseUtil.dropAllViews(database)

                    // do other database migrations here

                    // recreate views
                    DatabaseUtil.createAllViews(database, MainDatabase.DATABASE_VIEW_QUERIES)
                }
            })
            .fallbackToDestructiveMigration()

            // Add Logging (via function extension)
            .setLoggingQueryCallback(MainDatabase.DATABASE_NAME)

            // Add Logging (via Class)
//            .setQueryCallback(LoggingQueryCallback(MainDatabase.DATABASE_NAME), LoggingExecutor)
            .build()
    }
}