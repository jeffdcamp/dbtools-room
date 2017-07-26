package org.dbtools.sample.roomsqlite.datasource.database.main.foo

import android.arch.lifecycle.LiveData
import android.arch.persistence.room.Dao
import android.arch.persistence.room.Delete
import android.arch.persistence.room.Insert
import android.arch.persistence.room.Query
import android.arch.persistence.room.RoomDatabase
import android.arch.persistence.room.Update
import android.database.Cursor
import org.dbtools.android.room.DynamicQueryDao
import org.dbtools.query.sql.SQLQueryBuilder


@Dao
abstract class FooDao(db: RoomDatabase): DynamicQueryDao(db) {
    @Insert
    abstract fun insert(foo: Foo): Long

    @Update
    abstract fun update(foo: Foo): Int

    @Delete
    abstract fun delete(foo: Foo): Int

    @Query("DELETE FROM foo WHERE id = :id")
    abstract fun deleteById(id: Long): Int

    fun filterAll(startLocation: Location? = null, endLocation: Location? = null, enabled: Boolean? = null): LiveData<List<Foo>> {
        if (startLocation != null || endLocation != null) {
            checkNotNull(startLocation) { "startLocation cannot be null if endLocation is null" }
            checkNotNull(endLocation) { "endLocation cannot be null if startLocation is null" }
        }
        val query = SQLQueryBuilder()
                .table("foo")
                .orderBy("name")
        var argCount = 0
        if (startLocation != null && endLocation != null) {
            argCount += 4
        }
        if (enabled != null) {
            argCount++
        }
        val args = arrayOfNulls<Any>(argCount)
        if (startLocation != null && endLocation != null) {
            query.filter("latitude >= ? AND longitude >= ? AND latitude <= ? and longitude <= ?")
            args[0] = startLocation.latitude
            args[1] = startLocation.longitude
            args[2] = endLocation.latitude
            args[3] = endLocation.longitude
        }
        if (enabled != null) {
            query.filter("enabled = ?")
            args[args.size - 1] = when {
                enabled -> 1
                else -> 0
            }
        }
        return executeDynamicQuery(arrayOf("foo"), query.buildQuery(), args) { cursor ->
            val list = mutableListOf<Foo>()
            val cursorIndexOfId = cursor.getColumnIndexOrThrow("id")
            val cursorIndexOfName = cursor.getColumnIndexOrThrow("name")
            val cursorIndexOfLat = cursor.getColumnIndexOrThrow("latitude")
            val cursorIndexOfLong = cursor.getColumnIndexOrThrow("longitude")
            val cursorIndexOfEnabled = cursor.getColumnIndexOrThrow("enabled")

            if (cursor.moveToFirst()) {
                do {
                    list.add(mapToFoo(cursor, cursorIndexOfId, cursorIndexOfName, cursorIndexOfLat, cursorIndexOfLong, cursorIndexOfEnabled))
                } while (cursor.moveToNext())
            }

            return@executeDynamicQuery list
        }
    }

    fun findFirst(startLocation: Location? = null, endLocation: Location? = null, enabled: Boolean? = null): Foo? {
        if (startLocation != null || endLocation != null) {
            checkNotNull(startLocation) { "startLocation cannot be null if endLocation is null" }
            checkNotNull(endLocation) { "endLocation cannot be null if startLocation is null" }
        }
        val query = SQLQueryBuilder()
                .table("foo")
                .orderBy("name")
        var argCount = 0
        if (startLocation != null && endLocation != null) {
            argCount += 4
        }
        if (enabled != null) {
            argCount++
        }
        val args = arrayOfNulls<Any>(argCount)
        if (startLocation != null && endLocation != null) {
            query.filter("latitude >= ? AND longitude >= ? AND latitude <= ? and longitude <= ?")
            args[0] = startLocation.latitude
            args[1] = startLocation.longitude
            args[2] = endLocation.latitude
            args[3] = endLocation.longitude
        }
        if (enabled != null) {
            query.filter("enabled = ?")
            args[args.size - 1] = when {
                enabled -> 1
                else -> 0
            }
        }
        return executeDynamicQuery(query.buildQuery(), args) { cursor ->
            if (cursor.moveToFirst()) {
                val cursorIndexOfId = cursor.getColumnIndexOrThrow("id")
                val cursorIndexOfName = cursor.getColumnIndexOrThrow("name")
                val cursorIndexOfLat = cursor.getColumnIndexOrThrow("latitude")
                val cursorIndexOfLong = cursor.getColumnIndexOrThrow("longitude")
                val cursorIndexOfEnabled = cursor.getColumnIndexOrThrow("enabled")
                return@executeDynamicQuery mapToFoo(cursor, cursorIndexOfId, cursorIndexOfName, cursorIndexOfLat, cursorIndexOfLong, cursorIndexOfEnabled)
            }

            return@executeDynamicQuery null
        }
    }

    private fun mapToFoo(cursor: Cursor, cursorIndexOfId: Int, cursorIndexOfName: Int, cursorIndexOfLat: Int, cursorIndexOfLong: Int, cursorIndexOfEnabled: Int): Foo {
        return Foo().apply {
            id = cursor.getLong(cursorIndexOfId)
            name = cursor.getString(cursorIndexOfName)
            location = Location().apply {
                latitude = cursor.getDouble(cursorIndexOfLat)
                longitude = cursor.getDouble(cursorIndexOfLong)
            }
            enabled = cursor.getInt(cursorIndexOfEnabled) == 1
        }
    }

}