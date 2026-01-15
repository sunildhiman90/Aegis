package app.aegis.di

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import app.aegis.data.local.AegisDatabase
import app.aegis.data.local.getDatabaseBuilder
import app.aegis.platform.DeviceIdProvider
import org.koin.dsl.module

/**
 * Android platform module providing Context-aware database and platform services
 */
actual val platformModule =
    module {
        single<AegisDatabase> {
            getDatabaseBuilder(get())
                .setDriver(BundledSQLiteDriver())
                .build()
        }

        single { DeviceIdProvider(get()) }
    }
