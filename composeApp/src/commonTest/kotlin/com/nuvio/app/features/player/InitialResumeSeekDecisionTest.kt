package com.nuvio.app.features.player

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class InitialResumeSeekDecisionTest {

    @Test
    fun resolveTarget_absoluteUnknownDuration_returnsRawPosition() {
        assertEquals(
            600_000L,
            resolveInitialResumeTargetMs(
                activeInitialPositionMs = 600_000L,
                activeInitialProgressFraction = null,
                durationMs = 0L,
            ),
        )
    }

    @Test
    fun resolveTarget_absoluteKnownDuration_clamps() {
        assertEquals(
            100L,
            resolveInitialResumeTargetMs(
                activeInitialPositionMs = 500L,
                activeInitialProgressFraction = null,
                durationMs = 100L,
            ),
        )
    }

    @Test
    fun resolveTarget_fractionUnknownDuration_returnsNull() {
        assertEquals(
            null,
            resolveInitialResumeTargetMs(
                activeInitialPositionMs = 0L,
                activeInitialProgressFraction = 0.5f,
                durationMs = 0L,
            ),
        )
    }

    @Test
    fun evaluate_absoluteUnknownDuration_returnsSeek() {
        assertEquals(
            InitialResumeSeekDecision.Seek(600_000L),
            evaluateInitialResumeSeekDecision(
                initialSeekApplied = false,
                isLoading = false,
                activeInitialPositionMs = 600_000L,
                activeInitialProgressFraction = null,
                durationMs = 0L,
                positionMs = 0L,
                seekAttempts = 0,
                lastSeekAttemptTimeMs = null,
                nowTimeMs = 1_000L,
            ),
        )
    }

    @Test
    fun evaluate_fractionUnknownDuration_defersTimeline() {
        assertEquals(
            InitialResumeSeekDecision.DeferTimeline,
            evaluateInitialResumeSeekDecision(
                initialSeekApplied = false,
                isLoading = false,
                activeInitialPositionMs = 0L,
                activeInitialProgressFraction = 0.5f,
                durationMs = 0L,
                positionMs = 0L,
                seekAttempts = 0,
                lastSeekAttemptTimeMs = null,
                nowTimeMs = 1_000L,
            ),
        )
    }

    @Test
    fun evaluate_absoluteUnknownDuration_throttlesSecondSeekWhenPositionStillNearZero() {
        val first = evaluateInitialResumeSeekDecision(
            initialSeekApplied = false,
            isLoading = false,
            activeInitialPositionMs = 600_000L,
            activeInitialProgressFraction = null,
            durationMs = 0L,
            positionMs = 0L,
            seekAttempts = 1,
            lastSeekAttemptTimeMs = 1000L,
            nowTimeMs = 1100L,
        )
        val sleep = assertIs<InitialResumeSeekDecision.Sleep>(first)
        assertTrue(sleep.ms > 0L)
        assertTrue(sleep.ms <= InitialResumeSeekRetryThrottleMs)

        val afterThrottle = evaluateInitialResumeSeekDecision(
            initialSeekApplied = false,
            isLoading = false,
            activeInitialPositionMs = 600_000L,
            activeInitialProgressFraction = null,
            durationMs = 0L,
            positionMs = 0L,
            seekAttempts = 1,
            lastSeekAttemptTimeMs = 1000L,
            nowTimeMs = 1000L + InitialResumeSeekRetryThrottleMs + 1L,
        )
        assertIs<InitialResumeSeekDecision.Seek>(afterThrottle)
    }

    @Test
    fun evaluate_absoluteUnknownDuration_doesNotMarkAppliedViaDecision_positionMustConfirm() {
        val farFromTarget = evaluateInitialResumeSeekDecision(
            initialSeekApplied = false,
            isLoading = false,
            activeInitialPositionMs = 600_000L,
            activeInitialProgressFraction = null,
            durationMs = 0L,
            positionMs = 5_000L,
            seekAttempts = 1,
            lastSeekAttemptTimeMs = null,
            nowTimeMs = 10_000L,
        )
        assertIs<InitialResumeSeekDecision.Seek>(farFromTarget)

        val closeEnough = evaluateInitialResumeSeekDecision(
            initialSeekApplied = false,
            isLoading = false,
            activeInitialPositionMs = 600_000L,
            activeInitialProgressFraction = null,
            durationMs = 0L,
            positionMs = 599_500L,
            seekAttempts = 2,
            lastSeekAttemptTimeMs = 9_000L,
            nowTimeMs = 10_000L,
        )
        assertEquals(InitialResumeSeekDecision.ConfirmComplete, closeEnough)
    }

    @Test
    fun evaluate_knownDuration_finalClampSeek() {
        assertEquals(
            InitialResumeSeekDecision.Seek(100L),
            evaluateInitialResumeSeekDecision(
                initialSeekApplied = false,
                isLoading = false,
                activeInitialPositionMs = 500L,
                activeInitialProgressFraction = null,
                durationMs = 100L,
                positionMs = 0L,
                seekAttempts = 0,
                lastSeekAttemptTimeMs = null,
                nowTimeMs = 1_000L,
            ),
        )
    }
}
