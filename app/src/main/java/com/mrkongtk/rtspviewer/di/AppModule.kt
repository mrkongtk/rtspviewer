package com.mrkongtk.rtspviewer.di

import android.content.Context
import androidx.room.Room
import com.mrkongtk.rtspviewer.data.database.AppDatabase
import com.mrkongtk.rtspviewer.data.repository.RTSPItemRepository
import com.mrkongtk.rtspviewer.data.repository.RTSPItemWithSampleInitRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Dagger Hilt module responsible for providing application-level dependencies.
 *
 * This module is installed in the [SingletonComponent], meaning the dependencies
 * defined here will exist for the entire lifecycle of the application.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    /**
     * Provides a singleton instance of the [RTSPItemRepository].
     *
     * It binds the interface to the [RTSPItemWithSampleInitRepositoryImpl] implementation,
     * which initializes the repository with sample data.
     *
     * @param context The application context injected by Hilt.
     * @param db The [AppDatabase] instance required by the repository.
     * @return The concrete implementation of the repository.
     */
    @Provides
    @Singleton
    fun provideRTSPItemRepository(
        @ApplicationContext context: Context,
        db: AppDatabase
    ): RTSPItemRepository {
        return RTSPItemWithSampleInitRepositoryImpl(context, db)
    }

    /**
     * Provides a singleton instance of the [AppDatabase].
     *
     * This method initializes the Room database with the name "RTSP_Viewer_database".
     *
     * @param context The application context used to build the database.
     * @return The built Room database instance.
     */
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room
            .databaseBuilder(context, AppDatabase::class.java, "RTSP_Viewer_database")
            .build()
    }
}
