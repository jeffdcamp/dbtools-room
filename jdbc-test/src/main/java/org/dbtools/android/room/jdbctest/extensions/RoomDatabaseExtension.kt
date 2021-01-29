package org.dbtools.android.room.jdbctest.extensions

import android.app.Application
import androidx.room.Room
import androidx.room.RoomDatabase
import org.dbtools.android.room.jdbc.JdbcSQLiteOpenHelperFactory
import org.dbtools.android.room.jdbctest.util.TestFileSystem
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import java.io.File

/**
 * Extension to automatically create and manage a new Room database for each test-case.
 * A database file will be generated for each test-case; the file will be named after
 * the test-case method. At the end of the test, the database will be closed.
 *
 * ## Usage
 * ```
 * @JvmField
 * @RegisterExtension
 * val roomDatabaseExtension = RoomDatabaseExtension(mock(Application::class), MyDatabase::class.java)
 *
 * @Test
 * fun testCase() {
 *    val db = roomDatabaseExtension.testDatabase
 * }
 * ```
 *
 * ## Viewing test database files
 *
 * By default, all test database files are saved relative the the project's module folder (app):
 * `../ProjectName/app/build/test-db/`. To change this relative path, supply a `databaseDir`
 * when constructing this extension.
 *
 * @param mockApplication Mocked application instance, required for this extension to use the Room.databaseBuilder
 * @param databaseClass The class for the database that should be instantiated by this extension for testing
 */
class RoomDatabaseExtension<T : RoomDatabase>(
    private val mockApplication: Application,
    private val databaseClass: Class<T>
) : BeforeEachCallback, AfterEachCallback {

    lateinit var testDatabase: T

    override fun beforeEach(context: ExtensionContext?) {
        context ?: error("ExtensionContext was null...")
        val dbFileName = formatTestMethodDisplayName(context.displayName)
        File(TestFileSystem.getDatabasePath(dbFileName)).delete()
        testDatabase = createTestDatabase(dbFileName)
    }

    override fun afterEach(context: ExtensionContext?) {
        testDatabase.close()
    }

    private fun formatTestMethodDisplayName(rawDisplayName: String): String {
        if (rawDisplayName.isBlank()) error("The test case's display name was blank")
        val dbFileName = rawDisplayName.replace(' ', '_').replace("()", "")
        return "$dbFileName.db"
    }

    private fun createTestDatabase(filename: String): T {
        return Room.databaseBuilder(mockApplication, databaseClass, filename)
            .allowMainThreadQueries()
            .openHelperFactory(JdbcSQLiteOpenHelperFactory(TestFileSystem.INTERNAL_DATABASES_DIR_PATH))
            .fallbackToDestructiveMigration()
            .build()
    }
}