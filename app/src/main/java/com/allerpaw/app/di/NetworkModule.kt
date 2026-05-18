package com.allerpaw.app.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.allerpaw.app.data.remote.api.BrightSkyApi
import com.allerpaw.app.data.remote.api.OpenMeteoApi
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences>
    by preferencesDataStore(name = "allerpaw_prefs")

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides @Singleton
    fun provideMoshi(): Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory()).build()

    @Provides @Singleton
    fun provideOkHttp(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
        .build()

    @Provides @Singleton @Named("brightsky")
    fun provideBrightSkyRetrofit(okHttp: OkHttpClient, moshi: Moshi): Retrofit =
        Retrofit.Builder().baseUrl("https://api.brightsky.dev/")
            .client(okHttp).addConverterFactory(MoshiConverterFactory.create(moshi)).build()

    @Provides @Singleton
    fun provideBrightSkyApi(@Named("brightsky") r: Retrofit): BrightSkyApi =
        r.create(BrightSkyApi::class.java)

    @Provides @Singleton @Named("openmeteo")
    fun provideOpenMeteoRetrofit(okHttp: OkHttpClient, moshi: Moshi): Retrofit =
        Retrofit.Builder().baseUrl("https://air-quality-api.open-meteo.com/")
            .client(okHttp).addConverterFactory(MoshiConverterFactory.create(moshi)).build()

    @Provides @Singleton
    fun provideOpenMeteoApi(@Named("openmeteo") r: Retrofit): OpenMeteoApi =
        r.create(OpenMeteoApi::class.java)

    @Provides @Singleton
    fun provideDataStore(@ApplicationContext ctx: Context): DataStore<Preferences> =
        ctx.dataStore
}
