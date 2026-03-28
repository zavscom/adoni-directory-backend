package com.zavscom.adonidirectory.data.repository

import androidx.room.withTransaction
import com.zavscom.adonidirectory.data.local.dao.BusinessDao
import com.zavscom.adonidirectory.data.local.dao.SyncMetaDao
import com.zavscom.adonidirectory.data.local.db.AppDatabase
import com.zavscom.adonidirectory.data.local.entity.BusinessEntity
import com.zavscom.adonidirectory.data.local.entity.SyncMetaEntity
import com.zavscom.adonidirectory.data.remote.BusinessJson
import com.zavscom.adonidirectory.data.remote.FullSnapshotJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import okhttp3.CacheControl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.time.Instant

/**
 * Offline cache + remote sync. [database] is required so [syncFromRemote] can run
 * [clearAll], [upsertAll], and sync metadata in a single Room transaction.
 *
 * Note: full sync clears all rows then re-inserts from the snapshot, so [BusinessEntity.isFavorite]
 * is reset unless you extend this flow to merge local favorites by id.
 */
class BusinessRepository(
    private val database: AppDatabase,
    private val businessDao: BusinessDao,
    private val syncMetaDao: SyncMetaDao,
    private val httpClient: OkHttpClient,
    private val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    },
) {
    fun getAllBusinesses(): Flow<List<BusinessEntity>> = businessDao.getAllFlow()

    fun getBusinessesByCategory(category: String): Flow<List<BusinessEntity>> =
        businessDao.getByCategoryFlow(category)

    fun searchBusinesses(query: String): Flow<List<BusinessEntity>> =
        businessDao.searchFlow(query)

    fun observeDistinctCategories(): Flow<List<String>> =
        businessDao.observeDistinctCategories()

    fun observeLastSyncAt(): Flow<String?> =
        syncMetaDao.observeMeta().map { it?.lastSyncAt }

    fun observeBusinessById(id: String): Flow<BusinessEntity?> =
        businessDao.observeById(id)

    /** Alias for callers that prefer the `get*` naming; same as [observeBusinessById]. */
    fun getBusinessById(id: String): Flow<BusinessEntity?> = observeBusinessById(id)

    suspend fun syncFromRemote(fullUrl: String) = withContext(Dispatchers.IO) {
        val body = downloadBody(fullUrl)
        val snapshot = json.decodeFromString(FullSnapshotJson.serializer(), body)
        val entities = snapshot.businesses.map { it.toEntity(json) }
        val now = Instant.now().toString()

        database.withTransaction {
            businessDao.clearAll()
            businessDao.upsertAll(entities)
            syncMetaDao.upsert(SyncMetaEntity(id = 1, lastSyncAt = now))
        }
    }

    private fun downloadBody(fullUrl: String): String {
        // Avoid stale CDN/browser-style caching of GitHub Pages JSON.
        val request = Request.Builder()
            .url(fullUrl)
            .cacheControl(CacheControl.FORCE_NETWORK)
            .header("Cache-Control", "no-cache, no-store")
            .header("Pragma", "no-cache")
            .build()
        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("HTTP ${response.code} for $fullUrl")
            }
            return response.body?.string() ?: throw IOException("Empty body for $fullUrl")
        }
    }

    private fun BusinessJson.toEntity(jsonFormat: Json): BusinessEntity =
        BusinessEntity(
            id = id,
            name = name,
            category = category,
            subCategory = subCategory,
            address = address,
            area = area,
            pincode = pincode,
            city = city,
            state = state,
            phone = phone,
            whatsapp = whatsapp,
            email = email,
            website = website,
            latitude = latitude,
            longitude = longitude,
            source = source,
            lastSeenAt = lastSeenAt,
            extraJson = extra?.let { jsonFormat.encodeToString(JsonObject.serializer(), it) },
            isFavorite = false,
        )
}
