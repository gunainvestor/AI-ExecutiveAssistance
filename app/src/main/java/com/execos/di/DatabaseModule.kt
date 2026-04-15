package com.execos.di

import android.content.Context
import androidx.room.Room
import com.execos.data.local.ExecOsDatabase
import com.execos.data.local.Migrations
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
    fun provideDatabase(@ApplicationContext context: Context): ExecOsDatabase =
        Room.databaseBuilder(context, ExecOsDatabase::class.java, "execos.db")
            .addMigrations(Migrations.MIGRATION_1_2, Migrations.MIGRATION_2_3)
            .build()
}
