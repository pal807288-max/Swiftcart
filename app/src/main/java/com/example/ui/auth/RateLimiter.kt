package com.example.ui.auth

import java.util.concurrent.ConcurrentHashMap

class RateLimiter(
    val maxAttempts: Int = 5,
    val windowMs: Long = 30000L,
    val cooldownMs: Long = 30000L
) {
    private val attemptHistory = ConcurrentHashMap<String, MutableList<Long>>()
    private val cooldownEndMap = ConcurrentHashMap<String, Long>()

    /**
     * Tries to record an attempt for the given key.
     * Returns a [RateLimitResult].
     */
    @Synchronized
    fun checkLimit(key: String): RateLimitResult {
        val now = System.currentTimeMillis()

        // 1. Check if key is currently in cooldown
        val cooldownEnd = cooldownEndMap[key] ?: 0L
        if (now < cooldownEnd) {
            val remainingSeconds = ((cooldownEnd - now) + 999) / 1000
            return RateLimitResult.Blocked(remainingSeconds)
        }

        // 2. Clean up old attempts and check window
        val history = attemptHistory.getOrPut(key) { mutableListOf() }
        history.removeAll { it < now - windowMs }

        // 3. Check if number of attempts exceeds maxAttempts
        if (history.size >= maxAttempts) {
            val endCooldown = now + cooldownMs
            cooldownEndMap[key] = endCooldown
            val remainingSeconds = (cooldownMs + 999) / 1000
            return RateLimitResult.Blocked(remainingSeconds)
        }

        // 4. Record the attempt
        history.add(now)
        val remainingAttempts = maxAttempts - history.size
        return RateLimitResult.Allowed(remainingAttempts)
    }

    /**
     * Resets/clears history for a key (e.g. on successful login).
     */
    @Synchronized
    fun reset(key: String) {
        attemptHistory.remove(key)
        cooldownEndMap.remove(key)
    }
}

sealed interface RateLimitResult {
    data class Allowed(val remainingAttempts: Int) : RateLimitResult
    data class Blocked(val remainingSeconds: Long) : RateLimitResult
}
