package org.dbtools.room.ext

import androidx.room3.TransactionScope
import androidx.room3.Transactor
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteStatement
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

/**
 * Runs the [Transactor] forms of the schema extensions, which are maintained separately from the [SQLiteConnection]
 * forms in [SqlSchemaExtTest], against a real (in-memory) SQLite database.
 *
 * The RoomDatabase forms only hand their Transactor to these functions, so they are covered here too.
 */
class TransactorSchemaExtTest {
    private lateinit var connection: SQLiteConnection
    private lateinit var transactor: Transactor

    @BeforeTest
    fun setUp() {
        connection = BundledSQLiteDriver().open(":memory:")
        connection.execSQL("CREATE TABLE item (id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT NOT NULL DEFAULT 'untitled')")
        connection.execSQL("CREATE VIEW item_title AS SELECT id, title FROM item")
        connection.execSQL("CREATE VIRTUAL TABLE docs USING fts4(body)")
        connection.execSQL("INSERT INTO item (title) VALUES ('Hymns'), ('Manual')")
        connection.execSQL("""ATTACH DATABASE ':memory:' AS "my db"""")
        connection.execSQL("""CREATE TABLE "my db".note (note_id INTEGER)""")
        transactor = ConnectionTransactor(connection)
    }

    @AfterTest
    fun tearDown() {
        connection.close()
    }

    @Test
    fun `findTablesInfo hides internal and shadow tables`() = runTest {
        assertThat(transactor.findTablesInfo().map { it.name }).containsExactly("docs", "item", "item_title")
    }

    @Test
    fun `findColumnsInfo in an attached database`() = runTest {
        assertThat(transactor.findColumnsInfo("note", databaseName = "my db").map { it.name }).containsExactly("note_id")
    }

    @Test
    fun `rowCount counts rows`() = runTest {
        assertThat(transactor.rowCount("item")).isEqualTo(2L)
        assertThat(transactor.rowCount("note", databaseName = "my db")).isEqualTo(0L)
    }

    @Test
    fun `columnExists finds existing columns only`() = runTest {
        assertThat(transactor.columnExists("item", "title")).isTrue()
        assertThat(transactor.columnExists("item", "missing")).isFalse()
        assertThat(transactor.columnExists("note", "note_id", databaseName = "my db")).isTrue()
    }

    /** A [Transactor] over a single connection: just enough for the extensions, which only prepare statements. */
    private class ConnectionTransactor(private val connection: SQLiteConnection) : Transactor {
        override suspend fun <R> usePrepared(sql: String, block: suspend (SQLiteStatement) -> R): R =
            connection.prepare(sql).use { block(it) }

        override suspend fun <R> withTransaction(
            type: Transactor.SQLiteTransactionType,
            block: suspend TransactionScope<R>.() -> R,
        ): R = error("Transactions are not used by these tests")

        override suspend fun inTransaction(): Boolean = false
    }
}
