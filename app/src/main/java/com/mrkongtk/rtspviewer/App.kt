package com.mrkongtk.rtspviewer

import android.app.Application
import com.mrkongtk.rtspviewer.shared.di.initKoin
import dagger.hilt.android.HiltAndroidApp
import org.koin.android.ext.koin.androidContext

@HiltAndroidApp
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin {
            androidContext(this@App)
        }
    }
}
