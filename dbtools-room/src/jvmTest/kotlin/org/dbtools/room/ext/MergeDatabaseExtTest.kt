package org.dbtools.room.ext

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.hasMessage
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

class MergeDatabaseExtTest {
    private lateinit var otherDatabaseFile: File
    private lateinit var connection: SQLiteConnection

    @BeforeTest
    fun setUp() {
        otherDatabaseFile = File.createTempFile("merge-source", ".db")
        BundledSQLiteDriver().open(otherDatabaseFile.absolutePath).apply {
            execSQL("CREATE TABLE item (id INTEGER PRIMARY KEY, title TEXT NOT NULL)")
            execSQL("INSERT INTO item (id, title) VALUES (2, 'Manual'), (3, 'Guide')")
            close()
        }

        connection = BundledSQLiteDriver().open(":memory:")
        connection.execSQL("CREATE TABLE item (id INTEGER PRIMARY KEY, title TEXT NOT NULL)")
        connection.execSQL("INSERT INTO item (id, title) VALUES (1, 'Hymns'), (2, 'Existing')")
    }

    @AfterTest
    fun tearDown() {
        connection.close()
        otherDatabaseFile.delete()
    }

    @Test
    fun `merge copies rows from the other database and reports success`() {
        val failures = mutableListOf<Exception>()

        val merged = connection.mergeDatabase(otherDatabaseFile.absolutePath, onFailBlock = { e, _ -> failures += e })

        assertThat(merged).isTrue()
        assertThat(failures).isEmpty()
        assertThat(titles()).containsExactly("Hymns", "Existing", "Guide")
        assertThat(attachedDatabaseNames()).containsExactly("main")
    }

    @Test
    fun `failed merge rolls back, reports the failure, and detaches`() {
        val failures = mutableListOf<Exception>()

        val merged = connection.mergeDatabase(
            otherDatabasePath = otherDatabaseFile.absolutePath,
            onFailBlock = { e, _ -> failures += e },
            mergeBlock = { sqLiteConnection, sourceTableName, targetTableName ->
                sqLiteConnection.execSQL("INSERT OR IGNORE INTO $targetTableName SELECT * FROM $sourceTableName")
                error("merge failed")
            }
        )

        assertThat(merged).isFalse()
        assertThat(failures.map { it.message }).containsExactly("merge failed")
        assertThat(titles()).containsExactly("Hymns", "Existing")
        assertThat(attachedDatabaseNames()).containsExactly("main")
    }

    @Test
    fun `merge still detaches when onFailBlock throws`() {
        assertFailure {
            connection.mergeDatabase(
                otherDatabasePath = otherDatabaseFile.absolutePath,
                onFailBlock = { _, _ -> error("onFailBlock failed") },
                mergeBlock = { _, _, _ -> error("merge failed") }
            )
        }.hasMessage("onFailBlock failed")

        assertThat(titles()).containsExactly("Hymns", "Existing")
        assertThat(attachedDatabaseNames()).containsExactly("main")
    }

    @Test
    fun `merge into a database missing the target table fails without merging`() {
        connection.execSQL("DROP TABLE item")

        val merged = connection.mergeDatabase(otherDatabaseFile.absolutePath)

        assertThat(merged).isFalse()
        assertThat(attachedDatabaseNames()).containsExactly("main")
    }

    @Test
    fun `runInTransaction rolls back when the block throws`() {
        val committed = connection.runInTransaction {
            connection.execSQL("INSERT INTO item (id, title) VALUES (9, 'Rolled back')")
            error("boom")
        }

        assertThat(committed).isFalse()
        assertThat(connection.rowCount("item")).isEqualTo(2L)
    }

    private fun titles(): List<String> = connection.prepare("SELECT title FROM item ORDER BY id").use { statement ->
        buildList { while (statement.step()) add(statement.getText(0)) }
    }

    private fun attachedDatabaseNames(): List<String> = connection.getAttachedDatabases().map { it.name }
}
