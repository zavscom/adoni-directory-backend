package com.zavscom.adonidirectory.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.zavscom.adonidirectory.data.local.dao.BusinessDao
import com.zavscom.adonidirectory.data.local.dao.SyncMetaDao
import com.zavscom.adonidirectory.data.local.entity.BusinessEntity
import com.zavscom.adonidirectory.data.local.entity.SyncMetaEntity

@Database(
    entities = [BusinessEntity::class, SyncMetaEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun businessDao(): BusinessDao
    abstract fun syncMetaDao(): SyncMetaDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "directory.db",
                ).build().also { INSTANCE = it }
            }
        }
    }
}
