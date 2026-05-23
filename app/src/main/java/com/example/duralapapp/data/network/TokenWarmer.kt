package com.example.duralapapp.data.network

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.firstOrNull

@HiltWorker
class TokenWarmWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val tokenManager: TokenManager,
    private val tokenRefreshCoordinator: TokenRefreshCoordinator
) : CoroutineWorker(appContext, params) {
    companion object {
        private const val WARMUP_WINDOW_MS = 5 * 60 * 1000L
    }

    override suspend fun doWork(): Result {
        val refreshToken = tokenManager.refreshToken.firstOrNull()
        if (refreshToken.isNullOrBlank()) return Result.success()

        val expiresAt = tokenManager.accessTokenExpiresAt.firstOrNull() ?: return Result.success()
        val now = System.currentTimeMillis()
        if (now >= expiresAt - WARMUP_WINDOW_MS) {
            return if (tokenRefreshCoordinator.refreshIfNeeded(forceRefresh = true)) {
                Result.success()
            } else {
                Result.retry()
            }
        }
        return Result.success()
    }
}

@Singleton
class TokenWarmer @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val PERIODIC_WARMER = "token_periodic_warmer"
        private const val ONE_TIME_WARMER = "token_one_time_warmer"
        private const val PERIODIC_INTERVAL_MINUTES = 15L
        private const val PREWARM_DELAY_MS = 15_000L
        private const val BACKOFF_WARMUP_DELAY_MS = 0L
        private const val BACKOFF_DELAY_SECONDS = 10L
    }

    fun ensurePeriodicWarmup() {
        val request = PeriodicWorkRequestBuilder<TokenWarmWorker>(
            PERIODIC_INTERVAL_MINUTES,
            TimeUnit.MINUTES
        )
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BACKOFF_DELAY_SECONDS, TimeUnit.SECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_WARMER,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    fun enqueuePrewarm(delayMs: Long = PREWARM_DELAY_MS) {
        enqueueOneTimeWarmup(delayMs)
    }

    /**
     * Runs during retry backoff so a fresh token may be ready before the request is retried.
     */
    fun enqueueBackoffWarmup() {
        enqueueOneTimeWarmup(BACKOFF_WARMUP_DELAY_MS)
    }

    private fun enqueueOneTimeWarmup(delayMs: Long) {
        val request = OneTimeWorkRequestBuilder<TokenWarmWorker>()
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            ONE_TIME_WARMER,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }
}
