package org.dbtools.room.ext

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteException
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import org.dbtools.room.data.DatabaseColumnInfo
import org.dbtools.room.data.DatabaseTableType
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

/**
 * Exercises the schema extensions against a real (in-memory) SQLite database.
 *
 * Only the [SQLiteConnection] forms are tested here: the Transactor and RoomDatabase forms run the same SQL through
 * the same row readers (see SqlSchemaExt.kt), differing only in how they get a connection.
 */
class SqlSchemaExtTest {
    private lateinit var connection: SQLiteConnection

    @BeforeTest
    fun setUp() {
        connection = BundledSQLiteDriver().open(":memory:")
        connection.execSQL("CREATE TABLE item (id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT NOT NULL DEFAULT 'untitled', version INTEGER)")
        connection.execSQL("CREATE TABLE item_tag (item_id INTEGER NOT NULL, tag TEXT NOT NULL, PRIMARY KEY (item_id, tag))")
        connection.execSQL("CREATE VIEW item_title AS SELECT id, title FROM item")
        connection.execSQL("CREATE TABLE room_master_table (id INTEGER PRIMARY KEY, identity_hash TEXT)")
        connection.execSQL("INSERT INTO item (title, version) VALUES ('Hymns', 140), ('Manual', 3)")
    }

    @AfterTest
    fun tearDown() {
        connection.close()
    }

    @Test
    fun `findTablesInfo returns tables then views with their sql`() {
        val tables = connection.findTablesInfo()

        assertThat(tables.map { it.name }).containsExactly("item", "item_tag", "item_title")
        assertThat(tables.map { it.type }).containsExactly(DatabaseTableType.TABLE, DatabaseTableType.TABLE, DatabaseTableType.VIEW)
        assertThat(tables.first().sql).isNotNull()
    }

    /** sqlite_sequence (created by AUTOINCREMENT) and room_master_table are bookkeeping, not data. */
    @Test
    fun `findTablesInfo hides internal tables unless asked`() {
        assertThat(connection.findTablesInfo().none { it.name == "sqlite_sequence" || it.name == "room_master_table" }).isTrue()

        val allTableNames = connection.findTablesInfo(includeInternalTables = true).map { it.name }
        assertThat(allTableNames.contains("sqlite_sequence")).isTrue()
        assertThat(allTableNames.contains("room_master_table")).isTrue()
    }

    /** `_` is a LIKE wildcard, so an unescaped 'sqlite_%' would also hide a user table such as `sqliteX`. */
    @Test
    fun `findTablesInfo only hides the real sqlite_ prefix`() {
        connection.execSQL("CREATE TABLE sqliteish (id INTEGER)")

        assertThat(connection.findTablesInfo().any { it.name == "sqliteish" }).isTrue()
    }

    @Test
    fun `findTablesInfo in an attached database`() {
        connection.execSQL("ATTACH DATABASE ':memory:' AS other")
        connection.execSQL("CREATE TABLE other.note (id INTEGER)")

        assertThat(connection.findTablesInfo(databaseName = "other").map { it.name }).containsExactly("note")
    }

    /** Virtual tables store their data in shadow tables (docs_content, docs_segments, ...), which are not user data. */
    @Test
    fun `findTablesInfo hides virtual table shadow tables unless asked`() {
        connection.execSQL("CREATE VIRTUAL TABLE docs USING fts4(body)")
        connection.execSQL("CREATE VIRTUAL TABLE docs5 USING fts5(body)")
        connection.execSQL("CREATE VIRTUAL TABLE box USING rtree(id, minX, maxX)")
        connection.execSQL("CREATE TABLE notes_data (id INTEGER)")

        assertThat(connection.findTablesInfo().map { it.name })
            .containsExactly("box", "docs", "docs5", "item", "item_tag", "notes_data", "item_title")

        val allTableNames = connection.findTablesInfo(includeInternalTables = true).map { it.name }
        assertThat(allTableNames.containsAll(listOf("docs_content", "docs5_data", "box_node"))).isTrue()
    }

    @Test
    fun `findTablesInfo sorts names ignoring case`() {
        connection.execSQL("CREATE TABLE Zone (id INTEGER)")
        connection.execSQL("CREATE TABLE apple (id INTEGER)")

        assertThat(connection.findTablesInfo().filter { it.type == DatabaseTableType.TABLE }.map { it.name })
            .containsExactly("apple", "item", "item_tag", "Zone")
    }

    @Test
    fun `findColumnsInfo describes each column`() {
        val columns = connection.findColumnsInfo("item")

        assertThat(columns).containsExactly(
            DatabaseColumnInfo(name = "id", type = "INTEGER", notNull = false, defaultValue = null, primaryKeyPosition = 1),
            DatabaseColumnInfo(name = "title", type = "TEXT", notNull = true, defaultValue = "'untitled'", primaryKeyPosition = 0),
            DatabaseColumnInfo(name = "version", type = "INTEGER", notNull = false, defaultValue = null, primaryKeyPosition = 0),
        )
        assertThat(columns.first().isPrimaryKey).isTrue()
    }

    @Test
    fun `findColumnsInfo reports composite key positions`() {
        val columns = connection.findColumnsInfo("item_tag")

        assertThat(columns.map { it.primaryKeyPosition }).containsExactly(1, 2)
    }

