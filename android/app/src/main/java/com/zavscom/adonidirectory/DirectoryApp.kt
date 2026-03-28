package com.zavscom.adonidirectory

import android.app.Application
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.zavscom.adonidirectory.di.DirectoryAppServices
import com.zavscom.adonidirectory.work.DirectorySyncWorker
import java.util.concurrent.TimeUnit

class DirectoryApp : Application() {

    override fun onCreate() {
        super.onCreate()
        DirectoryAppServices.init(this)
        scheduleDirectorySync()
    }

    private fun scheduleDirectorySync() {
        val wm = WorkManager.getInstance(this)
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val periodic = PeriodicWorkRequestBuilder<DirectorySyncWorker>(24, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()

        wm.enqueueUniquePeriodicWork(
            "directory_sync",
            ExistingPeriodicWorkPolicy.KEEP,
            periodic,
        )

        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        if (!prefs.getBoolean(KEY_INITIAL_SYNC_ENQUEUED, false)) {
            val seed = OneTimeWorkRequestBuilder<DirectorySyncWorker>()
                .setConstraints(constraints)
                .build()
            wm.enqueueUniqueWork(
                "directory_sync_seed",
                ExistingWorkPolicy.KEEP,
                seed,
            )
            prefs.edit().putBoolean(KEY_INITIAL_SYNC_ENQUEUED, true).apply()
        }
    }

    companion object {
        private const val PREFS_NAME = "directory_app"
        private const val KEY_INITIAL_SYNC_ENQUEUED = "initial_sync_enqueued"
    }
}
