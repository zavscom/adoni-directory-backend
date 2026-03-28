package com.zavscom.adonidirectory.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.zavscom.adonidirectory.data.local.entity.SyncMetaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncMetaDao {
    @Query("SELECT * FROM sync_meta WHERE id = 1 LIMIT 1")
    suspend fun getMeta(): SyncMetaEntity?

    @Query("SELECT * FROM sync_meta WHERE id = 1 LIMIT 1")
    fun observeMeta(): Flow<SyncMetaEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(meta: SyncMetaEntity)
}
