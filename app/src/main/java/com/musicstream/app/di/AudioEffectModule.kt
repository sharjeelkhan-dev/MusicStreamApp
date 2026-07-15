package com.musicstream.app.di

import com.musicstream.app.service.audio_fx.AudioEffectManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AudioEffectModule {

    @Provides
    @Singleton
    fun provideAudioEffectManager(): AudioEffectManager {
        return AudioEffectManager()
    }
}
