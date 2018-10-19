package org.dbtools.sample.roomsqlite.model.repository

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.experimental.Dispatchers
import kotlinx.coroutines.experimental.GlobalScope
import kotlinx.coroutines.experimental.launch
import org.dbtools.android.room.ext.validDatabaseFile
import org.dbtools.android.room.toLiveData
import org.dbtools.android.room.util.DatabaseUtil
import org.dbtools.sample.roomsqlite.model.db.main.MainDatabaseWrapperRepository
import org.dbtools.sample.roomsqlite.model.db.main.individual.Individual
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalDateTime
import timber.log.Timber

class IndividualRepository(
    private val application: Application
) {

    private val mainDatabaseWrapperRepository by lazy { MainDatabaseWrapperRepository(application) }
    private fun mainDatabase(key: String) = mainDatabaseWrapperRepository.getDatabase(key)
    private fun individualDao(key: String) = mainDatabase(key)?.individualDao

    fun init() {
        mainDatabaseWrapperRepository.registerDatabase(DB_A, DB_A_PATH)
        mainDatabaseWrapperRepository.registerDatabase(DB_B, DB_B_PATH)
    }

    fun addIndividual(firstName: String, lastName: String, key: String = DB_A) {
        individualDao(key)?.insert(createIndividual(firstName, lastName))
    }

    fun getIndividualCount(key: String = DB_A) = individualDao(key)?.findCount() ?: 0


    fun getLastIndividualNumber(key: String = DB_A): Int {
        getLastIndividualId(key)?.let {
            return individualDao(key)?.findNumberById(it) ?: -1
        } ?: return -1
    }

    fun updateLastIndividualNumber(newNumber: Int, key: String = DB_A) {
        getLastIndividualId(key)?.let {
            individualDao(key)?.updateNumber(it, newNumber)
        }
    }

    fun findNumberByIdLiveData(key: String = DB_A): LiveData<Long> {
        return mainDatabaseWrapperRepository.getDatabase(key)?.toLiveData("individual") {
            getLastIndividualId(key) ?: 0
        } ?: MutableLiveData<Long>()
    }

    fun getLastIndividualId(key: String = DB_A) = individualDao(key)?.findLastIndividualId()

    fun getLastIndividual(key: String = DB_A): Individual? {
        getLastIndividualId(key)?.let {
            return individualDao(key)?.findById(it)
        } ?: return null
    }

    fun getLastIndividualFirstName(key: String = DB_A): String {
        getLastIndividualId(key)?.let {
            return individualDao(key)?.findFirstNameById(it) ?: ""
        } ?: return ""
    }

    fun deleteLastIndividual(key: String = DB_A): Int {
        getLastIndividualId(key)?.let {
            return individualDao(key)?.deleteById(it) ?: 0
        } ?: return 0
    }

    fun updateLastIndividualName(key: String = DB_A): Boolean {
        getLastIndividual(key)?.let { individual ->
            individual.firstName = "Jeffery"
            individualDao(key)?.update(individual)
            return true
        } ?: return false
    }

    fun showAllIndividuals(key: String = DB_A) = GlobalScope.launch(Dispatchers.Default) {
        showMainDatabaseInfo(key)
    }

    fun deleteAllIndividuals(key: String = DB_A) = GlobalScope.launch(Dispatchers.Default) {
        individualDao(key)?.deleteAll()
    }

    private fun createIndividual(firstName: String, lastName: String, birth: LocalDate = LocalDate.of(1970, 2, 2), phone: String = "555-555-1234"): Individual {
        return Individual().apply {
            this.firstName = firstName
            this.lastName = lastName
            sampleDateTime = LocalDateTime.now()
            birthDate = birth
            lastModified = LocalDateTime.now()
            number = 1234
            this.phone = phone
            email = "test@test.com"
            amount1 = 19.95f
            amount2 = 1000000000.25
            enabled = true
        }
    }

    fun validateDatabases() {
        // write a bad database file
        // File(downloadsDir, DB_A).writeText("bad data")

        // Test
        val mainDatabaseA = mainDatabaseWrapperRepository.getDatabase(DB_A)
        val mainDatabaseB = mainDatabaseWrapperRepository.getDatabase(DB_B)

        if (mainDatabaseA?.validDatabaseFile(DB_A) == false) {
            Timber.e("Database validation failed.... exiting")
            return
        }

        Timber.i("Database A path: [$DB_A_PATH]" )
        Timber.i("Database B path: [$DB_B_PATH]" )

        showAllMainDatabaseInfo("After Register Database")

        addIndividual("Jeff", "Campbell", DB_A)

        showAllMainDatabaseInfo("After Insert A")

        addIndividual("John", "Brown", DB_B)

        showAllMainDatabaseInfo("After Insert A AND B")

        // close will unregister databases
//        mainDatabaseWrapperRepository.closeAllDatabases(true)
    }

    fun mergeDatabases(key: String = DB_A) {
        // copy the test database from assets
        val mergeDatabase1 = DatabaseUtil.copyDatabaseFromAssets(application, "merge1", true)
        val mergeDatabase2 = DatabaseUtil.copyDatabaseFromAssets(application, "merge2", true)

        val mainDatabase = mainDatabaseWrapperRepository.getDatabase(key)
        mainDatabase?.mergeDataFromOtherDatabase(mergeDatabase1)
        mainDatabase?.mergeDataFromOtherDatabase(mergeDatabase2)

        showMainDatabaseInfo(key)
    }

    private fun showAllMainDatabaseInfo(event: String) {
        Timber.i("========== $event ==========")
        showMainDatabaseInfo(DB_A)
        showMainDatabaseInfo(DB_B)
    }

    private fun showMainDatabaseInfo(key: String = DB_A) {
        Timber.i("===== Database [$key] info: count[${getIndividualCount(key)}] =====")
        val allIndividuals = individualDao(key)?.findAll() ?: emptyList()
        allIndividuals.forEach {individual ->
            Timber.i("- ${individual.firstName} ${individual.lastName}")
        }
    }

    fun findCount(key: String = DB_A) = individualDao(key)?.findCount() ?: 0

    companion object {
        private const val DB_A = "a"
        private const val DB_A_PATH = "a.db"
        private const val DB_B = "b"
        private const val DB_B_PATH = "b.db"
    }
}