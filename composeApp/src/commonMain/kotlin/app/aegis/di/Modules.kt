package app.aegis.di

import app.aegis.data.repository.TrustedContactRepositoryImpl
import app.aegis.data.repository.IncidentRepositoryImpl
import app.aegis.data.settings.AppSettingsRepository
import app.aegis.domain.repository.TrustedContactRepository
import app.aegis.domain.repository.IncidentRepository
import app.aegis.ui.viewmodel.TrustedContactViewModel
import app.aegis.ui.viewmodel.SettingsViewModel
import app.aegis.ui.viewmodel.ActivityLogViewModel
import app.aegis.ui.viewmodel.DashboardViewModel
import app.aegis.ui.viewmodel.ProfileViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

import app.aegis.ai.gemini.GeminiClient
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

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
    viewModelOf(::DashboardViewModel)
    viewModelOf(::ProfileViewModel)
}

val networkModule = module {
    single {
        HttpClient {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                })
            }
            install(Logging) {
                logger = object : Logger {
                    override fun log(message: String) {
                        println("HTTP Client:$message")
                    }
                }
                //TODO, enable only in debug build
                level = LogLevel.ALL
            }
        }
    }

    single { GeminiClient(get()) }
}

