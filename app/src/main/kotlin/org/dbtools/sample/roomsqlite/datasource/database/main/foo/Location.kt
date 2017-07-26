package org.dbtools.sample.roomsqlite.datasource.database.main.foo

class Location {
    var latitude: Double = 0.0
    var longitude: Double = 0.0

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other?.javaClass != javaClass) return false

        other as Location

        if (latitude != other.latitude) return false
        if (longitude != other.longitude) return false

        return true
    }

    override fun hashCode(): Int {
        var result = latitude.hashCode()
        result = 31 * result + longitude.hashCode()
        return result
    }

    override fun toString(): String {
        return "Location(latitude=$latitude, longitude=$longitude)"
    }
}