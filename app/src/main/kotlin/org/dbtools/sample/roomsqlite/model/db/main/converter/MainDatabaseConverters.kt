package org.dbtools.sample.roomsqlite.model.db.main.converter

import androidx.room.TypeConverter
import org.dbtools.sample.roomsqlite.model.db.main.type.IndividualType

class MainDatabaseConverters {
    @TypeConverter
    fun fromStringToIndividualType(value: String): IndividualType {
        return IndividualType.valueOf(value)
    }

    @TypeConverter
    fun fromIndividualTypeToString(value: IndividualType): String {
        return value.toString()
    }

}