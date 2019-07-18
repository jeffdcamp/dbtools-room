package org.dbtools.sample.roomsqlite

import android.app.Application
import com.jakewharton.threetenabp.AndroidThreeTen
import timber.log.Timber


class App : Application() {

    override fun onCreate() {
        super.onCreate()
        AndroidThreeTen.init(this)
        setupLogging()
    }

    private fun setupLogging() {
        Timber.plant(Timber.DebugTree())
    }
}
