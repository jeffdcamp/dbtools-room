# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [9.0.8] - 2025-04-12

### Changed

- Changed Android Gradle Library Plugin to "com.android.kotlin.multiplatform.library"

## [9.0.7] - 2025-04-10

### Changed

- Room 2.7.0

## [9.0.6] - 2025-03-13

### Changed

- Room 2.7.0-rc03
- Kotlin 2.1.20


## [9.0.5] - 2025-03-13

### Changed

- Room 2.7.0-rc02


## [9.0.4] - 2025-02-27

### Changed

- Room 2.7.0-rc01


## [9.0.3] - 2025-02-20

### Changed

- Room 2.7.0-beta01


## [9.0.2] - 2025-02-04

### Changed

- Room 2.7.0-alpha13

## [9.0.1] - 2024-12-22

### Changed

- Room 2.7.0-alpha12
- Kotlin 2.1.0
- Coroutines 1.10.1


## [9.0.0] - 2024-11-27

### Changed

- Changed dbtools-room to fully support Room KMP (https://developer.android.com/training/data-storage/room/room-kmp-migration)
- Room 2.7.0-alpha11
- Added SqliteConnection extensions
- Added SQLiteStatement extensions
- Added Kotlin date-time TypeConverters
- Added Filesystem utilities
- Added RoomDatabaseRepository

### Removed

- Removed all Room code associated with Support SQLite
- Removed JDBC libraries (jdbc, jdbc-tes)
- Remove sqlite-android

## [8.3.0] - 2023-10-18

### Changed

- Room 2.6.1
- Kotlin 1.9.20
- Updated versions

## [8.2.0] - 2023-10-18

### Changed

- Room 2.6.0
- Updated versions

Version 8.2.0-rc01 *(2023-09)*
-------------------------
* Room 2.6.0-rc01
* Updated versions

Version 8.2.0-beta01 *(2023-09)*
-------------------------
* Room 2.6.0-beta01
* Kotlin 1.9.10
* Changed Timber > Logger 
* Support for xerial 3.43.0.0 / fixed JdbcSQLiteStatement.executeInsert() to no longer use generatedKeys

Version 8.2.0-alpha02 *(2023-06)*
-------------------------
* Room 2.6.0-alpha02
* Kotlin 1.8.21
* Updated documentation
* Updated other project dependencies (AGP 8.0.2/Gradle 8.1.1/Java 17/etc)

Version 8.1.0 *(2023-03)*
-------------------------
* Added proguard rules for R8
* Kotlin 1.8.10

Version 8.0.0 *(2023-01)*
-------------------------
* Room 2.5.0

Version 8.0.0-rc01 *(2022-12)*
-------------------------
* Room 2.5.0-rc01

Version 8.0.0-beta02 *(2022-11)*
-------------------------
* Room 2.5.0-beta02

Version 8.0.0-beta01 *(2022-10)*
-------------------------
* Room 2.5.0-beta01

Version 8.0.0-alpha02 *(2022-09)*
-------------------------
* Room 2.5.0-alpha03

Version 8.0.0-alpha01 *(2022-06)*
-------------------------
* Initial support for Room 2.5.0-alpha02

Version 7.1.0 *(2022-05)*
-------------------------
* Fix for isDatabaseIntegrityOk() to call prepareStatement
* Updated publishing in build.gradle.kts
* Kotlin 1.6.21

NOTE: If using "dbtools-room-jdbc-test", migrating to 7.1.0 will require you to add "dbtools-room-jdbc" dependency 

Version 7.0.1 *(2022-01)*
-------------------------
* Added Room 2.4.1

Version 7.0.0 *(2022-01)*
-------------------------
* Added Room 2.4.0
* Kotlin 1.6.10

Version 7.0.0-beta04 *(2021-12)*
-------------------------
* Added Room 2.4.0-rc01
* Kotlin 1.6
* Improved LoggingQueryCallback to better support ? in args

Version 7.0.0-beta03 *(2021-11)*
-------------------------
* Added RoomDatabaseExtension enableJdbcTransactionSupport 

Version 7.0.0-beta02 *(2021-10)*
-------------------------
* Added CursorExt.kt (Set of extension functions on Cursor to make it simpler to get and test data)

Version 7.0.0-beta01 *(2021-10)*
-------------------------
* Room 2.4.0-beta01

Version 7.0.0-alpha02 *(2021-09)*
-------------------------
* Added coded to fix/prevent issues with leading newline characters in DatabaseViewQuery.viewQuery
* Added unit tests to core JDBC library
* Added ability to disable JDBC transactions (known issue as of Room 2.4.0 - bulk insert needs to be fixed (because inserts get put into multiple threads, this sometimes causes the jdbc driver to throw: "database in auto-commit mode"))

Version 7.0.0-alpha01 *(2021-09)*
-------------------------
* Room 2.4.0-alpha04
* Improved logging when database corruption occurs

Version 6.1.0 *(2021-05)*
-------------------------
* Changed validateDatabaseFile(...) to use Room isDatabaseIntegrityOk()
* Added LoggingCallback - from hansenji

Version 6.0.0 *(2021-04)*
-------------------------
* Room 2.3.0

Version 6.0.0-rc01 *(2021-03)*
-------------------------
* Room 2.3.0-rc01

Version 6.0.0-beta03 *(2021-03)*
-------------------------
* Room 2.3.0-beta03
* Kotlin 1.4.31

Version 6.0.0-beta01 *(2021-01)*
-------------------------
* Room 2.3.0-beta01
* Added the ability to test Room migrations in JUnit tests (added RoomDatabaseMigrationExtension and BaseMigrationTest)
* Added LoggingQueryCallback (simple sql logger to support new Room 2.3.0 QueryCallback)

Version 5.8.0 *(2020-09)*
-------------------------
* Added AndroidSQLiteDatabase to support extended database functionality on default Android databases (AndroidSQLiteOpenHelperFactory onDatabaseConfigureBlock)

Version 5.7.2 *(2020-09)*
-------------------------
* Bug fixes

Version 5.7.1 *(2020-06)*
-------------------------
* Changed parameters from "application: Application" to "context: Context" to better support JetPack Android Hilt

Version 5.6.1 *(2020-05)*
-------------------------
* Changed dependency Xerial dependency to be compileOnly (to allow substitution of different jdbc driver... each app will need to include Xerial or other jdbc library in their projects)

Version 5.6.0 *(2020-03)*
-------------------------
* Improved view create/drop functions and extension functions
* Added support for dropping ALL views from a database

Version 5.5.3 *(2020-03)*
-------------------------
* Room 2.2.5
* Kotlin 1.3.70

Version 5.5.2 *(2020-01)*
-------------------------
* Minor patch for SqliteOrg databases (contributed by hansenji)

Version 5.5.1 *(2020-01)*
-------------------------
* Room 2.2.3
* Improved database validation checks (contributed by hansenji)

Version 5.4.0 *(2019-12)*
-------------------------
* Room 2.2.2
* Added table exist check to SQLiteDatabase.alterTableIfColumnDoesNotExist(...)

Version 5.3.0 *(2019-10)*
-------------------------
* Added RoomFlow.toFlow(...) - get notified when specific table changes happen

Version 5.2.1 *(2019-10)*
-------------------------
* Room 2.2.1

Version 5.2.0 *(2019-10)*
-------------------------
* Room 2.2.0
* Remove cancelling job in `toLiveData` since a race condition can cancel all jobs (contributed by hansenji)
* Changed default coroutine dispatchers to Dispatchers.IO

Version 5.0.1 *(2019-09)*
-------------------------
* Fixed issue with alterTableIfColumnDoesNotExist(...)

Version 5.0.0 *(2019-09)*
-------------------------
* Room 2.2.0-rc01
* Added tableNameMap parameter to MergeDatabaseUtil.mergeDatabase(...)
* Improved checkAndFixRoomIdentityHash() to work better with Room 2.2.0
* Removed deprecated functions

Version 4.9.6 *(2019-07)*
-------------------------
* Minor fix to JdbcSQLiteOpenHelper

Version 4.9.5 *(2019-07)*
-------------------------
* Fixed missing dependencies
* Minor bug fixes
* Updated versions

Version 4.9.4 *(2019-06)*
-------------------------
* Room 2.1.0

Version 4.9.3 *(2019-06)*
-------------------------
* Added SupportSQLiteDatabase.runInTransaction
* Added SupportSQLiteDatabase.withTransaction (support suspend)
* AGP 3.5.0-beta04

Version 4.9.1 *(2019-06)*
-------------------------
* Room 2.1.0-rc01
* AGP 3.5.0-beta03

Version 4.9.0 *(2019-05)*
-------------------------
* Room 2.1.0-beta01
* AGP 3.5.0-beta01

Version 4.8.0 *(2019-04)*
-------------------------
* Room 2.1.0-alpha07
* Coroutines 1.2.1

Version 4.7.1 *(2019-04)*
-------------------------
* Kotlin 1.3.31 and other minor version updates
* Minor fix when deleting or renaming database files 

Version 4.7.0 *(2019-03)*
-------------------------
* Room 2.1.0-alpha06

Version 4.6.0 *(2019-03)*
-------------------------
* Room 2.1.0-alpha05
* Kotlin 1.3.21
* Deprecated RoomDatabaseExt.runInTransactionSuspend(block) (replaced with room-ktx withTransaction(block))

Version 4.5.0 *(2019-01)*
-------------------------
* Room 2.1.0-alpha04
* Added RoomDatabaseExt.runInTransactionSuspend(block) (suspend function support)
* Kotlin 1.3.20
* Coroutines 1.1.1

Version 4.4.0 *(2019-01)*
-------------------------
* Added DatabaseUtil.checkAndFixRoomIdentityHash(...), findRoomIdentityHash(...), tableExists(...)

Version 4.3.0 *(2018-12)*
-------------------------
* Added JdbcDatabaseUtil.alterTableIfColumnDoesNotExist(...), columnExists(...), resetRoom(...)
* Added password support to JdbcSQLiteOpenHelper
* Added onDatabaseConfigureBlock support to JdbcSQLiteOpenHelper

Version 4.2.0 *(2018-12)*
-------------------------
* Added DatabaseUtil.alterTableIfColumnDoesNotExist(...), columnExists(...), resetRoom(...)
* Added SQLiteDatabase and SqliteOrgDatabaseUtil .alterTableIfColumnDoesNotExist(...), columnExists(...), resetRoom(...)
* Room 2.1.0-alpha03
* Kotlin 1.3.11

Version 4.1.0 *(2018-11)*
-------------------------
* Initial support for Room Views (added DatabaseUtil / CloseableDatabaseWrapper / CloseableDatabaseWrapperRepository functions to create/drop/update views)
* Room 2.1.0-alpha02
* Kotlin 1.3.10
* Coroutines 1.0.1

Version 4.0.0 *(2018-10)*
-------------------------
* Kotlin 1.3.0
* Coroutines 1.0.0

Version 3.3.4 *(2018-10)*
-------------------------
* CloseableDatabaseWrapperRepository.closeDatabase() returns true/false if a database is removed

Version 3.3.0 *(2018-10)*
-------------------------
* Removed CloseableDatabaseWrapperRepository.getDatabaseOrNull()
* Changed CloseableDatabaseWrapperRepository.getDatabase() to return null instead of throwing an exception

Version 3.2.0 *(2018-10)*
-------------------------
* Added CloseableDatabaseWrapperRepository.getDatabaseOrNull()
* Room 2.0.0

Version 3.1.0 *(2018-09)*
-------------------------
* Fixed RoomDatabaseExt attach / detach
* Added RoomDatabaseExt.getAttachedDatabases()  getSqliteVersion(), getVersion(), findTableRowCount(tableName)
* Added SupportSQLiteDatabaseExt.getAttachedDatabases()

Version 3.0.0 *(2018-09)*
-------------------------
* AndroidX / SDK 28

Version 2.5.2 *(2018-09)*
-------------------------
* Fixed RoomDatabaseExt applySqlFile(...) to handle comments generated by sqldiff

Version 2.5.1 *(2018-09)*
-------------------------
* Fixed RoomLiveData to not getData() if there are no observers

Version 2.5.0 *(2018-08)*
-------------------------
* Improved support to configure a Room database with custom extensions (onDatabaseConfigureBlock)

Version 2.4.0 *(2018-07)*
-------------------------
* Apply SQL text files to a database (such as a sql diff file)
* Added ability to validate a database prior to connecting it to Room

Version 2.3.1 *(2018-06)*
-------------------------
* Added support for sqlite extensions (when using SqliteOrg database)
* Added RoomDatabase.tableExists(...) and SupportSQLiteDatabase.tableExists(...)

Version 2.2.0 *(2018-06)*
-------------------------
* Added ability to merge databases (MergeDatabaseUtil)
* Added RoomDatabase Kotlin extensions (RoomDatabaseExt)
* Added SupportSQLiteDatabase Kotlin extensions (SupportSQLiteDatabaseExt)

Version 2.1.0 *(2018-06)*
-------------------------
* Room 1.1.1
* Updated other project versions

Version 2.0.0 *(2018-05)*
-------------------------
* Added RoomLiveData.toLiveData(...)
* ClosableDatabaseManager to ClosableDatabaseWrapper
* Added ClosableDatabaseWrapperRepository
* Added DatabaseUtil: validateDatabase(...), deleteDatabaseFiles(...), renameDatabaseFiles(...)
* Create helper to run migration unit tests locally in the JVM
* Removed DynamicQueryDao
* Room 1.1.0

Version 1.5.4 *(2018-05)*
-------------------------
* Room 1.1.0-rc1

Version 1.5.3 *(2018-03)*
-------------------------
* Room 1.1.0-beta1
* Lifecycle 1.1.1

Version 1.5.2 *(2018-03)*
-------------------------
* Improvements to JdbcSqliteDatabase to for testing
* Fixed JdbcSqliteDatabase.getType(...)

Version 1.5.0 *(2018-03)*
-------------------------
* Room 1.1.0-alpha3
* Lifecycle 1.1.0
* Removed conflicting BuildConfig (with dbtools-android)
* Support Library 27.1.0

Version 1.3.3 *(2018-01)*
-------------------------
* Fix for issue #5 JdbcMemoryCursor doesn't support nullable fields
* Updated dependency versions

Version 1.2.0 *(2017-12)*
-------------------------
* Added Support for swapping databases using new CloseableDatabaseManager (https://issuetracker.google.com/issues/64681453)
* Support Library 27.0.2
* Kotlin 1.2.0
* Changed Kotlin Standard Library to kotlin-stdlib-jdk7

Version 1.1.0 *(2017-11)*
-------------------------
* Room 1.0.0
* Android SDK 27 / Support Library 27.0.0
* Kotlin 1.1.51

Version 1.0.7 *(2017-09)*
-------------------------
* Room 1.0.0-beta1

Version 1.0.6 *(2017-09)*
-------------------------
* Room 1.0.0-alpha9-1

Version 1.0.5 *(2017-09)*
-------------------------
* Room 1.0.0-alpha9

Version 1.0.4 *(2017-08)*
---------------------------
* Room 1.0.0-alpha8

Version 1.0.3 *(2017-07)*
---------------------------
* Added the ability to supply custom sqlite library loader
* Room 1.0.0-alpha7

Version 1.0.2 *(2017-07)*
---------------------------
* Added Dynamic Query support (contributed by hansenji)
* Room 1.0.0-alpha6


Version 1.0.1 *(2017-07)*
---------------------------
* Added JDBC Room support (contributed by hansenji)
* Room 1.0.0-alpha5


Version 1.0.0 *(2017-07)*
---------------------------
* Initial Release


