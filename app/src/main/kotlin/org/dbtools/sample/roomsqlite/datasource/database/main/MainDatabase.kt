package org.dbtools.sample.roomsqlite.datasource.database.main

import android.arch.persistence.room.Database
import android.arch.persistence.room.RoomDatabase
import android.arch.persistence.room.TypeConverters
import org.dbtools.sample.roomsqlite.datasource.database.main.converter.MainDatabaseConverters
import org.dbtools.sample.roomsqlite.datasource.database.main.foo.Foo
import org.dbtools.sample.roomsqlite.datasource.database.main.foo.FooDao
import org.dbtools.sample.roomsqlite.datasource.database.main.individual.Individual
import org.dbtools.sample.roomsqlite.datasource.database.main.individual.IndividualDao


@Database(entities = [Individual::class, Foo::class], version = 1)
@TypeConverters(MainDatabaseConverters::class)
abstract class MainDatabase : RoomDatabase() {

    abstract fun individualDao(): IndividualDao
    abstract fun fooDao(): FooDao

    companion object {
        const val DATABASE_NAME: String = "Main"
    }
}