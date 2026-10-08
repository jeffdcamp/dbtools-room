package org.dbtools.room.ext

import androidx.room3.Room
import androidx.sqlite.SQLiteStatement
import org.dbtools.room.data.DatabaseColumnInfo
import org.dbtools.room.data.DatabaseTableInfo
import org.dbtools.room.data.DatabaseTableType

/**
 * Quote this string as a SQLite identifier (a table, view, column or schema name), doubling any embedded `"`.
 *
 * SQLite can bind values but not identifiers, so a name that has to be built into a statement must be quoted
 * rather than concatenated as-is: a name containing a space, a quote, or a keyword would otherwise break the
 * statement, or change what it does.
 *
 * Example: `my "odd" table` becomes `"my ""odd"" table"`
 */
fun String.quoteSqlIdentifier(): String = "\"${replace("\"", "\"\"")}\""

/**
 * Tables SQLite and Room create for their own bookkeeping. They describe the database rather than its data, so
 * [findTablesInfo] leaves them out unless asked.
 */
private val INTERNAL_TABLE_NAMES = listOf(Room.MASTER_TABLE_NAME, "android_metadata")

/**
 * Shadow tables SQLite creates to store a virtual table's data, by module: a virtual table `docs` using fts4 is
 * stored in `docs_content`, `docs_segments`, etc. Like [INTERNAL_TABLE_NAMES], [findTablesInfo] leaves them out
 * unless asked.
 */
private val SHADOW_TABLE_SUFFIXES = mapOf(
    "fts3" to setOf("content", "segments", "segdir", "docsize", "stat"),
    "fts4" to setOf("content", "segments", "segdir", "docsize", "stat"),
    "fts5" to setOf("data", "idx", "content", "docsize", "config"),
    "rtree" to setOf("node", "parent", "rowid"),
    "rtree_i32" to setOf("node", "parent", "rowid"),
)

private val VIRTUAL_TABLE_MODULE_REGEX = Regex("""^\s*CREATE\s+VIRTUAL\s+TABLE\b[\s\S]*?\bUSING\s+(\w+)""", RegexOption.IGNORE_CASE)

/** `sqlite_master` for [databaseName] (an attached database alias), or for the main database when blank. */
internal fun sqliteMasterTable(databaseName: String): String =
    if (databaseName.isBlank()) "sqlite_master" else "${databaseName.quoteSqlIdentifier()}.sqlite_master"

/** `"table"`, or `"schema"."table"` when [databaseName] is not blank. */
internal fun qualifiedTableName(tableName: String, databaseName: String): String =
    if (databaseName.isBlank()) tableName.quoteSqlIdentifier() else "${databaseName.quoteSqlIdentifier()}.${tableName.quoteSqlIdentifier()}"

/** `('a','b')`: [values] as a SQL IN list of string literals, with any embedded `'` doubled. */
internal fun sqlInClause(values: List<String>): String =
    values.joinToString(",", prefix = "(", postfix = ")") { "'${it.replace("'", "''")}'" }

internal fun findTablesInfoSql(databaseName: String, includeInternalTables: Boolean): String {
    val internalFilter = if (includeInternalTables) {
        ""
    } else {
        val internalNames = INTERNAL_TABLE_NAMES.joinToString(",") { "'$it'" }
        """ AND name NOT LIKE 'sqlite\_%' ESCAPE '\' AND name NOT IN ($internalNames)"""
    }

    return "SELECT name, type, sql FROM ${sqliteMasterTable(databaseName)} WHERE type IN ('table', 'view')$internalFilter ORDER BY type, name COLLATE NOCASE"
}

/**
 * `pragma_table_info()` as a table-valued function, so the table name (and schema) are bound rather than built into
 * the statement. The schema, when there is one, is the function's optional last argument.
 */
internal fun findColumnsInfoSql(databaseName: String): String =
    if (databaseName.isBlank()) {
        """SELECT name, type, "notnull", dflt_value, pk FROM pragma_table_info(?)"""
    } else {
        """SELECT name, type, "notnull", dflt_value, pk FROM pragma_table_info(?, ?)"""
    }

internal fun SQLiteStatement.bindFindColumnsArgs(tableName: String, databaseName: String) {
    bindText(1, tableName)
    if (databaseName.isNotBlank()) {
        bindText(2, databaseName)
    }
}

internal fun rowCountSql(tableName: String, databaseName: String): String =
    "SELECT count(*) FROM ${qualifiedTableName(tableName, databaseName)}"

/**
 * Read every row of a [findTablesInfoSql] statement.
 *
 * @param includeInternalTables false to also drop virtual table shadow tables (which the SQL alone cannot recognize)
 */
internal fun SQLiteStatement.readTableInfoRows(includeInternalTables: Boolean): List<DatabaseTableInfo> {
    val tables = buildList {
        while (step()) {
            val type = DatabaseTableType.fromSqliteType(getText(1)) ?: continue
            add(DatabaseTableInfo(name = getText(0), type = type, sql = getTextOrNull(2)))
        }
    }

    return if (includeInternalTables) tables else tables.withoutShadowTables()
}

private fun List<DatabaseTableInfo>.withoutShadowTables(): List<DatabaseTableInfo> {
    val shadowTableNames = flatMap { table ->
        val module = table.sql?.let { VIRTUAL_TABLE_MODULE_REGEX.find(it) }?.groupValues?.get(1)?.lowercase()
        SHADOW_TABLE_SUFFIXES[module].orEmpty().map { suffix -> "${table.name}_$suffix".lowercase() }
    }.toSet()

    return if (shadowTableNames.isEmpty()) this else filterNot { it.name.lowercase() in shadowTableNames }
}

/** Read every row of a [findColumnsInfoSql] statement. */
internal fun SQLiteStatement.readColumnInfoRows(): List<DatabaseColumnInfo> = buildList {
    while (step()) {
        add(
            DatabaseColumnInfo(
                name = getText(0),
                type = getText(1),
                notNull = getBoolean(2),
                defaultValue = getTextOrNull(3),
                primaryKeyPosition = getInt(4),
            )
        )
    }
}
