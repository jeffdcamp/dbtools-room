package org.dbtools.sample.roomsqlite.model.db.main

import android.app.Application
import android.arch.persistence.room.Room
import org.dbtools.android.room.CloseableDatabaseWrapperRepository

class MainDatabaseRepository
constructor(
    application: Application
) : CloseableDatabaseWrapperRepository<MainDatabase>(application) {

    fun individualDao(key: String) = getDatabase(key).individualDao()

    override fun createDatabase(filename: String): MainDatabase {
        return Room.databaseBuilder(application, MainDatabase::class.java, filename)
            .fallbackToDestructiveMigration()
            .build()
    }
}