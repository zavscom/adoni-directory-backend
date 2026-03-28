package com.zavscom.adonidirectory.di

import android.content.Context
import com.zavscom.adonidirectory.data.local.db.AppDatabase
import com.zavscom.adonidirectory.data.repository.BusinessRepository
import okhttp3.OkHttpClient

/**
 * Minimal process-wide access for [BusinessRepository] (Workers have no Hilt here).
 * Call [init] once from [android.app.Application.onCreate] before any worker runs.
 */
object DirectoryAppServices {
    private lateinit var appContext: Context

    fun init(context: Context) {
        if (::appContext.isInitialized) return
        appContext = context.applicationContext
    }

    private val database by lazy { AppDatabase.getInstance(appContext) }

    private val httpClient by lazy { OkHttpClient() }

    val repository: BusinessRepository by lazy {
        BusinessRepository(
            database = database,
            businessDao = database.businessDao(),
            syncMetaDao = database.syncMetaDao(),
            httpClient = httpClient,
        )
    }
}
