package org.dbtools.room.data

/**
 * A column of a table or view, as reported by `PRAGMA table_info`.
 *
 * @property name Name of the column
 * @property type Declared type exactly as written in the CREATE statement (may be empty, since SQLite does not require one)
 * @property notNull true if the column has a NOT NULL constraint
 * @property defaultValue The DEFAULT expression as SQL text (for example `'abc'` or `0`), or null when there is none
 * @property primaryKeyPosition 1-based position of this column within the primary key, or 0 if it is not part of it
 */
data class DatabaseColumnInfo(
    val name: String,
    val type: String,
    val notNull: Boolean,
    val defaultValue: String?,
    val primaryKeyPosition: Int,
) {
    /** true if this column is part of the primary key */
    val isPrimaryKey: Boolean get() = primaryKeyPosition > 0
}
