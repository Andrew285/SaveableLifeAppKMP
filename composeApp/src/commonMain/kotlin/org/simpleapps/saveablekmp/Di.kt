package org.simpleapps.saveablekmp

import org.koin.compose.viewmodel.dsl.viewModelOf
import org.koin.core.module.Module
import org.koin.dsl.module
import org.simpleapps.saveablekmp.data.db.DatabaseDriverFactory
import org.simpleapps.saveablekmp.data.db.SaveableDatabase
import org.simpleapps.saveablekmp.data.repository.SaveableRepository
import org.simpleapps.saveablekmp.domain.usecase.DeleteItemUseCase
import org.simpleapps.saveablekmp.domain.usecase.SaveItemUseCase
import org.simpleapps.saveablekmp.domain.usecase.UpdateItemUseCase
import org.simpleapps.saveablekmp.ui.main.MainViewModel
import org.simpleapps.saveablekmp.ui.settings.SettingsViewModel

// Platform-specific module (database driver)
expect val platformModule: Module

val dataModule = module {
    single { SaveableDatabase(get<DatabaseDriverFactory>().createDriver()) }
    single { SaveableRepository(get()) }
}

val domainModule = module {
    factory { SaveItemUseCase(get()) }
    factory { DeleteItemUseCase(get()) }
    factory { UpdateItemUseCase(get()) }
}

val viewModelModule = module {
    viewModelOf(::MainViewModel)
    viewModelOf(::SettingsViewModel)
}

val appModules = listOf(platformModule, dataModule, domainModule, viewModelModule)