package com.example

import com.example.ui.auth.RateLimitResult
import com.example.ui.auth.RateLimiter
import org.junit.Assert.*
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun rateLimiter_allows_up_to_max_attempts() {
    val limiter = RateLimiter(maxAttempts = 3, windowMs = 5000, cooldownMs = 5000)
    
    // 1st attempt: Allowed
    val res1 = limiter.checkLimit("user1")
    assertTrue(res1 is RateLimitResult.Allowed)
    assertEquals(2, (res1 as RateLimitResult.Allowed).remainingAttempts)

    // 2nd attempt: Allowed
    val res2 = limiter.checkLimit("user1")
    assertTrue(res2 is RateLimitResult.Allowed)
    assertEquals(1, (res2 as RateLimitResult.Allowed).remainingAttempts)

    // 3rd attempt: Allowed
    val res3 = limiter.checkLimit("user1")
    assertTrue(res3 is RateLimitResult.Allowed)
    assertEquals(0, (res3 as RateLimitResult.Allowed).remainingAttempts)

    // 4th attempt: Blocked
    val res4 = limiter.checkLimit("user1")
    assertTrue(res4 is RateLimitResult.Blocked)
    assertTrue((res4 as RateLimitResult.Blocked).remainingSeconds > 0)
  }

  @Test
  fun rateLimiter_reset_clears_cooldown_and_history() {
    val limiter = RateLimiter(maxAttempts = 2, windowMs = 5000, cooldownMs = 5000)
    
    limiter.checkLimit("user2")
    limiter.checkLimit("user2")
    
    // Should be blocked now
    val blockedRes = limiter.checkLimit("user2")
    assertTrue(blockedRes is RateLimitResult.Blocked)
    
    // Reset key
    limiter.reset("user2")
    
    // Should be allowed again
    val allowedRes = limiter.checkLimit("user2")
    assertTrue(allowedRes is RateLimitResult.Allowed)
    assertEquals(1, (allowedRes as RateLimitResult.Allowed).remainingAttempts)
  }
}
