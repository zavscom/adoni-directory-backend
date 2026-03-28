package com.zavscom.adonidirectory.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.zavscom.adonidirectory.di.DirectoryAppServices
import com.zavscom.adonidirectory.sync.FULL_URL
import kotlinx.serialization.SerializationException
import java.io.IOException

class DirectorySyncWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val repository = DirectoryAppServices.repository
        return try {
            repository.syncFromRemote(FULL_URL)
            Result.success()
        } catch (_: IOException) {
            Result.retry()
        } catch (_: SerializationException) {
            Result.retry()
        }
    }
}
