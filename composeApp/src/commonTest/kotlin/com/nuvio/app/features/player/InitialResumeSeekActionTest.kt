package com.nuvio.app.features.player

import kotlin.test.Test
import kotlin.test.assertEquals

class InitialResumeSeekActionTest {

    @Test
    fun absoluteResume_seeksEvenWhenDurationUnknown() {
        assertEquals(
            InitialResumeSeekAction.Seek(600_000L),
            resolveInitialResumeSeekAction(
                activeInitialPositionMs = 600_000L,
                activeInitialProgressFraction = null,
                durationMs = 0L,
            ),
        )
    }

    @Test
    fun absoluteResume_clampsWhenDurationKnown() {
        assertEquals(
            InitialResumeSeekAction.Seek(100L),
            resolveInitialResumeSeekAction(
                activeInitialPositionMs = 500L,
                activeInitialProgressFraction = null,
                durationMs = 100L,
            ),
        )
    }

    @Test
    fun fractionResume_defersUntilDurationKnown() {
        assertEquals(
            InitialResumeSeekAction.DeferUntilTimelineKnown,
            resolveInitialResumeSeekAction(
                activeInitialPositionMs = 0L,
                activeInitialProgressFraction = 0.5f,
                durationMs = 0L,
            ),
        )
    }

    @Test
    fun fractionResume_convertsWhenDurationKnown() {
        assertEquals(
            InitialResumeSeekAction.Seek(300_000L),
            resolveInitialResumeSeekAction(
                activeInitialPositionMs = 0L,
                activeInitialProgressFraction = 0.5f,
                durationMs = 600_000L,
            ),
        )
    }

    @Test
    fun noResume_returnsNoSeekNeeded() {
        assertEquals(
            InitialResumeSeekAction.NoSeekNeeded,
            resolveInitialResumeSeekAction(
                activeInitialPositionMs = 0L,
                activeInitialProgressFraction = null,
                durationMs = 0L,
            ),
        )
    }
}
