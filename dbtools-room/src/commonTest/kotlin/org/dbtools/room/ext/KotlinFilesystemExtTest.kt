package org.dbtools.room.ext

import assertk.assertThat
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.files.SystemTemporaryDirectory
import kotlinx.io.writeString
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

class KotlinFilesystemExtTest {

    private val fileSystem = SystemFileSystem
    private val dir = Path(SystemTemporaryDirectory, "dbtools-room-kotlinfs-test")

    @BeforeTest
    fun setUp() {
        deleteRecursively(dir)
        fileSystem.createDirectories(dir)
    }

    @AfterTest
    fun tearDown() {
        deleteRecursively(dir)
    }

    /**
     * Regression: the db file lives in a nested directory. All sidecars (`-wal`, `-shm`, `-journal`,
     * `.lck`) must be deleted from that same directory — previously they were resolved by bare file
     * name and so were left behind.
     */
    @Test
    fun deleteDatabaseFilesRemovesSidecarsInSubdirectory() {
        val dbFile = Path(dir, "catalog-eng.db")
        val sidecars = listOf("$dbFile-journal", "$dbFile-shm", "$dbFile-wal", "$dbFile.lck").map { Path(it) }

        write(dbFile, "SQLite format 3 ")
        sidecars.forEach { sidecar -> write(sidecar, "sidecar") }

        val result = fileSystem.deleteDatabaseFiles(dbFile)

        assertThat(result).isTrue()
        assertThat(fileSystem.exists(dbFile)).isFalse()
        sidecars.forEach { sidecar -> assertThat(fileSystem.exists(sidecar)).isFalse() }
    }

    /** Deleting a database with no sidecars present must succeed rather than error. */
    @Test
    fun deleteDatabaseFilesWithNoSidecarsSucceeds() {
        val dbFile = Path(dir, "catalog-eng.db")
        write(dbFile, "SQLite format 3 ")

        val result = fileSystem.deleteDatabaseFiles(dbFile)

        assertThat(result).isTrue()
        assertThat(fileSystem.exists(dbFile)).isFalse()
    }

    /** Renaming moves the db and every sidecar into the target's directory (not the working dir). */
    @Test
    fun renameDatabaseFilesMovesSidecarsWithinDirectory() {
        val src = Path(dir, "old.db")
        val target = Path(dir, "new.db")
        write(src, "SQLite format 3 ")
        write(Path("$src-wal"), "wal")
        write(Path("$src-shm"), "shm")

        val result = fileSystem.renameDatabaseFiles(src, target)

        assertThat(result).isTrue()
        assertThat(fileSystem.exists(src)).isFalse()
        assertThat(fileSystem.exists(Path("$src-wal"))).isFalse()
        assertThat(fileSystem.exists(target)).isTrue()
        assertThat(fileSystem.exists(Path("$target-wal"))).isTrue()
        assertThat(fileSystem.exists(Path("$target-shm"))).isTrue()
    }

    private fun write(path: Path, text: String) {
        fileSystem.sink(path).buffered().use { it.writeString(text) }
    }

    private fun deleteRecursively(path: Path) {
        if (!fileSystem.exists(path)) return
        val metadata = fileSystem.metadataOrNull(path)
        if (metadata?.isDirectory == true) {
            fileSystem.list(path).forEach { child -> deleteRecursively(child) }
        }
        fileSystem.delete(path, mustExist = false)
    }
}
