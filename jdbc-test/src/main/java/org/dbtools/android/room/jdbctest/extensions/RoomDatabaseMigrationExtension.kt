@file:SuppressLint("RestrictedApi")
@file:Suppress("unused")

package org.dbtools.android.room.jdbctest.extensions

import android.annotation.SuppressLint
import android.content.Context
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
import androidx.room.migration.bundle.FtsEntityBundle
import androidx.room.migration.bundle.IndexBundle
import androidx.room.migration.bundle.SchemaBundle
import androidx.room.util.FtsTableInfo
import androidx.room.util.TableInfo
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import io.mockk.mockk
import org.dbtools.android.room.jdbc.JdbcSQLiteOpenHelperFactory
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.lang.ref.WeakReference

/**
 * An extension that can be used in your local unit tests which can create the database in an
 * older schema. This is the JUnit5 equivalent of Google's [MigrationTestHelper][androidx.room.testing.MigrationTestHelper] from JUnit4.
 *
 * You must copy the schema json files (created by passing `room.schemaLocation` argument into the annotation processor)
 * into your test assets and pass in the path for that folder into the constructor. This class will read the folder and extract
 * the schemas from there.
 *
 * ```
 * android {
 *   defaultConfig {
 *     javaCompileOptions {
 *       annotationProcessorOptions {
 *         arguments = ["room.schemaLocation": "$projectDir/schemas".toString()]
 *       }
 *     }
 *   }
 *   sourceSets {
 *     androidTest.assets.srcDirs += files("$projectDir/schemas".toString())
 *   }
 * }
 * ```
 *
 * Create a `buildConfigField` in your app gradle like so: `buildConfigField "String", "SCHEMA_PATH", "\"schemas\""`
 *
 * @param schemaPath Path to your database schemas. Can be obtained by passing `BuildConfig.SCHEMA_PATH` if you set up your buildConfigField (see above notes)
 * @param assetsFolder Usually `YourDatabaseClass::class.java.canonicalName`
 * @param databaseDir The directory for this extension to create the database files
 */
