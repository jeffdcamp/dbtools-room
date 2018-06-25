
package org.dbtools.sample.roomsqlite.model.db.main.individual

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Delete
import android.arch.persistence.room.Insert
import android.arch.persistence.room.Query
import android.arch.persistence.room.Update


@Dao
interface IndividualDao  {
    @Insert
    fun insert(individual: Individual)

    @Insert
    fun insert(individuals: Collection<Individual>)

    @Query("SELECT count(1) FROM individual")
    fun findCount(): Long

    @Query("SELECT * FROM individual WHERE id = :id")
    fun findById(id: Long): Individual?

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

    @Query("DELETE FROM INDIVIDUAL")
    fun deleteAll()

    @Query("DELETE FROM INDIVIDUAL WHERE id = :id")
    fun deleteById(id: Long): Int

    @Query("SELECT * FROM individual")
    fun findAll(): List<Individual>

    @Query("SELECT number FROM individual WHERE id = :id")
    fun findNumberById(id: Long): Int?

    @Query("UPDATE individual SET number = :number WHERE id = :id")
    fun updateNumber(id: Long, number: Int)
}