package app.aegis.di

import app.aegis.data.repository.TrustedContactRepositoryImpl
import app.aegis.domain.repository.TrustedContactRepository
import app.aegis.ui.viewmodel.TrustedContactViewModel
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

    // ViewModels
    viewModelOf(::TrustedContactViewModel)
}
