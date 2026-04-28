package com.allerpaw.app.di

import android.content.Context
import androidx.room.Room
import com.allerpaw.app.data.local.AppDatabase
import com.allerpaw.app.data.local.dao.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): AppDatabase =
        Room.databaseBuilder(ctx, AppDatabase::class.java, "allerpaw.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun provideHundDao(db: AppDatabase): HundDao = db.hundDao()
    @Provides fun provideZutatenDao(db: AppDatabase): ZutatenDao = db.zutatenDao()
    @Provides fun provideRezeptDao(db: AppDatabase): RezeptDao = db.rezeptDao()
    @Provides fun provideTagebuchDao(db: AppDatabase): TagebuchDao = db.tagebuchDao()
    @Provides fun provideParameterDao(db: AppDatabase): ParameterDao = db.parameterDao()
    @Provides fun provideHundZustandDao(db: AppDatabase): HundZustandDao = db.hundZustandDao()
    @Provides fun provideTaskDao(db: AppDatabase): TaskDao = db.taskDao()
}
