package com.zavscom.adonidirectory.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.zavscom.adonidirectory.data.local.entity.BusinessEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BusinessDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(businesses: List<BusinessEntity>)

    @Query("DELETE FROM businesses")
    suspend fun clearAll()

    @Query("SELECT * FROM businesses ORDER BY name")
    fun getAllFlow(): Flow<List<BusinessEntity>>

    @Query("SELECT * FROM businesses WHERE category = :category ORDER BY name")
    fun getByCategoryFlow(category: String): Flow<List<BusinessEntity>>

    @Query(
        """
        SELECT * FROM businesses
        WHERE name LIKE '%' || :query || '%' OR area LIKE '%' || :query || '%'
        ORDER BY name
        """,
    )
    fun searchFlow(query: String): Flow<List<BusinessEntity>>

    @Query("SELECT * FROM businesses WHERE id = :id LIMIT 1")
    fun observeById(id: String): Flow<BusinessEntity?>

    @Query(
        """
        SELECT DISTINCT category FROM businesses
        WHERE TRIM(IFNULL(category, '')) != ''
        ORDER BY category COLLATE NOCASE ASC
        """,
    )
    fun observeDistinctCategories(): Flow<List<String>>
}
