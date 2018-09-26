package org.dbtools.sample.roomsqlite

import androidx.multidex.MultiDexApplication
import com.jakewharton.threetenabp.AndroidThreeTen
import timber.log.Timber


class App : MultiDexApplication() {

    override fun onCreate() {
        super.onCreate()
        AndroidThreeTen.init(this)
        setupLogging()
    }

    private fun setupLogging() {
        Timber.plant(Timber.DebugTree())
    }
}
