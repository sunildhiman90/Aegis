package app.aegis.di

import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration

/**
 * Initialize Koin for dependency injection
 */
fun initKoin(appDeclaration: KoinAppDeclaration? = {}) =
    startKoin {
        appDeclaration?.invoke(this)
        modules(
            platformModule,
            appModule,
            networkModule,
        )
    }
