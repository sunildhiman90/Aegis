package app.aegis

import android.app.Application
import app.aegis.di.initKoin
import org.koin.android.ext.koin.androidContext

class AegisApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin {
            androidContext(this@AegisApplication)
        }
    }
}
