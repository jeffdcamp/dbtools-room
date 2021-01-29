package org.dbtools.android.room.jdbctest.database

import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import org.dbtools.android.room.jdbctest.extensions.InstantTaskExecutorExtension
import org.dbtools.android.room.jdbctest.extensions.RoomDatabaseMigrationExtension
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.RegisterExtension

/**
 * Base class to run database migration tests. Includes the [org.dbtools.android.room.jdbctest.extensions.RoomDatabaseMigrationExtension][RoomDatabaseMigrationExtension]
 * as a property for your implementing test class.
 *
 * Usage: Simply call [testMigration] from within your test-case
 *
 * @param schemaPath See [org.dbtools.android.room.jdbctest.extensions.RoomDatabaseMigrationExtension]. `BuildConfig.SCHEMA_PATH`
 * @param dbClass Class for the database being tested
 * @param migrations Migrations to be tested
 */
@ExtendWith(InstantTaskExecutorExtension::class)
abstract class BaseMigrationTest<T : RoomDatabase>(schemaPath: String, dbClass: Class<T>, private vararg val migrations: Migration) {

    @JvmField
    @RegisterExtension
    val migrationTestExtension = RoomDatabaseMigrationExtension(schemaPath, dbClass.canonicalName ?: "NULL")

    /**
     * @param fromVersion Starting version for the database, before migrating
     * @param toVersion Target version for the database, after migrating
     * @param block Perform any setup logic (ex: prepopulate with data) to be run before starting the migration
     */
    protected fun testMigration(fromVersion: Int, toVersion: Int, block: (SupportSQLiteDatabase) -> Unit = {}): SupportSQLiteDatabase {
        val name = "test_$fromVersion-$toVersion.db"
        migrationTestExtension.createDatabase(name, fromVersion).use {
            block(it)
        }
        return migrationTestExtension.runMigrationsAndValidate(name, toVersion, true, *migrations)
    }
}