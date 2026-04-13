package org.simpleapps.saveablekmp

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.simpleapps.saveablekmp.sync.SyncWorker

class SaveableApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@SaveableApplication)
            modules(appModules)
        }
        SyncWorker.schedule(this)
    }
}