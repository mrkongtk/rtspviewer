package com.mrkongtk.rtspviewer.di

import android.content.Context
import androidx.room.Room
import com.mrkongtk.rtspviewer.data.repository.RTSPItemRepository
import com.mrkongtk.rtspviewer.data.repository.RTSPItemRepositoryImpl
import com.mrkongtk.rtspviewer.shared.data.database.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Dagger Hilt module responsible for providing application-level dependencies.
 *
 * This module is installed in the [SingletonComponent], which ensures that the dependencies
 * provided here live as long as the application itself.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    /**
     * Provides a singleton instance of the [RTSPItemRepository].
     *
     * This method satisfies dependencies for the repository interface by returning
     * the concrete [RTSPItemRepositoryImpl] implementation.
     *
     * @param context The application context.
     * @param db The Room database instance.
     * @param fileRepository The repository used for file operations.
     * @return The concrete implementation of the repository.
     */
    @Provides
    @Singleton
    fun provideRTSPItemRepository(
        @ApplicationContext context: Context,
        db: AppDatabase
    ): RTSPItemRepository {
        return RTSPItemRepositoryImpl(context, db)
    }

    /**
     * Provides a singleton instance of the [AppDatabase].
     *
     * This method initializes the Room database with the name "RTSP_Viewer_database".
     *
     * @param context The application context provided by Hilt, used to create the database builder.
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
