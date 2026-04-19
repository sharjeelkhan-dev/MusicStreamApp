package com.musicstream.app.di

import com.musicstream.app.data.repository.MusicRepositoryImpl
import com.musicstream.app.data.repository.UserRepositoryImpl
import com.musicstream.app.domain.repository.MusicRepository
import com.musicstream.app.domain.repository.UserRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindMusicRepository(impl: MusicRepositoryImpl): MusicRepository

    @Binds
    @Singleton
    abstract fun bindUserRepository(impl: UserRepositoryImpl): UserRepository
}
