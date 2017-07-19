
package org.dbtools.sample.roomsqlite.datasource.database.main.individual

import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey


@Entity(tableName = "individual")
class Individual  {
    @PrimaryKey(autoGenerate = true)
    var id = 0L
    var individualType = org.dbtools.sample.roomsqlite.datasource.database.main.type.IndividualType.HEAD
    var individualTypeText = org.dbtools.sample.roomsqlite.datasource.database.main.type.IndividualType.HEAD
    var firstName = ""
    var lastName = ""
    var sampleDateTime: java.util.Date? = null
    var birthDate: java.util.Date? = null
    var lastModified: java.util.Date? = null
    var number: Int? = null
    var phone: String? = null
    var email: String? = null
    var data: ByteArray? = null
    var amount1: Float? = null
    var amount2: Double? = null
    var enabled: Boolean? = null
    var spouseIndividualId: Long? = null
}