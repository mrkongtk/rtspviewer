package com.mrkongtk.rtspviewer.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Dagger Hilt module responsible for providing application-level dependencies.
 *
 * This module is installed in the [SingletonComponent], which ensures that the dependencies
 * provided here live as long as the application itself.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

}
