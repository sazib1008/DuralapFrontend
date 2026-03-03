package com.example.duralab.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.duralab.data.local.dao.ChatDao
import com.example.duralab.data.local.dao.MessageDao
import com.example.duralab.data.local.dao.UserDao
import com.example.duralab.data.local.entity.ChatEntity
import com.example.duralab.data.local.entity.MessageEntity
import com.example.duralab.data.local.entity.UserEntity

@Database(
    entities = [MessageEntity::class, UserEntity::class, ChatEntity::class],
    version = 1,
    exportSchema = false
)
abstract class DuralabDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatDao
    abstract fun messageDao(): MessageDao
    abstract fun userDao(): UserDao
}
