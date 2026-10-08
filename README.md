DBTools Room KMP Library
========================

DBTools Room is a KMP library that makes it even easier to work with Google Room Library and SQLite Databases.

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.dbtools/dbtools-room/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.dbtools/dbtools-room)

**Setup**

```kotlin
commonMain.dependencies {
    implementation("org.dbtools:dbtools-room:11.0.0")
}
```

Platforms: Android (minSdk 26), JVM, iOS (`iosArm64`, `iosSimulatorArm64`), macOS (`macosArm64`), Linux (`linuxX64`)

**Room version support**

| dbtools-room | Room |
| --- | --- |
| 10.x+ | Room 3.x KMP (`androidx.room3`) |
| 9.x | Room 2.7.0+ KMP (SQLite Driver) |
| 8.3.0+ | Room 2.7.0 with Support SQLite |

**Features**

* Tools to validate a Sqlite database (PRAGMA checks, etc)
* Tools to Delete and Rename database (making sure to take care of all extra files)
* Kotlin date-time TypeConverters
* Filesystem utilities (Okio and kotlinx-io)
    * delete all sqlite files
    * rename files for sqlite database
* SQLiteConnection extensions (also available on Room `Transactor` and `RoomDatabase`)
    * Attach / Detach database
    * Merge data between multiple databases
    * Find table/view names
    * Find table/view info (type and `CREATE` statement) and column info (type, not-null, default, primary key)
    * Row count for a table or view
    * Check if table/view/column exists
    * Simplification for create/drop/recreate views
    * Apply SQL text files to a database (such as a sql diff file)
    * Get/set database version
    * Validate Database / Integrity checks
* SQLiteStatement extensions
    * `getColumnIndexOrThrow()`
    * Nullable getters (`getTextOrNull()`, `getLongOrNull()`, etc)
    * `bindArgs()` to bind a list of values, and `getValue()` / `getValues()` to read a column or row back as its SQLite type
* `String.quoteSqlIdentifier()` to safely quote table/column names in SQL
* DatabaseProvider manages creating, closing, and deleting a single database instance
* RoomDatabaseRepository allows an app to manage multiple instance of the same database by key


License
=======

    Copyright 2017-2026 Jeff Campbell

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
