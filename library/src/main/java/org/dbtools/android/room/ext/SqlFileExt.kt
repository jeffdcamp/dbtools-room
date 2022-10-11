package org.dbtools.android.room.ext

import java.io.File

fun File.parseAndExecuteSqlStatements(execSQL: (String) -> Unit) {
    var statement = ""
    forEachLine { line ->
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
