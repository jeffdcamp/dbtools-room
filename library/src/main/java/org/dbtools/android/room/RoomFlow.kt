@file:Suppress("MemberVisibilityCanPrivate", "unused")

package org.dbtools.android.room

import androidx.room.InvalidationTracker
import androidx.room.RoomDatabase
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext

/**
 * This class creates a Flow that will emit when there are changes to specific tables of a database.
 * This is useful when simple Flow from Room Dao objects is not enough.
 *
 * Similar code in Room CoroutinesRoom.createFlow(...) code (used by generated Room Dao code)
 *
 * Example:
 *
 * // CREATE the flow
 * val flow = mainDatabase.toFlow("individual", "household") {
 *     // do lots of work to pull data from different resources and possibly perform some custom data manipulation
 *     // this code will get executed anytime there are changes on the "individual" table
 *     arrayOf("a", "b", "c")
 * }
 *
 * // OBSERVE
 * flow.collect { data ->
 *     // show data ("a", "b", "c")
 * })
 *
 * // CHANGE - This will cause the flow to emit to the collector
 * individualDao().updateName(id, "Bob")
 *
 *
 * (Alternative) Multiple Databases:
 * val flow = mainDatabase.toFlow(listOf(database1.tableChangeReferences("downloads"), database2.tableChangeReferences("collections"))) {
 *     arrayOf("a", "b", "c")
 * }
 */
@Suppress("unused")
object RoomFlow {

    /**
     * Return data retrieved via block parameter as Flow
     *
     * @param tableChangeReferences Tables that will cause this Flow to be triggered
     * @param inTransaction Determine which coroutine dispatcher to use
     * @param block Function that is executed to get data
     *
     * @return Flow<T>
     */
    fun <T> toFlow(
            tableChangeReferences: List<TableChangeReference>?,
            inTransaction: Boolean = false,
            block: suspend () -> T
    ): Flow<T> = flow {
        // Observer channel receives signals from the invalidation tracker to emit queries.
        val observerChannel = Channel<Unit>(Channel.CONFLATED)

        tableChangeReferences?.forEach { tableChangeManager ->
            val db = tableChangeManager.database
            val tableNames: Array<String> = tableChangeManager.tableNames.toTypedArray()

            val observer = object : InvalidationTracker.Observer(tableNames) {
                override fun onInvalidated(tables: Set<String>) {
                    observerChannel.trySend(Unit).isSuccess
                }
            }

            observerChannel.trySend(Unit).isSuccess // Initial signal to perform first query.

            val flowContext = kotlin.coroutines.coroutineContext
            val queryContext = if (inTransaction) db.getTransactionExecutor().asCoroutineDispatcher() else db.getQueryExecutor().asCoroutineDispatcher()
            withContext(queryContext) {
                db.invalidationTracker.addObserver(observer)
                try {
                    // Iterate until cancelled, transforming observer signals to query results to
                    // be emitted to the flow.
                    for (signal in observerChannel) {
                        val result = block()
                        withContext(flowContext) { emit(result) }
                    }
                } finally {
                    db.invalidationTracker.removeObserver(observer)
                }
            }
        }
    }
}

fun <T> RoomDatabase.toFlow(vararg tableNames: String, inTransaction: Boolean = false, block: suspend () -> T): Flow<T> {
    return RoomFlow.toFlow(listOf(TableChangeReference(this, tableNames.toList())), inTransaction) { block() }
}

