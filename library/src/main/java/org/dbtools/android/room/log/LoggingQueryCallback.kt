package org.dbtools.android.room.log

import androidx.room.RoomDatabase
import co.touchlab.kermit.Logger

/**
 * Room QueryCallback function extension for logging executed queries
 *
 * @param T Type of RoomDatabase
 * @param queryContext Name that helps identify the source of the query (Example: "main.db", "userdata.db", "book-1234.db", etc)
 * @param enabled Allow an app to dynamically enable/disable logging (default: true)
 * @param combineSqlAndArgs If true, then show queries with ? replaced with arg values (default: true)
 *
 * @return RoomDatabase.Builder<T>
 */
fun <T : RoomDatabase> RoomDatabase.Builder<T>.setLoggingQueryCallback(
    queryContext: String,
    enabled: Boolean = true,
    combineSqlAndArgs: Boolean = true
): RoomDatabase.Builder<T> {
    return this.setQueryCallback(LoggingQueryCallback(queryContext, enabled, combineSqlAndArgs), LoggingExecutor)
}

/**
 * Room QueryCallback for logging executed queries
 * @property queryContext Name that helps identify the source of the query (Example: "main.db", "userdata.db", "book-1234.db", etc)
 * @property enabled Allow an app to dynamically enable/disable logging (default: true)
 * @property combineSqlAndArgs If true, then show queries with ? replaced with arg values (default: true)
 */
class LoggingQueryCallback(
    private val queryContext: String,
    var enabled: Boolean = true,
    private val combineSqlAndArgs: Boolean = true
) : RoomDatabase.QueryCallback {
    var lastLog = ""
        private set

    override fun onQuery(sqlQuery: String, bindArgs: List<Any?>) {
        if (!enabled) {
            return
        }

        // Separated
        if(!combineSqlAndArgs) {
            log("$queryContext Query: [$sqlQuery]  Args: $bindArgs")
            return
        }

        // Combined
        val formattedArgs: List<String> = bindArgs.map { arg ->
            when (arg) {
                null -> "NULL"
                is Number -> arg.toString()
                is Boolean -> if (arg) "1" else "0"
                is ByteArray -> "BLOB"
                is String -> "'$arg'"
                else -> "NULL" // nulls also come in as objects
            }
        }

        var formattedSql = sqlQuery
        formattedArgs.forEach { arg ->
            formattedSql = replaceNextArg(formattedSql, arg)
        }

        log("$queryContext Query: [$formattedSql]")
    }

    private fun replaceNextArg(sqlQuery: String, arg: String): String {
        var inText = false
        var foundIndex = -1

        // find the index of the next ? that is NOT in text
        val chars = sqlQuery.toCharArray()
        for(index in 0..chars.size) {
            val c = chars[index]
            when {
                c == '?' && !inText -> {
                    foundIndex = index
                    break
                }
                c == '\'' -> inText = !inText
                else -> {
                    // continue
                }
            }
        }

        return if (foundIndex == -1) {
            sqlQuery
        } else {
            sqlQuery.replaceRange(foundIndex, foundIndex + 1, arg)
        }
    }

    private fun log(message: String) {
        lastLog = message
        Logger.d { message }
    }
}