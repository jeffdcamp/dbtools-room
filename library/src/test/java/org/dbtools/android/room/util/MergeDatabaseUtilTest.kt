package org.dbtools.android.room.util

import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

internal class MergeDatabaseUtilTest {

    @Test
    fun testCreateTableNamesToMergeBasic() {
        val resultMergeTables = MergeDatabaseUtil.createTableNamesToMerge(DEFAULT_SOURCES)

        validateMergeTables(listOf(A_A, B_B, C_C), resultMergeTables)
    }

    @Test
    fun testCreateTableNamesToMergeIncludes() {
        validateMergeTables(listOf(A_A), MergeDatabaseUtil.createTableNamesToMerge(DEFAULT_SOURCES, includeTables = listOf("a")))
        validateMergeTables(listOf(A_A, B_B), MergeDatabaseUtil.createTableNamesToMerge(DEFAULT_SOURCES, includeTables = listOf("a", "b")))
        validateMergeTables(listOf(A_A, B_B, C_C), MergeDatabaseUtil.createTableNamesToMerge(DEFAULT_SOURCES, includeTables = listOf("a", "b", "c")))
        validateMergeTables(listOf(A_A, C_C), MergeDatabaseUtil.createTableNamesToMerge(DEFAULT_SOURCES, includeTables = listOf("a", "c")))
        validateMergeTables(listOf(B_B), MergeDatabaseUtil.createTableNamesToMerge(DEFAULT_SOURCES, includeTables = listOf("b")))
    }

    @Test
    fun testCreateTableNamesToMergeExcludes() {
        validateMergeTables(listOf(B_B, C_C), MergeDatabaseUtil.createTableNamesToMerge(DEFAULT_SOURCES, excludeTables = listOf("a")))
        validateMergeTables(listOf(C_C), MergeDatabaseUtil.createTableNamesToMerge(DEFAULT_SOURCES, excludeTables = listOf("a", "b")))
        validateMergeTables(emptyList(), MergeDatabaseUtil.createTableNamesToMerge(DEFAULT_SOURCES, excludeTables = listOf("a", "b", "c")))
    }

    @Test
    fun testCreateTableNamesToMergeIncludeAndExclude() {
        validateMergeTables(emptyList(), MergeDatabaseUtil.createTableNamesToMerge(DEFAULT_SOURCES, includeTables = listOf("a"), excludeTables = listOf("a")))
        validateMergeTables(listOf(A_A), MergeDatabaseUtil.createTableNamesToMerge(DEFAULT_SOURCES, includeTables = listOf("a"), excludeTables = listOf("b")))
        validateMergeTables(listOf(A_A), MergeDatabaseUtil.createTableNamesToMerge(DEFAULT_SOURCES, includeTables = listOf("a", "c"), excludeTables = listOf("c")))
    }

    @Test
    fun testCreateTableNamesToMergeTableMap() {
        validateMergeTables(listOf(A_A, B_B, X_C), MergeDatabaseUtil.createTableNamesToMerge(listOf("a", "b", "x"), sourceTableNameMap = mapOf("x" to "c")))

        // multiple maps
        validateMergeTables(listOf(A_A, Y_B, X_C), MergeDatabaseUtil.createTableNamesToMerge(listOf("a", "y", "x"), sourceTableNameMap = mapOf("x" to "c", "y" to "b")))

        // ignore invalid mapping
        validateMergeTables(listOf(A_A, B_B), MergeDatabaseUtil.createTableNamesToMerge(listOf("a", "b"), sourceTableNameMap = mapOf("x" to "a")))
    }

    @Test
    fun testCreateTableNamesToMergeTableMapWithOtherOptions() {
        // with includes
        validateMergeTables(listOf(X_C), MergeDatabaseUtil.createTableNamesToMerge(listOf("a", "b", "x"), includeTables = listOf("x"),sourceTableNameMap = mapOf("x" to "c")))
        validateMergeTables(listOf(A_A, X_C), MergeDatabaseUtil.createTableNamesToMerge(listOf("a", "b", "x"), includeTables = listOf("a", "x"), sourceTableNameMap = mapOf("x" to "c")))

        // with includes/excludes
        validateMergeTables(listOf(X_C), MergeDatabaseUtil.createTableNamesToMerge(listOf("a", "b", "x"), includeTables = listOf("x"),sourceTableNameMap = mapOf("x" to "c")))
        validateMergeTables(listOf(A_A), MergeDatabaseUtil.createTableNamesToMerge(listOf("a", "b", "x"), includeTables = listOf("a", "x"), excludeTables = listOf("x"), sourceTableNameMap = mapOf("x" to "c")))

    }

    private fun validateMergeTables(expectedMergeTables: List<MergeTable>, actualMergeTables: List<MergeTable>) {
        println("Expected: $expectedMergeTables")
        println("Actual: $actualMergeTables")
        println("") // newline

        assertEquals(expectedMergeTables.size, actualMergeTables.size, "Same number of tables")

        expectedMergeTables.forEachIndexed { index, expectedMergeTable ->
            val actualMergeTable = actualMergeTables[index]

            assertEquals(expectedMergeTable.sourceTableName, actualMergeTable.sourceTableName, "Source Table Name")
            assertEquals(expectedMergeTable.targetTableName, actualMergeTable.targetTableName, "Target Table Name")
        }
    }

    companion object {
        private val DEFAULT_SOURCES = listOf("a", "b", "c")

        private val A_A = MergeTable("a", "a")
        private val B_B = MergeTable("b", "b")
        private val C_C = MergeTable("c", "c")
        private val X_C = MergeTable("x", "c")
        private val Y_B = MergeTable("y", "b")
    }
}