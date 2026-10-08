package org.dbtools.room.data

/**
 * The kind of schema object a [DatabaseTableInfo] describes, from the `type` column of `sqlite_master`.
 */
enum class DatabaseTableType(val sqliteType: String) {
    TABLE("table"),
    VIEW("view");

    companion object {
        /** @return the type for a `sqlite_master.type` value, or null for one that is not a table or view (index, trigger) */
        fun fromSqliteType(sqliteType: String): DatabaseTableType? = entries.firstOrNull { it.sqliteType == sqliteType }
    }
}
