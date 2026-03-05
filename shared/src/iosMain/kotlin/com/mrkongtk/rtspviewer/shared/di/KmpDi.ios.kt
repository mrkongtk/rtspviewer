package com.mrkongtk.rtspviewer.shared.di

import com.mrkongtk.rtspviewer.shared.data.DeviceInfo
import com.mrkongtk.rtspviewer.shared.data.DeviceInfoIosImpl
import com.mrkongtk.rtspviewer.shared.data.database.AppDatabase
import com.mrkongtk.rtspviewer.shared.data.database.DatabaseBuilderIosImpl
import com.mrkongtk.rtspviewer.shared.data.database.getDatabase
import com.mrkongtk.rtspviewer.shared.data.repository.FileRepository
import com.mrkongtk.rtspviewer.shared.data.repository.FileRepositoryIosImpl
import org.koin.dsl.module

/**
 * iOS implementation of the platform-specific dependencies.
 */
actual val platformModule = module {
    single<FileRepository> { FileRepositoryIosImpl() }
    single<DeviceInfo> { DeviceInfoIosImpl() }
    single<AppDatabase> { DatabaseBuilderIosImpl(get()).getDatabase() }
}
