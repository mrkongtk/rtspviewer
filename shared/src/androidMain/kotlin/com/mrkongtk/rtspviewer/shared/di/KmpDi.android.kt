package com.mrkongtk.rtspviewer.shared.di

import com.mrkongtk.rtspviewer.shared.data.repository.FileRepository
import com.mrkongtk.rtspviewer.shared.data.repository.FileRepositoryAndroidImpl
import org.koin.dsl.module

actual val platformModule = module {
    single<FileRepository> { FileRepositoryAndroidImpl(get()) }
}
