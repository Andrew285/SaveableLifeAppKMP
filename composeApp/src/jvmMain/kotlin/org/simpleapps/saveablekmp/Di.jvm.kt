package org.simpleapps.saveablekmp

import org.koin.core.module.Module
import org.koin.dsl.module
import org.simpleapps.saveablekmp.data.db.DatabaseDriverFactory

actual val platformModule: Module
    get() = module {
        single { DatabaseDriverFactory() }
    }