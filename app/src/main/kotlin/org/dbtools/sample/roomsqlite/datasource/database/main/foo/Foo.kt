package org.dbtools.sample.roomsqlite.datasource.database.main.foo

import android.arch.persistence.room.Embedded
import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey

@Entity(tableName = "foo")
class Foo {
    @PrimaryKey(autoGenerate = true)
    var id = 0L
    var name = ""
    @Embedded
    var location = Location()
    var enabled = true

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other?.javaClass != javaClass) return false

        other as Foo

        if (id != other.id) return false
        if (name != other.name) return false
        if (location != other.location) return false
        if (enabled != other.enabled) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + location.hashCode()
        result = 31 * result + enabled.hashCode()
        return result
    }

    override fun toString(): String {
        return "Foo(id=$id, name='$name', location=$location, enabled=$enabled)"
    }
}