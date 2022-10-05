@file:Suppress("MemberVisibilityCanBePrivate")

object AndroidSdk {
    const val MIN = 21
    const val COMPILE = 33
    const val TARGET = COMPILE
}

object Pom {
    const val GROUP_ID = "org.dbtools"
    const val VERSION_NAME = "8.0.0-beta01"
    const val POM_DESCRIPTION = "DBTools for Room"

    const val URL = "https://github.com/jeffdcamp/dbtools-room/"
    const val SCM_URL = "https://github.com/jeffdcamp/dbtools-room/"
    const val SCM_CONNECTION = "scm:git:git://github.com/jeffdcamp/dbtools-room.git"
    const val SCM_DEV_CONNECTION = "scm:git:git@github.com:jeffdcamp/dbtools-room.git"

    const val LICENCE_NAME = "The Apache Software License, Version 2.0"
    const val LICENCE_URL = "http://www.apache.org/licenses/LICENSE-2.0.txt"
    const val LICENCE_DIST = "repo"

    const val DEVELOPER_ID = "jcampbell"
    const val DEVELOPER_NAME = "Jeff Campbell"

    const val LIBRARY_ARTIFACT_ID = "dbtools-room"
    const val LIBRARY_NAME = "DBTools Room"

    const val LIBRARY_JDBC_ARTIFACT_ID = "dbtools-room-jdbc"
    const val LIBRARY_JDBC_NAME = "DBTools Room JDBC"

    const val LIBRARY_JDBC_TEST_ARTIFACT_ID = "dbtools-room-jdbc-test"
    const val LIBRARY_JDBC_TEST_NAME = "DBTools Room JDBC Test"

    const val LIBRARY_SQLITE_ORG_ARTIFACT_ID = "dbtools-room-sqliteorg"
    const val LIBRARY_SQLITE_ORG_NAME = "DBTools Room SQLITE"
}
