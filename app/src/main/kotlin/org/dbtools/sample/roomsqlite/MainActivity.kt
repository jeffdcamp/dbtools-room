package org.dbtools.sample.roomsqlite

import android.arch.lifecycle.Observer
import android.arch.persistence.room.Room
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.Toast
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.withContext
import org.dbtools.android.room.sqliteorg.SqliteOrgSQLiteOpenHelperFactory
import org.dbtools.android.room.toLiveData
import org.dbtools.android.room.util.DatabaseUtil
import org.dbtools.sample.roomsqlite.databinding.ActivityMainBinding
import org.dbtools.sample.roomsqlite.datasource.database.main.MainDatabase
import org.dbtools.sample.roomsqlite.datasource.database.main.MainDatabaseRepository
import org.dbtools.sample.roomsqlite.datasource.database.main.individual.Individual
import org.dbtools.sample.roomsqlite.datasource.database.main.individual.IndividualDao
import timber.log.Timber
import java.util.Date
import java.util.GregorianCalendar

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private lateinit var mainDatabase: MainDatabase
    private val mainDatabaseRepository by lazy { MainDatabaseRepository(application) }
    private lateinit var individualDao: IndividualDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        mainDatabase = Room.databaseBuilder(application, MainDatabase::class.java, MainDatabase.DATABASE_NAME)
                .openHelperFactory(SqliteOrgSQLiteOpenHelperFactory()) // with NO password
                .fallbackToDestructiveMigration()