    @Test
    fun `findColumnsInfo in an attached database`() {
        connection.execSQL("ATTACH DATABASE ':memory:' AS other")
        connection.execSQL("CREATE TABLE other.item (other_id TEXT)")

        assertThat(connection.findColumnsInfo("item", databaseName = "other").map { it.name }).containsExactly("other_id")
    }

    @Test
    fun `findColumnsInfo of a missing table is empty`() {
        assertThat(connection.findColumnsInfo("no_such_table")).isEmpty()
    }

    /** The table name is bound, not concatenated, so a name that needs quoting just works. */
    @Test
    fun `handles table names that need quoting`() {
        val oddName = """my "odd" table"""
        connection.execSQL("CREATE TABLE ${oddName.quoteSqlIdentifier()} (value TEXT)")
        connection.execSQL("INSERT INTO ${oddName.quoteSqlIdentifier()} (value) VALUES ('a'), ('b'), ('c')")

        assertThat(connection.findColumnsInfo(oddName).map { it.name }).containsExactly("value")
        assertThat(connection.columnExists(oddName, "value")).isTrue()
        assertThat(connection.rowCount(oddName)).isEqualTo(3L)
    }

    @Test
    fun `columnExists finds existing columns only`() {
        assertThat(connection.columnExists("item", "title")).isTrue()
        assertThat(connection.columnExists("item", "missing")).isFalse()
        assertThat(connection.columnExists("no_such_table", "title")).isFalse()
    }

    @Test
    fun `columnExists in an attached database`() {
        connection.execSQL("ATTACH DATABASE ':memory:' AS other")
        connection.execSQL("CREATE TABLE other.item (other_id TEXT)")

        assertThat(connection.columnExists("item", "other_id", databaseName = "other")).isTrue()
        assertThat(connection.columnExists("item", "title", databaseName = "other")).isFalse()
    }

    @Test
    fun `alterTableIfColumnDoesNotExist only adds a missing column once`() {
        repeat(2) {
            connection.alterTableIfColumnDoesNotExist("item", "note", "ALTER TABLE item ADD note TEXT")
        }

        assertThat(connection.columnExists("item", "note")).isTrue()
    }

    @Test
    fun `rowCount counts rows in tables and views`() {
        assertThat(connection.rowCount("item")).isEqualTo(2L)
        assertThat(connection.rowCount("item_tag")).isEqualTo(0L)
        assertThat(connection.rowCount("item_title")).isEqualTo(2L)
    }

    @Test
    fun `rowCount of a missing table throws`() {
        assertFailure { connection.rowCount("no_such_table") }.isInstanceOf<SQLiteException>()
    }

    /** The older name-based functions quote the attached database alias too. */
    @Test
    fun `table and view lookups in an attached database whose name needs quoting`() {
        connection.execSQL("""ATTACH DATABASE ':memory:' AS "my db"""")
        connection.execSQL("""CREATE TABLE "my db".note (id INTEGER)""")
        connection.execSQL("""CREATE VIEW "my db".note_ids AS SELECT id FROM note""")

        assertThat(connection.findTableNames("my db")).containsExactly("note")
        assertThat(connection.tableExists("note", "my db")).isTrue()
        assertThat(connection.findViewNames("my db")).containsExactly("note_ids")
        assertThat(connection.viewExists("note_ids", "my db")).isTrue()
    }

    @Test
    fun `tableExists handles a name containing an apostrophe`() {
        connection.execSQL("""CREATE TABLE "o'brien" (id INTEGER)""")

        assertThat(connection.tableExists("o'brien")).isTrue()
        assertThat(connection.tablesExists(listOf("item", "o'brien"))).isTrue()
    }

    @Test
    fun `quoteSqlIdentifier wraps and escapes double quotes`() {
        assertThat("item".quoteSqlIdentifier()).isEqualTo("\"item\"")
        assertThat("""a "b" c""".quoteSqlIdentifier()).isEqualTo("\"a \"\"b\"\" c\"")
    }

    /** Values come back as the storage class SQLite holds, not as the column's declared type. */
    @Test
    fun `getValue reads each storage class`() {
        connection.prepare("SELECT 42, 1.5, 'text', x'0102', NULL, CAST('7' AS TEXT)").use { statement ->
            assertThat(statement.step()).isTrue()

            assertThat(statement.getValue(0)).isEqualTo(42L)
            assertThat(statement.getValue(1)).isEqualTo(1.5)
            assertThat(statement.getValue(2)).isEqualTo("text")
            assertThat((statement.getValue(3) as ByteArray).toList()).containsExactly(1.toByte(), 2.toByte())
            assertThat(statement.getValue(4)).isNull()
            assertThat(statement.getValue(5)).isEqualTo("7")
        }
    }

    @Test
    fun `getValues reads the whole row`() {
        connection.prepare("SELECT id, title, version FROM item ORDER BY id").use { statement ->
            assertThat(statement.step()).isTrue()

            assertThat(statement.getValues()).containsExactly(1L, "Hymns", 140L)
        }
    }

    /** getValues and bindArgs round-trip: what one reads, the other can bind. */
    @Test
    fun `getValues round trips through bindArgs`() {
        val row = connection.prepare("SELECT title, version FROM item WHERE id = 1").use { statement ->
            statement.step()
            statement.getValues()
        }

        val matches = connection.prepare("SELECT count(*) FROM item WHERE title = ? AND version = ?").use { statement ->
            statement.bindArgs(row)
            statement.step()
            statement.getLong(0)
        }

        assertThat(matches).isEqualTo(1L)
    }
}
