package com.execos.di

import com.execos.BuildConfig
import com.execos.data.remote.OpenAiApi
import com.execos.data.strava.StravaApi
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun provideGson(): Gson = GsonBuilder().create()

    @Provides
    @Singleton
    fun provideOkHttp(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val key = BuildConfig.OPENAI_API_KEY
                val request = if (
                    key.isNotBlank() &&
                    chain.request().url.host.contains("api.openai.com")
                ) {
                    chain.request().newBuilder()
                        .header("Authorization", "Bearer $key")
                        .build()
                } else {
                    chain.request()
                }
                chain.proceed(request)
            }
            .addInterceptor(logging)
            .build()
    }

    @Provides
    @Singleton
    fun provideOpenAiApi(okHttpClient: OkHttpClient, gson: Gson): OpenAiApi =
        Retrofit.Builder()
            .baseUrl("https://api.openai.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(OpenAiApi::class.java)

    @Provides
    @Singleton
    fun provideStravaApi(okHttpClient: OkHttpClient, gson: Gson): StravaApi =
        Retrofit.Builder()
            .baseUrl("https://www.strava.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(StravaApi::class.java)
}
