package org.schabi.newpipe.backup

import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Covers test case C2-3 of docs/test-plan.html. Deleting the wrong file here is the one
 * mistake a backup system must never make, so the newest copy is checked explicitly.
 */
class BackupRetentionTest {
    private fun backupsFor(days: Int) = (1..days).map {
        BackupNames.of(LocalDateTime.of(2026, 8, 1, 3, 0).plusDays(it.toLong()))
    }

    @Test fun `a name carries the moment it was taken`() {
        val name = BackupNames.of(LocalDateTime.of(2026, 8, 28, 21, 30))

        assertEquals("CAI-PP-backup-20260828-2130.zip", name)
        assertEquals(LocalDateTime.of(2026, 8, 28, 21, 30), BackupNames.takenAt(name))
    }

    @Test fun `a file that is not ours is not read as a backup`() {
        assertNull(BackupNames.takenAt("holiday-photos.zip"))
        assertNull(BackupNames.takenAt("CAI-PP-backup-not-a-date.zip"))
        assertNull(BackupNames.takenAt(null))
    }

    @Test fun `under a week of backups nothing is deleted`() {
        assertTrue(BackupRetention.expired(backupsFor(7)).isEmpty())
        assertTrue(BackupRetention.expired(backupsFor(1)).isEmpty())
        assertTrue(BackupRetention.expired(emptyList()).isEmpty())
    }

    @Test fun `past a week the oldest go and seven remain`() {
        val ten = backupsFor(10)

        val expired = BackupRetention.expired(ten)

        assertEquals(3, expired.size)
        assertEquals(ten.take(3).toSet(), expired.toSet())
    }

    @Test fun `the newest backup is never deleted`() {
        val many = backupsFor(30)

        val expired = BackupRetention.expired(many)

        assertTrue("the newest copy was scheduled for deletion", many.last() !in expired)
        assertEquals(7, many.size - expired.size)
    }

    @Test fun `other people's files in the folder are left alone`() {
        val mixed = backupsFor(10) + listOf("tax-return.pdf", "song.mp3")

        val expired = BackupRetention.expired(mixed)

        assertTrue(expired.none { it == "tax-return.pdf" || it == "song.mp3" })
        assertEquals(3, expired.size)
    }

    @Test fun `an unsorted listing is still handled oldest first`() {
        val shuffled = backupsFor(10).reversed()

        val expired = BackupRetention.expired(shuffled)

        assertEquals(backupsFor(10).take(3).toSet(), expired.toSet())
    }
}
