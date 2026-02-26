package com.mrkongtk.rtspviewer.shared.di

import androidx.room.RoomDatabaseConstructor
import com.mrkongtk.rtspviewer.shared.data.database.AppDatabase
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

@Suppress("KotlinNoActualForExpect", "EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}

fun initKoin(config: KoinAppDeclaration? = null): KoinApplication {
    return startKoin {
        includes(config)
        modules(
            commonModule,
            platformModule
        )
    }
}
