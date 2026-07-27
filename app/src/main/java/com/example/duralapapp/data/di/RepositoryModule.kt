package com.example.duralapapp.data.di

import com.example.duralapapp.data.repository.AuthRepository
import com.example.duralapapp.data.repository.AuthRepositoryImpl
import com.example.duralapapp.data.repository.ChatRepository
import com.example.duralapapp.data.repository.ChatRepositoryImpl
import com.example.duralapapp.data.repository.ConversationRequestRepository
import com.example.duralapapp.data.repository.ConversationRequestRepositoryImpl
import com.example.duralapapp.data.repository.CallRepository
import com.example.duralapapp.data.repository.CallRepositoryImpl
import com.example.duralapapp.data.repository.SearchRepository
import com.example.duralapapp.data.repository.SearchRepositoryImpl
import com.example.duralapapp.data.repository.UserRepository
import com.example.duralapapp.data.repository.UserRepositoryImpl
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
    abstract fun bindAuthRepository(
        impl: AuthRepositoryImpl
    ): AuthRepository

    @Binds
    @Singleton
    abstract fun bindSearchRepository(
        impl: SearchRepositoryImpl
    ): SearchRepository

    @Binds
    @Singleton
    abstract fun bindConversationRequestRepository(
        impl: ConversationRequestRepositoryImpl
    ): ConversationRequestRepository

    @Binds
    @Singleton
    abstract fun bindChatRepository(
        impl: ChatRepositoryImpl
    ): ChatRepository

    @Binds
    @Singleton
    abstract fun bindUserRepository(
        impl: UserRepositoryImpl
    ): UserRepository

    @Binds
    @Singleton
    abstract fun bindCallRepository(
        impl: CallRepositoryImpl
    ): CallRepository
}
