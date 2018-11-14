
package org.dbtools.sample.roomsqlite.model.db.main.individualview

import androidx.room.DatabaseView

@DatabaseView(
    viewName = IndividualView.VIEW_NAME,
    value = IndividualView.VIEW_QUERY
)
data class IndividualView (
    val id: Long? = null,
    val name:String = "",
    val phone: String = ""
){
    companion object {
        const val VIEW_NAME = "individual_view"
        const val VIEW_QUERY = "SELECT id, firstName, phone FROM Individual"
    }
}