DBTools for Android
=================

DBTools Room for Android is an library that makes it even easier to work with Google Room Library and SQLite Databases.

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.dbtools/dbtools-room/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.dbtools/dbtools-room)

**Features**

* Sqlite.org database support
* JDBC database support (for Unit Tests against real sqlite databases using Room)
* Swap database files using the same Entities and Doa's (CloseableDatabaseWrapper and CloseableDatabaseWrapperRepository)
* Room ComputableLiveData for bulk work that fires LiveData events when specific database tables change (RoomLiveData)
* Tools to validate a Sqlite database (PRAGMA checks, etc)
* Tools to Delete and Rename database (making sure to take care of all extra files)
* Attach/Detach databases
* Merge data between multiple databases

Usage
=====


License
=======

    Copyright 2017 Jeff Campbell

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
