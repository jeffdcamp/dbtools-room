package org.dbtools.sample.roomsqlite

import android.app.Application
import android.content.Context
import android.support.multidex.MultiDex
import android.util.Log


class App : Application() {

    override fun onCreate() {
        super.onCreate()
    }

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        if (filesDir != null) {
            MultiDex.install(this)
        } else {
            // During app install it might have experienced "INSTALL_FAILED_DEXOPT" (reinstall is the only known work-around)
            // https://code.google.com/p/android/issues/detail?id=8886
            val message = getString(R.string.app_name) + " is in a bad state, please uninstall/reinstall"
            Log.e("App", message)
        }
    }
}
