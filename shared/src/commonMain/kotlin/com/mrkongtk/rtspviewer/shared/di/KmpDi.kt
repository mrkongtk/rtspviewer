package com.mrkongtk.rtspviewer.shared.di

import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.includes
import org.koin.dsl.module

val commonModule = module {
//    viewModel { AppViewModel(fileRepository = get()) }
}

expect val platformModule: Module

fun initKoin(config: KoinAppDeclaration? = null): KoinApplication {
    return startKoin {
        includes(config)
        modules(
            commonModule,
            platformModule
        )
    }
}
