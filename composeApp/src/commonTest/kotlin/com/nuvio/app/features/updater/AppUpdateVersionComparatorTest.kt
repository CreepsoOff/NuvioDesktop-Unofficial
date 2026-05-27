package com.nuvio.app.features.updater

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppUpdateVersionComparatorTest {
    @Test
    fun parsesVersionAndBuildFromReleaseNotesHeading() {
        val version = AppUpdateVersionComparator.parseReleaseVersion(
            tag = "pre",
            title = "Pre-Release - Betas are stored in this release",
            notes = "# Nuvio Desktop 0.1.14 build 55 - Pre-release",
        )

        assertEquals("0.1.14", version.versionName)
        assertEquals(55, version.versionCode)
    }

    @Test
    fun parsesExplicitVersionAndBuildLinesFromReleaseNotes() {
        val version = AppUpdateVersionComparator.parseReleaseVersion(
            tag = "pre",
            title = "Pre-Release - Betas are stored in this release",
            notes = """
                version=0.1.15
                build=56
            """.trimIndent(),
        )

        assertEquals("0.1.15", version.versionName)
        assertEquals(56, version.versionCode)
    }

    @Test
    fun greaterVersionAndGreaterBuildCountsAsAvailable() {
        assertTrue(
            AppUpdateVersionComparator.isUpdateAvailable(
                remoteVersionName = "0.1.15",
                remoteVersionCode = 56,
                remoteTag = "pre",
                localVersionName = "0.1.14",
                localVersionCode = 55,
            ),
        )
    }

    @Test
    fun sameVersionAndEqualRemoteBuildDoesNotCountAsAvailable() {
        assertFalse(
            AppUpdateVersionComparator.isUpdateAvailable(
                remoteVersionName = "0.1.14",
                remoteVersionCode = 55,
                remoteTag = "pre",
                localVersionName = "0.1.14",
                localVersionCode = 55,
            ),
        )
    }

    @Test
    fun sameVersionAndGreaterRemoteBuildCountsAsAvailable() {
        assertTrue(
            AppUpdateVersionComparator.isUpdateAvailable(
                remoteVersionName = "0.1.14",
                remoteVersionCode = 56,
                remoteTag = "pre",
                localVersionName = "0.1.14",
                localVersionCode = 55,
            ),
        )
    }

    @Test
    fun sameVersionAndOlderRemoteBuildDoesNotCountAsAvailable() {
        assertFalse(
            AppUpdateVersionComparator.isUpdateAvailable(
                remoteVersionName = "0.1.14",
                remoteVersionCode = 54,
                remoteTag = "pre",
                localVersionName = "0.1.14",
                localVersionCode = 55,
            ),
        )
    }

    @Test
    fun unparseableRemoteTagWithoutVersionMetadataDoesNotCountAsAvailable() {
        assertFalse(
            AppUpdateVersionComparator.isUpdateAvailable(
                remoteVersionName = null,
                remoteVersionCode = null,
                remoteTag = "pre",
                localVersionName = "0.1.15",
                localVersionCode = 56,
            ),
        )
    }

    @Test
    fun unparseableRemoteTagUsesVersionCodeWhenProvided() {
        assertTrue(
            AppUpdateVersionComparator.isUpdateAvailable(
                remoteVersionName = null,
                remoteVersionCode = 57,
                remoteTag = "pre",
                localVersionName = "0.1.15",
                localVersionCode = 56,
            ),
        )

        assertFalse(
            AppUpdateVersionComparator.isUpdateAvailable(
                remoteVersionName = null,
                remoteVersionCode = 55,
                remoteTag = "pre",
                localVersionName = "0.1.15",
                localVersionCode = 56,
            ),
        )
    }
}
