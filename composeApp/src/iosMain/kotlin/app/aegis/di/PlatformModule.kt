package app.aegis.di

import app.aegis.data.local.AegisDatabase
import app.aegis.data.local.getDatabaseBuilder
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import org.koin.dsl.module

/**
 * iOS platform module for database
 */
actual val platformModule = module {
    single<AegisDatabase> {
        getDatabaseBuilder()
            .setDriver(BundledSQLiteDriver())
            .build()
    }
}
