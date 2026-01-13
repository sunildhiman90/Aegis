package app.aegis.di

import app.aegis.data.repository.TrustedContactRepositoryImpl
import app.aegis.data.repository.IncidentRepositoryImpl
import app.aegis.data.settings.AppSettingsRepository
import app.aegis.domain.repository.TrustedContactRepository
import app.aegis.domain.repository.IncidentRepository
import app.aegis.ui.viewmodel.TrustedContactViewModel
import app.aegis.ui.viewmodel.SettingsViewModel
import app.aegis.ui.viewmodel.ActivityLogViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/**
 * Common app module with shared dependencies
 */
val appModule = module {
    // Repositories
    single<TrustedContactRepository> {
        TrustedContactRepositoryImpl(get<app.aegis.data.local.AegisDatabase>().trustedContactDao())
    }

    single<IncidentRepository> {
        IncidentRepositoryImpl(get<app.aegis.data.local.AegisDatabase>().incidentDao())
    }

    single { AppSettingsRepository() }

    // ViewModels
    viewModelOf(::TrustedContactViewModel)
    viewModelOf(::SettingsViewModel)
    viewModelOf(::ActivityLogViewModel)
}
