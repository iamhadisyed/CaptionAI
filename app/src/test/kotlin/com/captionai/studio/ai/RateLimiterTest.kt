package com.captionai.studio.ai

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RateLimiterTest {

    @Test
    fun blocksAfterMaxRequestsPerMinute() {
        // Drain whatever earlier tests recorded, then fill the window.
        while (RateLimiter.canMakeRequest()) RateLimiter.recordRequest()
        assertFalse(RateLimiter.canMakeRequest())
        assertTrue(RateLimiter.msUntilNextSlot() > 0)
    }
}
