package com.babymonitor.di

import android.content.Context
import com.babymonitor.camera.CameraManager
import com.babymonitor.data.SettingsRepository
import com.babymonitor.detection.AlertManager
import com.babymonitor.detection.AudioAnalyzer
import com.babymonitor.detection.MotionDetector
import com.babymonitor.streaming.SignalingClient
import com.babymonitor.streaming.StreamingManager
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
    fun provideCameraManager(
        @ApplicationContext context: Context
    ): CameraManager = CameraManager(context)

    @Provides
    @Singleton
    fun provideMotionDetector(): MotionDetector = MotionDetector()

    @Provides
    @Singleton
    fun provideAudioAnalyzer(
        @ApplicationContext context: Context
    ): AudioAnalyzer = AudioAnalyzer(context)

    @Provides
    @Singleton
    fun provideAlertManager(
        @ApplicationContext context: Context
    ): AlertManager = AlertManager(context)

    @Provides
    @Singleton
    fun provideSignalingClient(): SignalingClient = SignalingClient()

    @Provides
    @Singleton
    fun provideStreamingManager(
        @ApplicationContext context: Context,
        signalingClient: SignalingClient
    ): StreamingManager = StreamingManager(context, signalingClient)

    @Provides
    @Singleton
    fun provideSettingsRepository(
        @ApplicationContext context: Context
    ): SettingsRepository = SettingsRepository(context)
}
