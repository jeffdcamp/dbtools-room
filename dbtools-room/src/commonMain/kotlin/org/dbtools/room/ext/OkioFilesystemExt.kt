@file:Suppress("DuplicatedCode", "unused")

package org.dbtools.room.ext

import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.buffer
import okio.use

/**
 * Deletes a database including its journal file and other auxiliary files
 * that may have been created by the database engine.
 *
 * @param file The database file path.
 * @return true if the database was successfully deleted.
 */
fun FileSystem.deleteDatabaseFiles(file: Path): Boolean {
    delete(file)
    delete("${file.name}-journal".toPath())
    delete("${file.name}-shm".toPath())
    delete("${file.name}-wal".toPath())
    delete("${file.name}.lck".toPath())

    return !exists(file)
}

/**
 * Renames a database including its journal file and other auxiliary files
 * that may have been created by the database engine.
 *
 * @param srcFile The database file path.
 * @param targetFile New name of the database.
 * @return true if the database was successfully rename.
 */
fun FileSystem.renameDatabaseFiles(srcFile: Path, targetFile: Path): Boolean {
    atomicMove(srcFile, targetFile)
    atomicMoveIfExists("${srcFile.name}-journal".toPath(), "${targetFile.name}-journal".toPath())
    atomicMoveIfExists("${srcFile.name}-shm".toPath(), "${targetFile.name}-shm".toPath())
    atomicMoveIfExists("${srcFile.name}-wal".toPath(), "${targetFile.name}-wal".toPath())
    atomicMoveIfExists("${srcFile.name}.lck".toPath(), "${targetFile.name}.lck".toPath())

    return exists(targetFile) && !exists(srcFile)
}

private fun FileSystem.atomicMoveIfExists(source: Path, target: Path) {
    if (exists(source)) {
        atomicMove(source, target)
    }
}

@Suppress("NestedBlockDepth")
fun FileSystem.parseAndExecuteSqlStatements(path: Path, execSQL: (String) -> Unit) {
    var statement = ""
    source(path).use { fileSource ->
        fileSource.buffer().use { bufferedFileSource ->
            while (true) {
                val line = bufferedFileSource.readUtf8Line() ?: break

                // Prepare the line
                // - Remove any trailing comments (sqldiff may add comments to a line (Example: 'DROP TABLE speaker; -- due to schema mismatch'))
                // - Remove any trailing spaces (we check for a line ending with ';')

                // check for sqldiff comment
                val formattedLine = if (line.contains("; -- ")) {
                    // line without comment
                    line.substringBefore("; -- ") + ";" // be sure to add the ; back in
                } else {
                    line
                }

                statement += formattedLine
                if (statement.trim().endsWith(';')) {
                    execSQL(statement)
                    statement = ""
                } else {
                    // If the statement currently does not end with [;] then there must be multiple lines to the full statement.
                    // Make sure to keep the newline character (some text columns may have multiple lines of data)
                    statement += '\n'
                }
            }
        }
    }
}
