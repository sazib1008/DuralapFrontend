package com.example.duralapapp.data.network

import kotlin.math.pow
import kotlin.random.Random
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RetryInterceptor @Inject constructor(
    private val circuitBreaker: CircuitBreaker,
    private val tokenWarmer: TokenWarmer,
    private val networkMonitor: NetworkMonitor
) : Interceptor {
    companion object {
        private const val MAX_RETRIES = 3
        private const val MAX_JITTER_MS = 250L
        private const val CHECK_INTERVAL_MS = 200L
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        if (!networkMonitor.isConnected()) {
            throw OfflineModeException()
        }
        if (circuitBreaker.isOpen()) {
            throw OfflineModeException()
        }

        var attempt = 0
        var lastException: Exception? = null
        var response: Response? = null

        while (attempt < MAX_RETRIES) {
            ensureCallActive(chain)
            try {
                response = chain.proceed(chain.request())
                when {
                    response.isSuccessful -> {
                        circuitBreaker.onSuccess()
                        tokenWarmer.enqueuePrewarm()
                        return response
                    }

                    response.code == 429 -> {
                        val retryAfterSeconds = response.header("Retry-After")?.toLongOrNull()
                        response.close()
                        if (retryAfterSeconds != null) {
                            waitWithCallAwareness(chain, retryAfterSeconds * 1000L)
                        } else {
                            waitWithCallAwareness(chain, backoffDelay(attempt))
                        }
                    }

                    response.code >= 500 -> {
                        if (attempt < MAX_RETRIES - 1) {
                            response.close()
                            waitWithCallAwareness(chain, backoffDelay(attempt))
                        } else {
                            return response
                        }
                    }

                    else -> return response
                }
            } catch (e: OperationHaltedException) {
                throw e
            } catch (e: OfflineModeException) {
                throw e
            } catch (e: SessionExpiredException) {
                throw e
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                throw OperationHaltedException("Retry interrupted; halting operation")
            } catch (e: Exception) {
                lastException = e
                waitWithCallAwareness(chain, backoffDelay(attempt))
            }
            attempt++
        }

        response?.let {
            circuitBreaker.onFailure()
            return it
        }
        circuitBreaker.onFailure()
        throw (lastException ?: OfflineModeException())
    }

    private fun backoffDelay(attempt: Int): Long {
        val base = (2.0.pow(attempt.toDouble()) * 1000L).toLong() // 1s, 2s, 4s
        val jitter = Random.nextLong(0L, MAX_JITTER_MS)
        return base + jitter
    }

    private fun ensureCallActive(chain: Interceptor.Chain) {
        if (chain.call().isCanceled()) {
            throw OperationHaltedException("Call canceled; halting retries to prevent leaks")
        }
    }

    private fun waitWithCallAwareness(chain: Interceptor.Chain, totalDelayMs: Long) {
        tokenWarmer.enqueueBackoffWarmup()
        var remaining = totalDelayMs
        while (remaining > 0L) {
            ensureCallActive(chain)
            val sleepMs = minOf(remaining, CHECK_INTERVAL_MS)
            Thread.sleep(sleepMs)
            remaining -= sleepMs
        }
    }
}
