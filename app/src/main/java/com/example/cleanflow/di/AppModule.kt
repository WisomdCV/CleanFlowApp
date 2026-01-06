package com.example.cleanflow.di

import android.content.Context
import com.example.cleanflow.data.local.AppDatabase
import com.example.cleanflow.data.local.TrashDao
import com.example.cleanflow.data.repository.MediaRepositoryImpl
import com.example.cleanflow.data.repository.MediaStoreDataSource
import com.example.cleanflow.data.repository.SettingsRepositoryImpl
import com.example.cleanflow.data.repository.TrashRepository
import com.example.cleanflow.domain.repository.MediaRepository
import com.example.cleanflow.domain.repository.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getInstance(context)
    }

    @Provides
    @Singleton
    fun provideTrashDao(database: AppDatabase): TrashDao {
        return database.trashDao()
    }

    @Provides
    @Singleton
    fun provideMediaStoreDataSource(@ApplicationContext context: Context): MediaStoreDataSource {
        return MediaStoreDataSource(context)
    }

    @Provides
    @Singleton
    fun provideTrashRepository(trashDao: TrashDao): TrashRepository {
        return TrashRepository(trashDao)
    }

    @Provides
    @Singleton
    fun provideMediaRepository(
        dataSource: MediaStoreDataSource,
        trashRepository: TrashRepository
    ): MediaRepository {
        return MediaRepositoryImpl(dataSource, trashRepository)
    }

    @Provides
    @Singleton
    fun provideSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository {
        return impl
    }
}