//                .openHelperFactory(SqliteOrgSQLiteOpenHelperFactory(password = "abc123")) // password protected
//                .addMigrations(object: Migration(1, 2) {
//                    override fun migrate(p0: SupportSQLiteDatabase?) {
//                    }
//                })
                .build()

        individualDao = mainDatabase.individualDao()

        binding.createButton.setOnClickListener { insertIndividual() }
        binding.updateButton.setOnClickListener { updateLastIndividual() }
        binding.deleteButton.setOnClickListener { deleteLastIndividual() }
        binding.showButton.setOnClickListener { showLastIndividualName() }
        binding.testDatabaseRepositoryButton.setOnClickListener { testDatabaseRepository() }
        binding.testRoomLiveData.setOnClickListener { testRoomLiveData() }
    }

    private fun createIndividual(firstName: String, lastName: String): Individual {
        return Individual().apply {
            this.firstName = firstName
            this.lastName = lastName
            sampleDateTime = Date()
            birthDate = GregorianCalendar(1970, 1, 1).time
            lastModified = Date()
            number = 1234
            phone = "555-555-1234"
            email = "test@test.com"
            amount1 = 19.95f
            amount2 = 1000000000.25
            enabled = true
        }

    }
    private fun insertIndividual() = launch(UI) {
        val count = withContext(CommonPool) {
            val individual = createIndividual("Jeff", "Campbell")

            individualDao.insert(individual)

            return@withContext individualDao.findCount()
        }

        Toast.makeText(this@MainActivity, "Individual Count: $count", Toast.LENGTH_SHORT).show()
    }


    private fun deleteLastIndividual() = launch(UI) {
        if (!hasRecords()) {
            return@launch
        }

        val deleteCount = withContext(coroutineContext + CommonPool) {
            val lastIndividualId = individualDao.findLastIndividualId()
            individualDao.deleteById(lastIndividualId)
        }

        if (deleteCount > 0) {
            Toast.makeText(this@MainActivity, "Last individual deleted", Toast.LENGTH_SHORT).show()
        }

    }

    private fun updateLastIndividual() = launch(UI) {
        // check to make sure there is individuals
        if (!hasRecords()) {
            return@launch
        }

        val updated = withContext(coroutineContext + CommonPool) {
            try {
                val individual = individualDao.findLastIndividual()
                if (individual != null) {
                    individual.firstName = "Jeffery"
                    individualDao.update(individual)
                    return@withContext true
                } else {
                    return@withContext false
                }
            } catch (e: Exception) {
                e.printStackTrace()
                return@withContext false
            }
        }

        if (updated) {
            Toast.makeText(this@MainActivity, "Last individual updated", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this@MainActivity, "Failed to find last individual", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showLastIndividualName() = launch(UI) {
        // check to make sure there is individuals
        if (!hasRecords()) {
            return@launch
        }

        val firstName = withContext(coroutineContext + CommonPool) {
            val lastIndividualId = individualDao.findLastIndividualId()
            return@withContext individualDao.findFirstNameById(lastIndividualId)
        }
        Toast.makeText(this@MainActivity, "Last Individual First Name: $firstName", Toast.LENGTH_SHORT).show()
    }


    private suspend fun hasRecords(): Boolean {
        return withContext(CommonPool) {
            val count = individualDao.findCount()

            if (count <= 0) {
                withContext(UI) {
                    Toast.makeText(this, "No Records exist", Toast.LENGTH_SHORT).show()
                }
                return@withContext false
            } else {
                return@withContext true
            }
        }
    }

    private fun testDatabaseRepository() = launch {
        // Database in app Databases directory
        val databasePathA = "a"
        val databasePathB = "b"

        // Database in Downloads Dir
        // Requires permission
//        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
//        val databasePathA = File(downloadsDir, "a").absolutePath
//        val databasePathB = File(downloadsDir, "b").absolutePath

        // usually only done once on an app launch
        mainDatabaseRepository.registerDatabase("a", databasePathA)
        mainDatabaseRepository.registerDatabase("b", databasePathB)

        // write a bad database file
        // File(downloadsDir, "a").writeText("bad data")

        // Test
        val mainDatabaseA = mainDatabaseRepository.getDatabase("a")
        val mainDatabaseB = mainDatabaseRepository.getDatabase("b")

        if (!DatabaseUtil.validDatabaseFile("a", mainDatabaseA)) {
            Timber.e("Database validation failed.... exiting")
            return@launch
        }

        Timber.i("Database A path: [$databasePathA]" )
        Timber.i("Database A path: [$databasePathB]" )

        showAllMainDatabaseInfo("After Register Database")

        mainDatabaseA.beginTransaction()
        mainDatabaseRepository.individualDao("a").insert(createIndividual("Jeff", "Campbell"))
        mainDatabaseA.setTransactionSuccessful()
        mainDatabaseA.endTransaction()

        showAllMainDatabaseInfo("After Insert A")

        mainDatabaseB.beginTransaction()
        mainDatabaseB.individualDao().insert(createIndividual("John", "Brown"))
        mainDatabaseB.setTransactionSuccessful()
        mainDatabaseB.endTransaction()

        showAllMainDatabaseInfo("After Insert A AND B")

        // close will unregister databases
//        mainDatabaseRepository.closeAllDatabases()
        mainDatabaseRepository.closeAllDatabases(true)
    }

    private fun testRoomLiveData() = launch {
        // check to make sure there is individuals
        if (!hasRecords()) {
            return@launch
        }

        val lastIndividualId = individualDao.findLastIndividualId()

        // Create LiveData
        val liveData = mainDatabase.toLiveData("individual") {
            individualDao.findNumberById(lastIndividualId) ?: 0
        }

        // OBSERVE and show last number
        liveData.observe(this@MainActivity, Observer { data ->
            data ?: return@Observer
            Toast.makeText(this@MainActivity, "Last Number Set: $data", Toast.LENGTH_SHORT).show()
        })


        // CHANGE NUMBER
        delay(1000)
        val lastNumber = individualDao.findNumberById(lastIndividualId)

        if (lastNumber == null) {
            Timber.e("Last number was null")
            return@launch
        }

        individualDao.updateNumber(lastIndividualId, 1)


        // RESTORE
        delay(1000)
        individualDao.updateNumber(lastIndividualId, lastNumber)
    }

    private fun showAllMainDatabaseInfo(event: String) {
        Timber.i("========== $event ==========")
        val mainDatabaseA = mainDatabaseRepository.getDatabase("a")
        val mainDatabaseB = mainDatabaseRepository.getDatabase("b")
        showMainDatabaseInfo("a", mainDatabaseA)
        showMainDatabaseInfo("b", mainDatabaseB)
    }

    private fun showMainDatabaseInfo(key: String, mainDatabase: MainDatabase) {
        val individualDao = mainDatabase.individualDao()

        Timber.i("===== Database [$key] info: count[${individualDao.findCount()}] =====")
        individualDao.findAll().forEach {individual ->
            Timber.i("- ${individual.firstName} ${individual.lastName}")
        }
    }
}
