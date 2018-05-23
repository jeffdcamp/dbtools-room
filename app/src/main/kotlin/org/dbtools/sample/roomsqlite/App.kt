package org.dbtools.sample.roomsqlite

import android.support.multidex.MultiDexApplication
import timber.log.Timber


class App : MultiDexApplication() {

    override fun onCreate() {
        super.onCreate()
        setupLogging()
    }

    private fun setupLogging() {
        Timber.plant(Timber.DebugTree())
    }
}
