
package org.dbtools.sample.roomsqlite.datasource.database.main.individual

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Delete
import android.arch.persistence.room.Insert
import android.arch.persistence.room.Query
import android.arch.persistence.room.Update


@Dao
interface IndividualDao  {
    @Insert
    fun insert(individual: Individual)

    @Query("SELECT count(1) FROM individual")
    fun findCount(): Long

    @Query("SELECT firstName FROM individual WHERE id = :id")
    fun findFirstNameById(id: Long): String

    @Query("SELECT MAX(id) FROM individual")
    fun findLastIndividualId(): Long

    @Query("SELECT * FROM individual WHERE id = (SELECT MAX(id) FROM individual)")
    fun findLastIndividual(): Individual?

    @Update
    fun update(individual: Individual)

    @Delete
    fun delete(individual: Individual)

    @Query("DELETE FROM INDIVIDUAL WHERE id = :id")
    fun deleteById(id: Long): Int
}