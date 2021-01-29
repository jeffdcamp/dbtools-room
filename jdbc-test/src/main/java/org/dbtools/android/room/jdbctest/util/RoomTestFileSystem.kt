package org.dbtools.android.room.jdbctest.util

import java.io.File

object RoomTestFileSystem {

    private const val FILESYSTEM_DIR_PATH = "build/room-test-filesystem"
    private val FILESYSTEM_DIR = File(FILESYSTEM_DIR_PATH)
    private const val INTERNAL_DIR_PATH = "$FILESYSTEM_DIR_PATH/internal"
    val INTERNAL_DIR = File(INTERNAL_DIR_PATH)
    private const val EXTERNAL_DIR_PATH = "$FILESYSTEM_DIR_PATH/external"
    val EXTERNAL_DIR = File(EXTERNAL_DIR_PATH)

    const val INTERNAL_FILES_DIR_PATH = "$INTERNAL_DIR_PATH/files"
    val INTERNAL_FILES_DIR = File(INTERNAL_FILES_DIR_PATH)
    const val INTERNAL_DATABASES_DIR_PATH = "$INTERNAL_DIR_PATH/databases"

    private const val EXTERNAL_FILES_DIR_PATH = "$EXTERNAL_DIR_PATH/files"
    val EXTERNAL_FILES_DIR = File(EXTERNAL_FILES_DIR_PATH)

    const val ASSETS_DIR_PATH = "src/main/assets/"

    private const val TEST_RESOURCES_PATH = "src/test/resources"

    private const val DOWNLOADS_PATH = "$EXTERNAL_FILES_DIR_PATH/Downloads"
    val DOWNLOADS_DIR = File(DOWNLOADS_PATH)

    fun getDatabasePath(databaseName: String): String {
        return "$INTERNAL_DATABASES_DIR_PATH/$databaseName"
    }
}