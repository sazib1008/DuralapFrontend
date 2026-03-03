package com.example.duralab.di

import android.content.Context
import androidx.room.Room
import com.example.duralab.data.local.DuralabDatabase
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
    fun provideDatabase(@ApplicationContext context: Context): DuralabDatabase {
        return Room.databaseBuilder(
            context,
            DuralabDatabase::class.java,
            "duralab_db"
        ).build()
    }

    @Provides
    fun provideChatDao(database: DuralabDatabase) = database.chatDao()

    @Provides
    fun provideMessageDao(database: DuralabDatabase) = database.messageDao()

    @Provides
    fun provideUserDao(database: DuralabDatabase) = database.userDao()
}
