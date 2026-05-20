package com.allernutri.app.di

import com.allernutri.app.domain.RezeptAnalyseUseCase
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
