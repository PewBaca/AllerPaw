package com.allerpaw.app.di

import com.allerpaw.app.domain.RezeptAnalyseUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DomainModule {
    @Provides
    @Singleton
    fun provideRezeptAnalyseUseCase(): RezeptAnalyseUseCase = RezeptAnalyseUseCase()
}
