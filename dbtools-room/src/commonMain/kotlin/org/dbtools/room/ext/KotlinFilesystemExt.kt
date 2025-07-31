@file:Suppress("DuplicatedCode", "unused")

package org.dbtools.room.ext

import kotlinx.io.buffered
import kotlinx.io.files.FileSystem
import kotlinx.io.files.Path
import kotlinx.io.readLine

/**
 * Deletes a database including its journal file and other auxiliary files
 * that may have been created by the database engine.
 *
 * @param file The database file path.
 * @return true if the database was successfully deleted.
 */
fun FileSystem.deleteDatabaseFiles(file: Path): Boolean {
    delete(file)
    delete(Path("${file.name}-journal"))
    delete(Path("${file.name}-shm"))
    delete(Path("${file.name}-wal"))
    delete(Path("${file.name}.lck"))

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
    atomicMoveIfExists(Path("${srcFile.name}-journal"),Path( "${targetFile.name}-journal"))
    atomicMoveIfExists(Path("${srcFile.name}-shm"),Path( "${targetFile.name}-shm"))
    atomicMoveIfExists(Path("${srcFile.name}-wal"),Path( "${targetFile.name}-wal"))
    atomicMoveIfExists(Path("${srcFile.name}.lck"),Path( "${targetFile.name}.lck"))

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
        fileSource.buffered().use { bufferedFileSource ->
            while (true) {
                val line = bufferedFileSource.readLine() ?: break

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
