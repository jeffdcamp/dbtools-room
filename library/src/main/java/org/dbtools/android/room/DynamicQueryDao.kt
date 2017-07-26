package org.dbtools.android.room

import android.annotation.SuppressLint
import android.arch.lifecycle.ComputableLiveData
import android.arch.lifecycle.LiveData
import android.arch.persistence.room.InvalidationTracker
import android.arch.persistence.room.RoomDatabase
import android.database.Cursor


// TODO:: https://issuetracker.google.com/issues/62103290 replace this class when the issue noted is closed.
abstract class DynamicQueryDao(protected val db: RoomDatabase) {

    fun <T> executeDynamicQuery(query: String, args: Array<out Any?>? = null, converter: (Cursor) -> T): T {
        return db.query(query, args).use { converter(it) }
    }

    @SuppressLint("RestrictedApi")
    fun <T> executeDynamicQuery(tables: Array<out String>, query: String, args: Array<out Any?>? = null, converter: (Cursor) -> T): LiveData<T> {
        return object : ComputableLiveData<T>() {
            private var observer: InvalidationTracker.Observer? = null

            override fun compute(): T {
                if (observer == null) {
                    observer = object : InvalidationTracker.Observer(tables) {
                        override fun onInvalidated(tableSet: MutableSet<String>) { invalidate() }
                    }
                    db.invalidationTracker.addWeakObserver(observer)
                }
                return db.query(query, args).use { converter(it) }
            }
        }.liveData
    }
}