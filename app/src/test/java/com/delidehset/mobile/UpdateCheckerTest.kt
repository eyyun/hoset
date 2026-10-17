package com.delidehset.mobile

import com.delidehset.mobile.data.UpdateChecker
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Whether the release on GitHub is later than the build in hand.
 *
 * "Later" is compared number by number, because the obvious shortcut — comparing the
 * strings — calls 1.10.0 older than 1.9.0, and an updater that never offers the tenth
 * release of a series is worse than none.
 */
class UpdateCheckerTest {

    @Test
    fun `a later patch is newer`() {
        assertTrue(UpdateChecker.isNewer("1.0.1", "1.0.0"))
        assertTrue(UpdateChecker.isNewer("1.1.0", "1.0.9"))
        assertTrue(UpdateChecker.isNewer("2.0.0", "1.99.99"))
    }

    @Test
    fun `the same version is not newer`() {
        assertFalse(UpdateChecker.isNewer("1.0.0", "1.0.0"))
        assertFalse(UpdateChecker.isNewer("1.0.0", "1"))
    }

    @Test
    fun `parts are numbers, not text`() {
        assertTrue(UpdateChecker.isNewer("1.10.0", "1.9.0"))
        assertTrue(UpdateChecker.isNewer("1.0.10", "1.0.9"))
        assertFalse(UpdateChecker.isNewer("1.9.0", "1.10.0"))
    }

    @Test
    fun `a leading v and a pre-release suffix are tolerated`() {
        assertTrue(UpdateChecker.isNewer("v1.2.0", "1.1.0"))
        assertTrue(UpdateChecker.isNewer("1.2.0-beta1", "1.1.0"))
        assertFalse(UpdateChecker.isNewer("1.2.0-beta1", "1.2.0"))
    }

    @Test
    fun `a shorter version is not newer`() {
        assertFalse(UpdateChecker.isNewer("1.0", "1.0.1"))
        assertFalse(UpdateChecker.isNewer("", "1.0.0"))
    }
}
