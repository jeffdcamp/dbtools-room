/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.dbtools.android.room.testing

import android.app.Application
import android.util.Log
import androidx.arch.core.executor.ArchTaskExecutor
import androidx.room.DatabaseConfiguration
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.RoomOpenHelper
import androidx.room.migration.Migration
import androidx.room.migration.bundle.DatabaseBundle
import androidx.room.migration.bundle.EntityBundle
import androidx.room.migration.bundle.FieldBundle
import androidx.room.migration.bundle.ForeignKeyBundle
import androidx.room.migration.bundle.IndexBundle
import androidx.room.migration.bundle.SchemaBundle
import androidx.room.util.TableInfo
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import org.dbtools.android.room.jdbc.JdbcSQLiteOpenHelperFactory
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.lang.ref.WeakReference
import java.util.ArrayList
import java.util.HashMap
import java.util.HashSet

/**
 * MigrationUnitTestHelper is a replica of [android.arch.persistence.room.testing.MigrationTestHelper]
 * altered to be run in the JVM as a Unit Test.
 *
 * @param application May be passed as a Mock
 * @param testDatabasePath Directory path relative to the project root where the database will be generated
 * @param schemaLocation Directory path relative to the project root where the Room schema definitions are located
 */
class MigrationUnitTestHelper(
        private val application: Application,
        private val testDatabasePath: String,
        schemaLocation: String
) : TestWatcher() {

    private val schemaLocation: String
    private val jdbcOpenHelperFactory: JdbcSQLiteOpenHelperFactory
    private val managedDatabases = ArrayList<WeakReference<SupportSQLiteDatabase>>()

    init {
        this.schemaLocation = if (schemaLocation.endsWith("/")) {
            schemaLocation.substring(0, schemaLocation.length - 1)
        } else {
            schemaLocation
        }

        jdbcOpenHelperFactory = JdbcSQLiteOpenHelperFactory(testDatabasePath)
    }

    override fun finished(description: Description?) {
        super.finished(description)
        for (dbRef in managedDatabases) {
            val db = dbRef.get()
            if (db != null && db.isOpen) {
                try {
                    db.close()
                } catch(ignored: Throwable) { }
            }
        }
    }

    fun createDatabase(name: String, version: Int): SupportSQLiteDatabase {
        deleteDatabaseFileIfExists(name)

        val schemaBundle = loadSchema(version)
        val container = RoomDatabase.MigrationContainer()
        val configuration = DatabaseConfiguration(
                application,
                name,
                jdbcOpenHelperFactory,
                container,
                null,
                true,
                RoomDatabase.JournalMode.TRUNCATE,
                ArchTaskExecutor.getIOThreadExecutor(),
                true,
                true,
                true,
                emptySet()
        )
        val roomOpenHelper = RoomOpenHelper(
                configuration,
                CreatingDelegate(schemaBundle.database),
                schemaBundle.database.identityHash,
                schemaBundle.database.identityHash)

        return openDatabase(name, roomOpenHelper)
    }

    private fun deleteDatabaseFileIfExists(name: String) {
        val dbPath = File(testDatabasePath, name)
        if (dbPath.exists()) {
            Log.d(TAG, "deleting database file $name")
            if (!dbPath.delete()) {
                throw IllegalStateException("There is a database file and I could not delete " +
                        "it. Make sure you don't have any open connections to that " +
                        "database before calling this method.")
            }
        }
    }

    private fun verifyFileExists(name: String): Boolean {
        val dbPath = File(testDatabasePath, name)
        return dbPath.exists()
    }

    private fun loadSchema(version: Int): SchemaBundle {
        val schemaFile = File("$schemaLocation/$version.json")
        val schemaFilePath = schemaFile.absolutePath

        try {
            val inputStream = FileInputStream(schemaFilePath)
            return SchemaBundle.deserialize(inputStream) // deserialize will handle closing the stream
        } catch (e: FileNotFoundException) {
            throw FileNotFoundException("Cannot find the schema file in the following path: '$schemaFilePath'")
        }
    }

    private fun openDatabase(name: String, roomOpenHelper: RoomOpenHelper): SupportSQLiteDatabase {
        val config = SupportSQLiteOpenHelper.Configuration
                .builder(application)
                .callback(roomOpenHelper)
                .name(name)
                .build()
        val db = jdbcOpenHelperFactory.create(config).writableDatabase
        managedDatabases.add(WeakReference(db))
        return db
    }

    fun runMigrationsAndValidate(name: String, version: Int, validateDroppedTables: Boolean,
                                 vararg migrations: Migration): SupportSQLiteDatabase {
        if (!verifyFileExists(name)) {
            throw IllegalStateException("Cannot find the database file for $name. Before calling" +
                    " runMigrationsAndValidate, you must first create the database via createDatabase")
        }

        val schemaBundle = loadSchema(version)
        val container = RoomDatabase.MigrationContainer()
        container.addMigrations(*migrations)
        val configuration = DatabaseConfiguration(
                application,
                name,
                jdbcOpenHelperFactory,
                container,
                null,
                true,
                RoomDatabase.JournalMode.TRUNCATE,
                ArchTaskExecutor.getIOThreadExecutor(),
                true,
                true,
                true,
                emptySet<Int>()
        )
        val roomOpenHelper = RoomOpenHelper(
                configuration,
                MigratingDelegate(schemaBundle.database, validateDroppedTables),
                schemaBundle.database.identityHash,
                schemaBundle.database.identityHash)
        return openDatabase(name, roomOpenHelper)
    }

    private abstract class JdbcRoomOpenHelperDelegate(val databaseBundle: DatabaseBundle)
        : RoomOpenHelper.Delegate(databaseBundle.version) {
        override fun dropAllTables(database: SupportSQLiteDatabase) {
            throw UnsupportedOperationException("Cannot drop all tables in the test")
        }

        override fun onCreate(database: SupportSQLiteDatabase) {}

        override fun onOpen(database: SupportSQLiteDatabase) {}
    }

    private class CreatingDelegate(databaseBundle: DatabaseBundle)
        : JdbcRoomOpenHelperDelegate(databaseBundle) {

        override fun createAllTables(database: SupportSQLiteDatabase) {
            for (query in databaseBundle.buildCreateQueries()) {
                database.execSQL(query)

            }
        }

        override fun validateMigration(db: SupportSQLiteDatabase?) {
            throw UnsupportedOperationException("This open helper just creates the database" +
                    " but it received a migration request.")
        }
    }

    private class MigratingDelegate(
            databaseBundle: DatabaseBundle,
            private val verifyDroppedTables: Boolean
    ) : JdbcRoomOpenHelperDelegate(databaseBundle) {
        override fun createAllTables(database: SupportSQLiteDatabase) {
            throw UnsupportedOperationException("Was expecting to migrate but received create." + "Make sure you have created the database first.")
        }

        override fun validateMigration(db: SupportSQLiteDatabase) {
            val tables = databaseBundle.entitiesByTableName
            for (entity in tables.values) {
                val expected = toTableInfo(entity)
                val found = TableInfo.read(db, entity.tableName)
                if (expected != found) {
                    throw IllegalStateException(
                            "Migration failed. expected:$expected , found:$found")
                }
            }
            if (verifyDroppedTables) {
                // now ensure tables that should be removed are removed.
                val cursor = db.query("SELECT name FROM sqlite_master WHERE type='table'" + " AND name NOT IN(?, ?, ?)",
                        arrayOf(Room.MASTER_TABLE_NAME, "android_metadata", "sqlite_sequence"))

                try {
                    while (cursor.moveToNext()) {
                        val tableName = cursor.getString(0)
                        if (!tables.containsKey(tableName)) {
                            throw IllegalStateException("Migration failed. Unexpected table $tableName")
                        }
                    }
                } finally {
                    cursor.close()
                }
            }
        }

        private fun toTableInfo(entityBundle: EntityBundle): TableInfo {
            return TableInfo(entityBundle.tableName, toColumnMap(entityBundle),
                    toForeignKeys(entityBundle.foreignKeys), toIndices(entityBundle.indices))
        }

        private fun toIndices(indices: List<IndexBundle>?): Set<TableInfo.Index> {
            if (indices == null) {
                return emptySet()
            }
            val result = HashSet<TableInfo.Index>()
            for (bundle in indices) {
                result.add(TableInfo.Index(bundle.name, bundle.isUnique,
                        bundle.columnNames))
            }
            return result
        }

        private fun toForeignKeys(
                bundles: List<ForeignKeyBundle>?): Set<TableInfo.ForeignKey> {
            if (bundles == null) {
                return emptySet()
            }
            val result = HashSet<TableInfo.ForeignKey>(bundles.size)
            for (bundle in bundles) {
                result.add(TableInfo.ForeignKey(bundle.table,
                        bundle.onDelete, bundle.onUpdate,
                        bundle.columns, bundle.referencedColumns))
            }
            return result
        }

        private fun toColumnMap(entity: EntityBundle): Map<String, TableInfo.Column> {
            val result = HashMap<String, TableInfo.Column>()
            for (bundle in entity.fields) {
                val column = toColumn(entity, bundle)
                result[column.name] = column
            }
            return result
        }

        private fun toColumn(entity: EntityBundle, field: FieldBundle): TableInfo.Column {
            return TableInfo.Column(field.columnName, field.affinity,
                    field.isNonNull, findPrimaryKeyPosition(entity, field))
        }

        private fun findPrimaryKeyPosition(entity: EntityBundle, field: FieldBundle): Int {
            val columnNames = entity.primaryKey.columnNames
            var i = 0
            for (columnName in columnNames) {
                i++
                if (field.columnName.equals(columnName, ignoreCase = true)) {
                    return i
                }
            }
            return 0
        }
    }

    companion object {
        const val TAG = "MigrationUnitTestHelper"
    }
}