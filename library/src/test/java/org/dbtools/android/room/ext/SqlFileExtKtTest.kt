package org.dbtools.android.room.ext

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.jupiter.api.Test
import java.io.File

class SqlFileExtKtTest {

    @Test
    fun testSimpleStatement() {
        val fileContent = "SELECT * FROM Person;"

        val statements = parseStatements(fileContent)

        assertThat(statements.size).isEqualTo(1)
        assertThat(statements[0]).isEqualTo("SELECT * FROM Person;")
    }

    @Test
    fun testMultiLineStatements() {
        // 2 statements
        val statements1 = parseStatements("SELECT * FROM Person WHERE name = 'Jeff';\nSELECT * FROM Person WHERE name = 'Ty';")
        assertThat(statements1.size).isEqualTo(2)
        assertThat(statements1[0]).isEqualTo("SELECT * FROM Person WHERE name = 'Jeff';")
        assertThat(statements1[1]).isEqualTo("SELECT * FROM Person WHERE name = 'Ty';")

        // 3 statements
        val statements2 = parseStatements("SELECT * FROM Person WHERE name = 'Jeff';\nSELECT * FROM Person WHERE name = 'Ty';\nSELECT * FROM Person WHERE name = 'Allie';")
        assertThat(statements2.size).isEqualTo(3)
        assertThat(statements2[0]).isEqualTo("SELECT * FROM Person WHERE name = 'Jeff';")
        assertThat(statements2[1]).isEqualTo("SELECT * FROM Person WHERE name = 'Ty';")
        assertThat(statements2[2]).isEqualTo("SELECT * FROM Person WHERE name = 'Allie';")

        // 3 statements with extra lines separating them
        val statements3 = parseStatements("SELECT * FROM Person WHERE name = 'Jeff';\n\n\nSELECT * FROM Person WHERE name = 'Ty';\n\n\n\nSELECT * FROM Person WHERE name = 'Allie';")
        assertThat(statements3.size).isEqualTo(3)
        assertThat(statements3[0]).isEqualTo("SELECT * FROM Person WHERE name = 'Jeff';")
        assertThat(statements3[1]).isEqualTo("\n\nSELECT * FROM Person WHERE name = 'Ty';")
        assertThat(statements3[2]).isEqualTo("\n\n\nSELECT * FROM Person WHERE name = 'Allie';")
    }

    @Test
    fun testLeadingLineStatements() {
        // five spaces
        val statements1 = parseStatements("     SELECT * FROM Person WHERE name = 'Jeff';")
        assertThat(statements1.size).isEqualTo(1)
        assertThat(statements1[0]).isEqualTo("     SELECT * FROM Person WHERE name = 'Jeff';")

        // indents in text
        val statements2 = parseStatements("UPDATE document SET html='<!DOCTYPE html>\n\t<body>\n\t\t<header>\n\t\t\tHelloWorld\n\t\t</header>\n\t</body>\n</html>';")
        assertThat(statements2.size).isEqualTo(1)
        assertThat(statements2[0]).isEqualTo("UPDATE document SET html='<!DOCTYPE html>\n\t<body>\n\t\t<header>\n\t\t\tHelloWorld\n\t\t</header>\n\t</body>\n</html>';")
    }

    @Test
    fun testEmptyStatement() {
        assertThat(parseStatements("").size).isEqualTo(0)
        assertThat(parseStatements("    ").size).isEqualTo(0)
    }

    @Test
    fun testComments() {
        assertThat(parseStatements("-- Some comment").size).isEqualTo(0)
        assertThat(parseStatements("--                    Some comment").size).isEqualTo(0)
        assertThat(parseStatements("               --                    Some comment").size).isEqualTo(0)

        // Simple comment after 1 line
        val statements1 = parseStatements("SELECT * FROM Person; -- Some comment")
        assertThat(statements1.size).isEqualTo(1)
        assertThat(statements1.first()).isEqualTo("SELECT * FROM Person;")

        // 2 line sql with comment after line 2 of statement
        val statements2 = parseStatements("SELECT * FROM Person\nWHERE name = 'Jeff'; -- Some comment")
        assertThat(statements2.size).isEqualTo(1)
        assertThat(statements2.first()).isEqualTo("SELECT * FROM Person\nWHERE name = 'Jeff';")

        // 2 x 2 line sql with comment after 2nd line of first statement
        val statements3 = parseStatements("SELECT * FROM Person\nWHERE name = 'Jeff'; -- Some comment\nSELECT * FROM Person\nWHERE name = 'Ty';")
        assertThat(statements3.size).isEqualTo(2)
        assertThat(statements3[1]).isEqualTo("SELECT * FROM Person\nWHERE name = 'Ty';")
    }

    private fun parseStatements(fileContent: String): List<String> {
        val sqlTestDir = File("build/test-sql")
        sqlTestDir.mkdirs()

        val tempFile = File(sqlTestDir, "test.sql")
        if (tempFile.exists()) {
            tempFile.delete()
        }

        tempFile.writeText(fileContent)

        val statements = mutableListOf<String>()
        tempFile.parseAndExecuteSqlStatements { sqlStatement ->
            statements.add(sqlStatement)
        }

        return statements
    }
}
