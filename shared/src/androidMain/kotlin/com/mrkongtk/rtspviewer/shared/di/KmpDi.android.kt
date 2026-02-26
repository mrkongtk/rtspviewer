package com.mrkongtk.rtspviewer.shared.di

import com.mrkongtk.rtspviewer.shared.data.DeviceInfo
import com.mrkongtk.rtspviewer.shared.data.DeviceInfoAndroidImpl
import com.mrkongtk.rtspviewer.shared.data.database.AppDatabase
import com.mrkongtk.rtspviewer.shared.data.database.DatabaseBuilderAndroidImpl
import com.mrkongtk.rtspviewer.shared.data.database.getDatabase
import com.mrkongtk.rtspviewer.shared.data.repository.FileRepository
import com.mrkongtk.rtspviewer.shared.data.repository.FileRepositoryAndroidImpl
import org.koin.dsl.module

actual val platformModule = module {
    single<FileRepository> { FileRepositoryAndroidImpl(get()) }
    single<DeviceInfo> { DeviceInfoAndroidImpl(get()) }
    single<AppDatabase> { DatabaseBuilderAndroidImpl(get()).getDatabase() }
}
