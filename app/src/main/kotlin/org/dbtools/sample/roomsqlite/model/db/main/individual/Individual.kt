
package org.dbtools.sample.roomsqlite.model.db.main.individual

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.dbtools.sample.roomsqlite.model.db.main.type.IndividualType
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalDateTime


@Entity(tableName = "individual")
class Individual  {
    @PrimaryKey(autoGenerate = true)
    var id = 0L
    var individualType = IndividualType.HEAD
    var individualTypeText = IndividualType.HEAD
    var firstName = ""
    var lastName = ""
    var sampleDateTime: LocalDateTime? = null
    var birthDate: LocalDate? = null
    var lastModified: LocalDateTime? = null
    var number: Int? = null
    var phone: String? = null
    var email: String? = null
    var data: ByteArray? = null
    var amount1: Float? = null
    var amount2: Double? = null
    var enabled: Boolean? = null
    var spouseIndividualId: Long? = null
}