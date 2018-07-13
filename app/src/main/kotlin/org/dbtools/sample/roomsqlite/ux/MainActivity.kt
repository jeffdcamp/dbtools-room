package org.dbtools.sample.roomsqlite.ux

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
import org.dbtools.android.room.ext.validDatabaseFile
import org.dbtools.android.room.sqliteorg.SqliteOrgDatabaseUtil
import org.dbtools.android.room.sqliteorg.SqliteOrgSQLiteOpenHelperFactory
import org.dbtools.android.room.toLiveData
import org.dbtools.android.room.util.DatabaseUtil
import org.dbtools.sample.roomsqlite.R
import org.dbtools.sample.roomsqlite.databinding.ActivityMainBinding
import org.dbtools.sample.roomsqlite.model.db.main.MainDatabase
import org.dbtools.sample.roomsqlite.model.db.main.MainDatabaseRepository
import org.dbtools.sample.roomsqlite.model.db.main.individual.Individual
import org.dbtools.sample.roomsqlite.model.db.main.individual.IndividualDao
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalDateTime
import timber.log.Timber
import kotlin.coroutines.experimental.coroutineContext

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private lateinit var mainDatabase: MainDatabase
    private val mainDatabaseRepository by lazy { MainDatabaseRepository(application) }
    private lateinit var individualDao: IndividualDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        mainDatabase = Room.databaseBuilder(application, MainDatabase::class.java, MainDatabase.DATABASE_NAME)
                .openHelperFactory(SqliteOrgSQLiteOpenHelperFactory(
                    password = "", // set a password (optional)
                    libraryLoaderBlock = {
                        SqliteOrgSQLiteOpenHelperFactory.loadSqliteLibrary() // NOTE! This line is REQUIRED to use SqliteOrgSQLiteOpenHelperFactory (default if libraryLoaderBlock param is not included)
                        // System.loadLibrary("my_custom_library") // load your custom tokenizer here
                    },
                    postDatabaseCreateBlock = { sqliteDatabase ->
                        // do post core org.sqlite.database.sqlite.SQLiteDatabase creation work here

                        // examples
                        // sqliteDatabase.loadExtension("...")
                        // sqliteDatabase.registerTokenizer("...")
                    }
                ))
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
        binding.testRoomLiveDataButton.setOnClickListener { testRoomLiveData() }
        binding.testMergeDatabaseButton.setOnClickListener { testMergeDatabase() }
        binding.logIndividualsButton.setOnClickListener { showAllIndividuals() }
        binding.deleteAllIndividualsButton.setOnClickListener { deleteAllIndividuals() }
        binding.validateDatabaseButton.setOnClickListener { testValidateDatabase() }
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
    private fun insertIndividual() = launch(UI) {
        val count = withContext(CommonPool) {
            individualDao.insert(createIndividual("Jeff", "Campbell"))
//            individualDao.insert(createIndividual("Tanner", "Campbell", LocalDate.of(1970, 2, 23), "555-555-0002"))
//            individualDao.insert(createIndividual("Ty", "Campbell", LocalDate.of(1970, 2, 13), "555-555-0005"))
//            individualDao.insert(createIndividual("Kylee", "Campbell", LocalDate.of(1970, 2, 9), "555-555-0001"))
//            individualDao.insert(createIndividual("Allie", "Campbell", LocalDate.of(1970, 2, 7), "555-555-0003"))
//            individualDao.insert(createIndividual("Haley", "Campbell", LocalDate.of(1970, 2, 28), "555-555-0004"))

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

        if (!mainDatabaseA.validDatabaseFile("a")) {
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

    private fun testMergeDatabase() = launch {
        // copy the test database from assets
        val mergeDatabase1 = DatabaseUtil.copyDatabaseFromAssets(this@MainActivity, "merge1", true)
        val mergeDatabase2 = DatabaseUtil.copyDatabaseFromAssets(this@MainActivity, "merge2", true)

        mainDatabase.mergeDataFromOtherDatabase(mergeDatabase1)
        mainDatabase.mergeDataFromOtherDatabase(mergeDatabase2)

        showMainDatabaseInfo("merge", mainDatabase)
    }

    private fun testValidateDatabase() {
        val database1 = DatabaseUtil.copyDatabaseFromAssets(this@MainActivity, "merge1", true)

        val success = SqliteOrgDatabaseUtil.validDatabaseFile(database1.absolutePath)
        Toast.makeText(this, "Database Valid: [$success]", Toast.LENGTH_SHORT).show()
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

    private fun showAllIndividuals() = launch {
        showMainDatabaseInfo("ShowAll", mainDatabase)
    }

    private fun deleteAllIndividuals() = launch {
        individualDao.deleteAll()
    }
}
