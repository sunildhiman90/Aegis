package app.aegis.di

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import app.aegis.data.local.AegisDatabase
import app.aegis.data.local.getDatabaseBuilder
import app.aegis.platform.DeviceIdProvider
import org.koin.dsl.module

/**
 * iOS platform module for database and platform services
 */
actual val platformModule =
    module {
        single<AegisDatabase> {
            getDatabaseBuilder()
                .setDriver(BundledSQLiteDriver())
                .build()
        }

        single { DeviceIdProvider() }
    }
