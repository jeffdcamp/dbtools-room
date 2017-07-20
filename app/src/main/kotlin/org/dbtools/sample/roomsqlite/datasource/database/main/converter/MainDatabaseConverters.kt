package org.dbtools.sample.roomsqlite.datasource.database.main.converter

import android.arch.persistence.room.TypeConverter
import org.dbtools.sample.roomsqlite.datasource.database.main.type.IndividualType
import java.util.Date

class MainDatabaseConverters {
    @TypeConverter
    fun fromStringToIndividualType(value: String): IndividualType {
        return IndividualType.valueOf(value)
    }

    @TypeConverter
    fun fromIndividualTypeToString(value: IndividualType): String {
        return value.toString()
    }

    @TypeConverter
    fun fromLongToDate(value: Long?): Date? {
        value ?: return null
        return Date(value)
    }

    @TypeConverter
    fun fromDateToLong(value: Date?): Long? {
        value ?: return null
        return value.time
    }
}