class RoomDatabaseMigrationExtension(
    private val schemaPath: String,
    private val assetsFolder: String,
    private val databaseDir: String = DEFAULT_DATABASE_DIR
) : BeforeEachCallback, AfterEachCallback {

    private lateinit var openFactory: SupportSQLiteOpenHelper.Factory
    private val managedDatabases = ArrayList<WeakReference<SupportSQLiteDatabase>>()
    private val managedRoomDatabases = ArrayList<WeakReference<RoomDatabase>>()
    private var testStarted = false

    private val mockContext = mockk<Context>()

    override fun beforeEach(context: ExtensionContext?) {
        testStarted = true
    }

    override fun afterEach(context: ExtensionContext?) {
        for (dbRef in managedDatabases) {
            val db = dbRef.get()
            if (db?.isOpen == true) {
                try {
                    db.close()
                } catch (ignored: Throwable) {
                }
            }
        }
        for (dbRef in managedRoomDatabases) {
            dbRef.get()?.close()
        }

        testStarted = false
    }

    /**
     * Registers a database connection to be automatically closed when the test finishes.
     *
     * This only works if `RoomDatabaseMigrationExtension` is registered as a Junit5 test extension via `@RegisterExtension.`
     *
     * @param db The database connection that should be closed after the test finishes.
     */
    fun closeWhenFinished(db: SupportSQLiteDatabase) {
        if (!testStarted) {
            throw IllegalStateException(
                """You cannot register a database to be closed before the test starts.
                    | Maybe you forgot to annotate MigrationTestExtension as a test rule? (@Rule)""".trimMargin()
            )
        }
        managedDatabases.add(WeakReference(db))
    }

    /**
     * Registers a database connection to be automatically closed when the test finishes.
     *
     * This only works if `RoomDatabaseMigrationExtension` is registered as a Junit5 test extension via `@RegisterExtension`
     *
     * @param db The RoomDatabase instance which holds the database.
     */
    fun closeWhenFinished(db: RoomDatabase) {
        if (!testStarted) {
            throw IllegalStateException(
                """You cannot register a database to be closed before the test starts.
                    | Maybe you forgot to annotate MigrationTestExtension as a test rule? (@Rule)""".trimMargin()
            )
        }
        managedRoomDatabases.add(WeakReference(db))
    }

    /**
     * Creates the database in the given version.
     * If the database file already exists, it tries to delete it first. If delete fails, throws
     * an exception.
     *
     * @param name    The name of the database.
     * @param version The version in which the database should be created.
     * @return A database connection which has the schema in the requested version.
     * @throws IOException If it cannot find the schema description in the assets folder.
     */
    fun createDatabase(name: String, version: Int): SupportSQLiteDatabase {
        val dbPath = File(databaseDir, name)
        if (dbPath.exists()) {
            Timber.d("deleting database file $dbPath")
            if (!dbPath.delete()) {
                throw IllegalStateException(
                    """There is a database file called $dbPath and I could not delete it.
                        | Make sure you don't have any open connections to
                        | that database before calling this method.""".trimMargin()
                )
            }
        }

        openFactory = JdbcSQLiteOpenHelperFactory(dbPath.parent)
        val schemaBundle = loadSchema(version)
        val container = RoomDatabase.MigrationContainer()
        val configuration = DatabaseConfiguration(
            mockContext,
            name,
            openFactory,
            container,
            null,
            true,
            RoomDatabase.JournalMode.TRUNCATE,
            ArchTaskExecutor.getMainThreadExecutor(),
            ArchTaskExecutor.getMainThreadExecutor(),
            true,
            true,
            true,
            emptySet(),
            null,
            null,
            null,
            null,
            null
        )
        val roomOpenHelper = RoomOpenHelper(
            configuration,
            CreatingDelegate(schemaBundle.database),
            schemaBundle.database.identityHash,
            // we pass the same hash twice since an old schema does not necessarily have
            // a legacy hash and we would not even persist it.
            schemaBundle.database.identityHash
        )
        return openDatabase(name, roomOpenHelper)
    }

    /**
     * Runs the given set of migrations on the provided database.
     *
     * It uses the same algorithm that Room uses to choose migrations so the migrations instances
     * that are provided to this method must be sufficient to bring the database from current
     * version to the desired version.
     *
     * After the migration, the method validates the database schema to ensure that migration
     * result matches the expected schema. Handling of dropped tables depends on the
     * `validateDroppedTables` argument. If set to true, the verification will fail if it
     * finds a table that is not registered in the Database. If set to false, extra tables in the
     * database will be ignored (this is the runtime library behavior).
     *
     * @param name                  The database name. You must first create this database via
     * [.createDatabase].
     * @param version               The final version after applying the migrations.
     * @param validateDroppedTables If set to true, validation will fail if the database has
     * unknown
     * tables.
     * @param migrations            The list of available migrations.
     * @throws IOException           If it cannot find the schema for `toVersion`.
     * @throws IllegalStateException If the schema validation fails.
     */
    fun runMigrationsAndValidate(name: String, version: Int, validateDroppedTables: Boolean, vararg migrations: Migration): SupportSQLiteDatabase {
        val dbPath = File(databaseDir, name)
        if (!dbPath.exists()) {
            throw IllegalStateException(
                """Cannot find the database file for $name. Before calling runMigrations,
                    | you must first create the database via createDatabase.""".trimMargin()
            )
        }
        val schemaBundle = loadSchema(version)
        val container = RoomDatabase.MigrationContainer()
        container.addMigrations(*migrations)
        val configuration = DatabaseConfiguration(
            mockContext,
            name,
            openFactory,
            container,
            null,
            true,
            RoomDatabase.JournalMode.TRUNCATE,
            ArchTaskExecutor.getMainThreadExecutor(),
            ArchTaskExecutor.getMainThreadExecutor(),
            true,
            true,
            true,
            emptySet(),
            null,
            null,
            null,
            null,
            null
        )
        val roomOpenHelper = RoomOpenHelper(
            configuration,
            MigratingDelegate(schemaBundle.database, validateDroppedTables),
            // we pass the same hash twice since an old schema does not necessarily have
            // a legacy hash and we would not even persist it.
            schemaBundle.database.identityHash,
            schemaBundle.database.identityHash
        )
        return openDatabase(name, roomOpenHelper)
    }

    private fun loadSchema(version: Int) = loadSchema(File(schemaPath, assetsFolder), version)

    private fun loadSchema(dir: File, version: Int): SchemaBundle {
        val input = FileInputStream(File(dir, "$version.json"))
        return SchemaBundle.deserialize(input)
    }

    private fun openDatabase(name: String, roomOpenHelper: RoomOpenHelper): SupportSQLiteDatabase {
        val config = SupportSQLiteOpenHelper.Configuration
            .builder(mockContext)
            .callback(roomOpenHelper)
            .name(name)
            .build()
        val db = openFactory.create(config).writableDatabase
        managedDatabases.add(WeakReference(db))
        return db
    }

    companion object {
        private const val DEFAULT_DATABASE_DIR = "build/test-db"

        private fun toTableInfo(entityBundle: EntityBundle): TableInfo {
            return TableInfo(
                entityBundle.tableName, toColumnMap(entityBundle),
                toForeignKeys(entityBundle.foreignKeys), toIndices(entityBundle.indices)
            )
        }

        private fun toFtsTableInfo(ftsEntityBundle: FtsEntityBundle): FtsTableInfo {
            return FtsTableInfo(
                ftsEntityBundle.tableName, toColumnNamesSet(ftsEntityBundle),
                ftsEntityBundle.createSql
            )
        }

        private fun toIndices(indices: List<IndexBundle>?): Set<TableInfo.Index> {
            if (indices == null) {
                return emptySet()
            }
            val result = HashSet<TableInfo.Index>()
            for (bundle in indices) {
                result.add(
                    TableInfo.Index(
                        bundle.name, bundle.isUnique,
                        bundle.columnNames
                    )
                )
            }
            return result
        }

        private fun toForeignKeys(
            bundles: List<ForeignKeyBundle>?
        ): Set<TableInfo.ForeignKey> {
            if (bundles == null) {
                return emptySet()
            }
            val result = HashSet<TableInfo.ForeignKey>(bundles.size)
            for (bundle in bundles) {
                result.add(
                    TableInfo.ForeignKey(
                        bundle.table,
                        bundle.onDelete, bundle.onUpdate,
                        bundle.columns, bundle.referencedColumns
                    )
                )
            }
            return result
        }

        private fun toColumnNamesSet(entity: EntityBundle): Set<String> {
            val result: MutableSet<String> = mutableSetOf()
            for (field in entity.fields) {
                result.add(field.columnName)
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
            return TableInfo.Column(
                field.columnName, field.affinity,
                field.isNonNull,
                findPrimaryKeyPosition(entity, field),
                null,
                TableInfo.CREATED_FROM_UNKNOWN
            )
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

    internal class MigratingDelegate(databaseBundle: DatabaseBundle, private val verifyDroppedTables: Boolean) : RoomOpenHelperDelegate(databaseBundle) {

        override fun createAllTables(database: SupportSQLiteDatabase) {
            throw UnsupportedOperationException("Was expecting to migrate but received create. Make sure you have created the database first.")
        }

        override fun validateMigration(db: SupportSQLiteDatabase) {
            val tables = mDatabaseBundle.entitiesByTableName
            for (actualTableEntity in tables.values) {
                if (actualTableEntity is FtsEntityBundle) {
                    val expected = toFtsTableInfo(actualTableEntity)
                    val found = FtsTableInfo.read(db, actualTableEntity.getTableName())
                    if (expected != found) {
                        throw IllegalStateException(
                            "Migration failed. expected:\n$expected\n found:\n$found"
                        )
                    }
                } else {
                    val expected = toTableInfo(actualTableEntity)
                    val found = TableInfo.read(db, actualTableEntity.tableName)
                    if (expected != found) {
                        throw IllegalStateException(
                            "Migration failed. expected:\n$expected\n found:\n$found"
                        )
                    }
                }
                if (verifyDroppedTables) {
                    // now ensure tables that should be removed are removed.
                    val expectedTables = hashSetOf<String>()
                    for (expectedTablesEntity in tables.values) {
                        expectedTables.add(expectedTablesEntity.tableName)
                        if (expectedTablesEntity is FtsEntityBundle) {
                            expectedTables.addAll(expectedTablesEntity.shadowTableNames)
                        }
                    }
                    val cursor = db.query(
                        "SELECT name FROM sqlite_master WHERE type='table' AND name NOT IN (?, ?, ?)",
                        arrayOf(Room.MASTER_TABLE_NAME, "android_metadata", "sqlite_sequence")
                    )

                    cursor.use {
                        while (it.moveToNext()) {
                            val tableName = cursor.getString(0)
                            if (!expectedTables.contains(tableName)) {
                                throw IllegalStateException("Migration failed. Unexpected table $tableName")
                            }
                        }
                    }
                }
            }
        }
    }

    internal class CreatingDelegate(databaseBundle: DatabaseBundle) : RoomOpenHelperDelegate(databaseBundle) {

        override fun createAllTables(database: SupportSQLiteDatabase) {
            for (query in mDatabaseBundle.buildCreateQueries()) {
                database.execSQL(query)
            }
        }

        override fun validateMigration(db: SupportSQLiteDatabase) {
            throw UnsupportedOperationException("This open helper just creates the database but it received a migration request.")
        }
    }

    internal abstract class RoomOpenHelperDelegate(val mDatabaseBundle: DatabaseBundle) : RoomOpenHelper.Delegate(mDatabaseBundle.version) {

        override fun dropAllTables(database: SupportSQLiteDatabase) {
            throw UnsupportedOperationException("cannot drop all tables in the test")
        }

        override fun onCreate(database: SupportSQLiteDatabase) {}

        override fun onOpen(database: SupportSQLiteDatabase) {}
    }
}