package org.dbtools.sample.roomsqlite.model.db.main

import android.app.Application
import androidx.room.Room
import org.dbtools.android.room.CloseableDatabaseWrapperRepository

class MainDatabaseWrapperRepository
constructor(
    application: Application
) : CloseableDatabaseWrapperRepository<MainDatabase>(application) {

    override fun createDatabase(filename: String): MainDatabase {
        return Room.databaseBuilder(application, MainDatabase::class.java, filename)
            .fallbackToDestructiveMigration()
            .build()
    }
}