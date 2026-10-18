package com.delidehset.mobile

import com.delidehset.mobile.data.UpdateChecker
import com.delidehset.mobile.data.UpdateDecision
import com.delidehset.mobile.data.decideUpdate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Two update bugs, pinned.
 *
 * A check that could not be made was written down as a check that succeeded, so the app
 * stopped asking for a day — which looks exactly like believing it is current. And a
 * version the person had put off stayed skipped even when they asked about it themselves,
 * so the answer to "check now" was "you are on the latest version" while a newer release
 * existed.
 */
class UpdateDecisionTest {

    @Test
    fun `a failed check is never up to date`() {
        assertEquals(UpdateDecision.NONE, decideUpdate(null, "1.2.1", auto = true, skippedVersion = null))
        assertEquals(UpdateDecision.FAILED, decideUpdate(null, "1.2.1", auto = false, skippedVersion = null))
    }

    @Test
    fun `a newer release is offered`() {
        assertEquals(
            UpdateDecision.AVAILABLE,
            decideUpdate("1.3.0", "1.2.1", auto = true, skippedVersion = null),
        )
    }

    @Test
    fun `later silences the automatic check, not the person`() {
        assertEquals(
            "otomatik denetim sessiz kalmalı",
            UpdateDecision.NONE,
            decideUpdate("1.3.0", "1.2.1", auto = true, skippedVersion = "1.3.0"),
        )
        assertEquals(
            "elle denetim yine göstermeli",
            UpdateDecision.AVAILABLE,
            decideUpdate("1.3.0", "1.2.1", auto = false, skippedVersion = "1.3.0"),
        )
    }

    @Test
    fun `same version is up to date only when asked`() {
        assertEquals(UpdateDecision.NONE, decideUpdate("1.2.1", "1.2.1", auto = true, skippedVersion = null))
        assertEquals(UpdateDecision.UP_TO_DATE, decideUpdate("1.2.1", "1.2.1", auto = false, skippedVersion = null))
    }

    @Test
    fun `an older tag never counts as newer`() {
        assertEquals(
            UpdateDecision.UP_TO_DATE,
            decideUpdate("1.1.0", "1.2.1", auto = false, skippedVersion = null),
        )
    }

    @Test
    fun `versions compare as numbers, not as text`() {
        assertTrue(UpdateChecker.isNewer("1.10.0", "1.9.0"))
        assertTrue(UpdateChecker.isNewer("2.0", "1.99.99"))
        assertFalse("aynı sürüm daha yeni değil", UpdateChecker.isNewer("1.2.1", "1.2.1"))
        assertFalse(UpdateChecker.isNewer("v", "1.2.1"))
    }
}
