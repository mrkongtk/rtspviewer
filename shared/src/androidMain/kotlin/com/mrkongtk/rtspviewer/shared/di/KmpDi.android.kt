package com.mrkongtk.rtspviewer.shared.di

import com.mrkongtk.rtspviewer.shared.data.DeviceInfo
import com.mrkongtk.rtspviewer.shared.data.DeviceInfoAndroidImpl
import com.mrkongtk.rtspviewer.shared.data.database.AppDatabase
import com.mrkongtk.rtspviewer.shared.data.database.DatabaseBuilderAndroidImpl
import com.mrkongtk.rtspviewer.shared.data.database.getDatabase
import com.mrkongtk.rtspviewer.shared.data.repository.FileRepository
import com.mrkongtk.rtspviewer.shared.data.repository.FileRepositoryAndroidImpl
import org.koin.dsl.module

/**
 * Android implementation of the platform-specific dependencies.
 * Requires the Android Context for most implementations.
 */
actual val platformModule = module {
    single<FileRepository> { FileRepositoryAndroidImpl(get()) }
    single<DeviceInfo> { DeviceInfoAndroidImpl(get()) }
    single<AppDatabase> { DatabaseBuilderAndroidImpl(get(), get()).getDatabase() }
}
