package org.dbtools.android.room.log

import android.annotation.SuppressLint
import android.util.Log
import androidx.room.RoomDatabase
import timber.log.Timber

/**
 * Room QueryCallback function extension for logging executed queries
 * @param queryContext Name that helps identify the source of the query (Example: "main.db", "userdata.db", "book-1234.db", etc)
 * @param enabled Allow an app to dynamically enable/disable logging (default: true)
 * @param combineSqlAndArgs If true, then show queries with ? replaced with arg values (default: true)
 * @param useTimber Use Timber or Log for logging (default: true)
 */
fun <T : RoomDatabase> RoomDatabase.Builder<T>.setLoggingQueryCallback(
    queryContext: String,
    enabled: Boolean = true,
    combineSqlAndArgs: Boolean = true,
    useTimber: Boolean = true
): RoomDatabase.Builder<T> {
    return this.setQueryCallback(LoggingQueryCallback(queryContext, enabled, combineSqlAndArgs, useTimber), LoggingExecutor)
}

/**
 * Room QueryCallback for logging executed queries
 * @param queryContext Name that helps identify the source of the query (Example: "main.db", "userdata.db", "book-1234.db", etc)
 * @param enabled Allow an app to dynamically enable/disable logging (default: true)
 * @param combineSqlAndArgs If true, then show queries with ? replaced with arg values (default: true)
 * @param useTimber Use Timber or Log for logging (default: true)
 */
class LoggingQueryCallback(
    private val queryContext: String,
    var enabled: Boolean = true,
    private val combineSqlAndArgs: Boolean = true,
    private val useTimber: Boolean = true
) : RoomDatabase.QueryCallback {
    override fun onQuery(sqlQuery: String, bindArgs: MutableList<Any?>) {
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

        val formattedQuery = sqlQuery.replace("?", "%s")
        val formattedSql = String.format(formattedQuery, *formattedArgs.toTypedArray())

        log("$queryContext Query: [$formattedSql]")
    }

    @SuppressLint("LogNotTimber")
    private fun log(message: String) {
        if (useTimber) {
            Timber.d(message)
        } else {
            Log.d("LoggingQueryCallback", message)
        }
    }
}