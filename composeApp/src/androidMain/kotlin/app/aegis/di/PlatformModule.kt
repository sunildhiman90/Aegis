package app.aegis.di

import app.aegis.data.local.AegisDatabase
import app.aegis.data.local.getDatabaseBuilder
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import org.koin.dsl.module

/**
 * Android platform module providing Context-aware database
 */
actual val platformModule = module {
    single<AegisDatabase> {
        getDatabaseBuilder(get())
            .setDriver(BundledSQLiteDriver())
            .build()
    }
}
