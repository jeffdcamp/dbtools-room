package org.dbtools.sample.roomsqlite.model.repository

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
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
    private fun individualDaoDatabaseA() = mainDatabase(DB_A)?.individualDao
    private fun individualDaoDatabaseB() = mainDatabase(DB_B)?.individualDao

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

    fun showAllIndividuals(key: String = DB_A) = GlobalScope.launch(Dispatchers.IO) {
        showMainDatabaseInfo(key)
    }

    fun deleteAllIndividuals(key: String = DB_A) = GlobalScope.launch(Dispatchers.IO) {
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

    fun mergeDatabases(): TestResults {
        val individualDao = individualDaoDatabaseA() ?: return TestResults(false, "individualDao == null")
        val mainDatabase = mainDatabaseWrapperRepository.getDatabase(DB_A) ?: return TestResults(false, "mainDatabase == null")

        // clear database
        individualDao.deleteAll()

        // add one Individual
        val initialIndividual = Individual().apply {
            firstName = "Jeff"
        }
        individualDao.insert(initialIndividual)
        if (individualDao.findCount() != 1L) {
            return TestResults(false, "Invalid initial count")
        }

        // copy the test database from assets
        val mergeDatabase1 = DatabaseUtil.copyDatabaseFromAssets(application, "merge1", true)
        val mergeDatabase2 = DatabaseUtil.copyDatabaseFromAssets(application, "merge2", true)
        val mergeDatabase3 = DatabaseUtil.copyDatabaseFromAssets(application, "merge3", true)


        // Test1 Database 2 names
        mainDatabase.mergeDataFromOtherDatabase(mergeDatabase1)
        if (individualDao.findCount() != 3L) {
            return TestResults(false, "Failed to merge1 current count: [${individualDao.findCount()}]  expected count: [3]")
        }

        // Test2 Database 3 names
        mainDatabase.mergeDataFromOtherDatabase(mergeDatabase2)
        if (individualDao.findCount() != 6L) {
            return TestResults(false, "Failed to merge2 current count: [${individualDao.findCount()}]  expected count: [6]")
        }

        // Test3 Database 2 names - different table name ("person" -> "individual")
        mainDatabase.mergeDataFromOtherDatabase(mergeDatabase3, includeTables = listOf("person"), tableNameMap = mapOf("person" to "individual"))
        if (individualDao.findCount() != 8L) {
            return TestResults(false, "Failed to merge3 current count: [${individualDao.findCount()}]  expected count: [8]")
        }

        showMainDatabaseInfo(DB_A)

        return TestResults(true, "Success")
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

data class TestResults(val success: Boolean, val message: String = "")