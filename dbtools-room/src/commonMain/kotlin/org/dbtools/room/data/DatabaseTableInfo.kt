package org.dbtools.room.data

/**
 * A table or view in a database, as recorded in `sqlite_master`.
 *
 * @property name Name of the table or view
 * @property type Whether this is a table or a view
 * @property sql The `CREATE TABLE` / `CREATE VIEW` statement SQLite stored for it. Carries detail the column list
 * cannot (composite keys, foreign keys, constraints). Null for objects SQLite creates internally without one.
 */
data class DatabaseTableInfo(
    val name: String,
    val type: DatabaseTableType,
    val sql: String?,
)
