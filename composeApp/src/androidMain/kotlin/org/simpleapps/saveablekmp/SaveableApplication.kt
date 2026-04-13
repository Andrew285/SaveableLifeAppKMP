package org.simpleapps.saveablekmp

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class SaveableApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@SaveableApplication)
            modules(appModules)
        }
    }
}