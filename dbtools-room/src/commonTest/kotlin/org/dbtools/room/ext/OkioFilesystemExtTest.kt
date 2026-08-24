package org.dbtools.room.ext

import assertk.assertThat
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import kotlin.test.Test

class OkioFilesystemExtTest {

    /**
     * Regression: the db file lives in a nested directory. All sidecars (`-wal`, `-shm`, `-journal`,
     * `.lck`) must be deleted from that same directory — previously they were resolved by bare file
     * name and so were left behind.
     */
    @Test
    fun deleteDatabaseFilesRemovesSidecarsInSubdirectory() {
        val fileSystem = FakeFileSystem()
        val dir = "/data/db".toPath()
        fileSystem.createDirectories(dir)
        val dbFile = dir / "catalog-eng.db"
        val sidecars = listOf("$dbFile-journal", "$dbFile-shm", "$dbFile-wal", "$dbFile.lck").map { it.toPath() }

        fileSystem.write(dbFile) { writeUtf8("SQLite format 3 ") }
        sidecars.forEach { sidecar -> fileSystem.write(sidecar) { writeUtf8("sidecar") } }

        val result = fileSystem.deleteDatabaseFiles(dbFile)

        assertThat(result).isTrue()
        assertThat(fileSystem.exists(dbFile)).isFalse()
        sidecars.forEach { sidecar -> assertThat(fileSystem.exists(sidecar)).isFalse() }
    }

    /** Deleting a database with no sidecars present must succeed rather than error. */
    @Test
    fun deleteDatabaseFilesWithNoSidecarsSucceeds() {
        val fileSystem = FakeFileSystem()
        val dir = "/data/db".toPath()
        fileSystem.createDirectories(dir)
        val dbFile = dir / "catalog-eng.db"
        fileSystem.write(dbFile) { writeUtf8("SQLite format 3 ") }

        val result = fileSystem.deleteDatabaseFiles(dbFile)

        assertThat(result).isTrue()
        assertThat(fileSystem.exists(dbFile)).isFalse()
    }

    /** Renaming moves the db and every sidecar into the target's directory (not the working dir). */
    @Test
    fun renameDatabaseFilesMovesSidecarsWithinDirectory() {
        val fileSystem = FakeFileSystem()
        val dir = "/data/db".toPath()
        fileSystem.createDirectories(dir)
        val src = dir / "old.db"
        val target = dir / "new.db"
        fileSystem.write(src) { writeUtf8("SQLite format 3 ") }
        fileSystem.write("$src-wal".toPath()) { writeUtf8("wal") }
        fileSystem.write("$src-shm".toPath()) { writeUtf8("shm") }

        val result = fileSystem.renameDatabaseFiles(src, target)

        assertThat(result).isTrue()
        assertThat(fileSystem.exists(src)).isFalse()
        assertThat(fileSystem.exists("$src-wal".toPath())).isFalse()
        assertThat(fileSystem.exists(target)).isTrue()
        assertThat(fileSystem.exists("$target-wal".toPath())).isTrue()
        assertThat(fileSystem.exists("$target-shm".toPath())).isTrue()
    }
}
