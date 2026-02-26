package com.mrkongtk.rtspviewer.shared.di

import com.mrkongtk.rtspviewer.shared.data.repository.FileRepository
import com.mrkongtk.rtspviewer.shared.data.repository.FileRepositoryIosImpl
import org.koin.dsl.module

actual val platformModule = module {
    single<FileRepository> { FileRepositoryIosImpl() }
}
