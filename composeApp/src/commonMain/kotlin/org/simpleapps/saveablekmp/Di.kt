package org.simpleapps.saveablekmp

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import org.koin.compose.viewmodel.dsl.viewModelOf
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import org.simpleapps.saveablekmp.data.db.DatabaseDriverFactory
import org.simpleapps.saveablekmp.data.db.SaveableDatabase
import org.simpleapps.saveablekmp.data.repository.SaveableRepository
import org.simpleapps.saveablekmp.domain.usecase.DeleteItemUseCase
import org.simpleapps.saveablekmp.domain.usecase.SaveItemUseCase
import org.simpleapps.saveablekmp.domain.usecase.UpdateItemUseCase
import org.simpleapps.saveablekmp.sync.DriveSync
import org.simpleapps.saveablekmp.sync.SyncManager
import org.simpleapps.saveablekmp.ui.flashcards.FlashcardsViewModel
import org.simpleapps.saveablekmp.ui.main.MainViewModel
import org.simpleapps.saveablekmp.ui.settings.SettingsViewModel
import org.simpleapps.saveablekmp.ui.tasks.TasksViewModel

// Platform-specific module (database driver)
expect val platformModule: Module

val dataModule = module {
    single { SaveableDatabase(get<DatabaseDriverFactory>().createDriver()) }
    single { SaveableRepository(get()) }
    single {
        DriveSync(
            repository = get(),
            httpClient = HttpClient {
                install(ContentNegotiation) { json() }
            },
        )
    }
    single { SyncManager(get(), get()) }
}

val domainModule = module {
    factory { SaveItemUseCase(get()) }
    factory { DeleteItemUseCase(get()) }
    factory { UpdateItemUseCase(get()) }
}

val viewModelModule = module {
    viewModel {
        MainViewModel(
            repository = get(),
            saveItem = get(),
            deleteItem = get(),
            updateItem = get(),
            authManager = get(),
            driveSync = get(),
            syncManager = get(),
        )
    }
    viewModel {
        SettingsViewModel(
            repository = get(),
            driveSync = get(),
            authManager = get(),
        )
    }
    viewModel {
        FlashcardsViewModel(
            repository = get(),
            syncManager = get(),
        )
    }
    viewModel {
        TasksViewModel(
            repository = get(),
            saveItem = get(),
            updateItem = get(),
            syncManager = get(),
        )
    }
}

val appModules = listOf(platformModule, dataModule, domainModule, viewModelModule)