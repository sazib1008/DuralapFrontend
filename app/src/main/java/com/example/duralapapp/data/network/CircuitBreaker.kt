package com.example.duralapapp.data.network

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CircuitBreaker @Inject constructor() {
    private val failureCount = AtomicInteger(0)
    private val openUntilMillis = AtomicLong(0L)
    private val threshold = 3
    private val openDurationMillis = 30_000L

    fun isOpen(nowMillis: Long = System.currentTimeMillis()): Boolean {
        return nowMillis < openUntilMillis.get()
    }

    fun onSuccess() {
        failureCount.set(0)
        openUntilMillis.set(0L)
    }

    fun onFailure(nowMillis: Long = System.currentTimeMillis()) {
        val failures = failureCount.incrementAndGet()
        if (failures >= threshold) {
            openUntilMillis.set(nowMillis + openDurationMillis)
        }
    }
}
