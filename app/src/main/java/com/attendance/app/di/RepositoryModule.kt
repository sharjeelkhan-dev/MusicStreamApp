package com.attendance.app.di

import com.attendance.app.data.repository.AttendanceRepositoryImpl
import com.attendance.app.data.repository.AuthRepositoryImpl
import com.attendance.app.data.repository.ClassRepositoryImpl
import com.attendance.app.data.repository.FirebaseAiRepositoryImpl
import com.attendance.app.data.repository.StudentRepositoryImpl
import com.attendance.app.data.repository.SyncRepositoryImpl
import com.attendance.app.domain.repository.AttendanceRepository
import com.attendance.app.domain.repository.AuthRepository
import com.attendance.app.domain.repository.AiRepository
import com.attendance.app.domain.repository.ClassRepository
import com.attendance.app.domain.repository.StudentRepository
import com.attendance.app.domain.repository.SyncRepository
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
    abstract fun bindClassRepository(impl: ClassRepositoryImpl): ClassRepository

    @Binds
    @Singleton
    abstract fun bindStudentRepository(impl: StudentRepositoryImpl): StudentRepository

    @Binds
    @Singleton
    abstract fun bindAttendanceRepository(impl: AttendanceRepositoryImpl): AttendanceRepository

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindSyncRepository(impl: SyncRepositoryImpl): SyncRepository

    @Binds
    @Singleton
    abstract fun bindAiRepository(impl: FirebaseAiRepositoryImpl): AiRepository
}
