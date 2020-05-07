package org.dbtools.sample.roomsqlite

import android.app.Application
import timber.log.Timber


class App : Application() {

    override fun onCreate() {
        super.onCreate()
        setupLogging()
    }

    private fun setupLogging() {
        Timber.plant(Timber.DebugTree())
    }
}
