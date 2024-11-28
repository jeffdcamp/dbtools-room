DBTools Room KMP Library
========================

DBTools Room is a KMP library that makes it even easier to work with Google Room Library and SQLite Databases.
NOTE: This version of the library ONLY works with the KMP version of Room (2.7.0+)

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.dbtools/dbtools-room/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.dbtools/dbtools-room)

**Features**

* Tools to validate a Sqlite database (PRAGMA checks, etc)
* Tools to Delete and Rename database (making sure to take care of all extra files)
* Kotlin date-time TypeConverters
* Filesystem utilities
    * delete all sqlite files
    * rename files for sqlite database
* SqliteConnection extensions
    * Attach / Detach database
    * Merge data between multiple databases
    * Find table/view names
    * Check if table/view/column exists
    * Simplification for create/drop/recreate views
    * Apply SQL text files to a database (such as a sql diff file) 
    * Get database version
    * Validate Database / Integrity checks
* SQLiteStatement extensions
    * Get getColumnIndexOrThrow 
* RoomDatabaseRepository allows an app to manage multiple instance of the same database by key

**Support SQLite support**

For support using Room 2.7.0 and Support SQLite use 8.3.0+ versions of dbtools-room libraries


License
=======

    Copyright 2017-2024 Jeff Campbell

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
