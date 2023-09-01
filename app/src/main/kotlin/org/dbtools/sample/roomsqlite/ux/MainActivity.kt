package org.dbtools.sample.roomsqlite.ux

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import co.touchlab.kermit.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.dbtools.android.room.sqliteorg.SqliteOrgDatabaseUtil
import org.dbtools.android.room.util.DatabaseUtil
import org.dbtools.sample.roomsqlite.databinding.ActivityMainBinding
import org.dbtools.sample.roomsqlite.model.repository.IndividualRepository
import kotlin.coroutines.CoroutineContext

class MainActivity : AppCompatActivity(), CoroutineScope {
    private lateinit var binding: ActivityMainBinding

    private val individualRepository by lazy { IndividualRepository(application) }

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        individualRepository.init()


        binding.createButton.setOnClickListener { insertIndividual() }
        binding.updateButton.setOnClickListener { updateLastIndividual() }
        binding.deleteButton.setOnClickListener { deleteLastIndividual() }
        binding.showButton.setOnClickListener { showLastIndividualName() }
        binding.testDatabaseRepositoryButton.setOnClickListener { testDatabaseRepository() }
        binding.testRoomLiveDataButton.setOnClickListener { testRoomLiveData() }
        binding.testRoomFlowButton.setOnClickListener { testRoomFlow() }
        binding.testMergeDatabaseButton.setOnClickListener { testMergeDatabase() }
        binding.logIndividualsButton.setOnClickListener { individualRepository.showAllIndividuals() }
        binding.deleteAllIndividualsButton.setOnClickListener { individualRepository.deleteAllIndividuals() }
        binding.validateGoodDatabaseButton.setOnClickListener { testValidateGoodDatabase() }
        binding.validateBadDatabaseButton.setOnClickListener { testValidateBadDatabase() }
        binding.testViewsDatabaseButton.setOnClickListener { testViewsDatabase() }
    }

    private fun insertIndividual() = launch {
        val count = withContext(Dispatchers.IO) {
            individualRepository.addIndividual("Jeff", "Campbell")
            return@withContext individualRepository.getIndividualCount()
        }

        Toast.makeText(this@MainActivity, "Individual Count: $count", Toast.LENGTH_SHORT).show()
    }

    private fun deleteLastIndividual() = launch {
        if (!hasRecords()) {
            return@launch
        }

        val deleteCount = withContext(Dispatchers.IO) {
            individualRepository.deleteLastIndividual()
        }

        if (deleteCount > 0) {
            Toast.makeText(this@MainActivity, "Last individual deleted", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateLastIndividual() = launch {
        // check to make sure there is individuals
        if (!hasRecords()) {
            return@launch
        }

        val updated = withContext(Dispatchers.IO) {
            individualRepository.updateLastIndividualName()
        }

        if (updated) {
            Toast.makeText(this@MainActivity, "Last individual updated", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this@MainActivity, "Failed to find last individual", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showLastIndividualName() = launch {
        // check to make sure there is individuals
        if (!hasRecords()) {
            return@launch
        }

        val firstName = withContext(Dispatchers.IO) {
            return@withContext individualRepository.getLastIndividualFirstName()
        }
        Toast.makeText(this@MainActivity, "Last Individual First Name: $firstName", Toast.LENGTH_SHORT).show()
    }


    private suspend fun hasRecords(): Boolean {
        return withContext(Dispatchers.IO) {
            val count = individualRepository.findCount()

            if (count <= 0) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "No Records exist", Toast.LENGTH_SHORT).show()
                }
                return@withContext false
            } else {
                return@withContext true
            }
        }
    }

    private fun testDatabaseRepository() = launch {
        withContext(Dispatchers.IO) {
            individualRepository.validateDatabases()
        }
    }

    private fun testRoomLiveData() = launch {
        // check to make sure there is individuals
        if (!hasRecords()) {
            Logger.e { "Cannot test.... no individuals" }
            return@launch
        }

        // Create LiveData
        val liveData = individualRepository.findNumberByIdLiveData()

        // OBSERVE and show last number
        Logger.i { "testRoomLiveData(): Initial observe " }
        liveData.observe(this@MainActivity, Observer { data ->
            data ?: return@Observer
            val message = "Last Number Set: $data"
            Logger.i { "testRoomLiveData(): $message " }
            Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
        })

        // CHANGE NUMBER
        withContext(Dispatchers.IO) {
            delay(1000)
            Logger.i { "testRoomLiveData(): change #1 " }
            val lastNumber = individualRepository.getLastIndividualNumber()
            individualRepository.updateLastIndividualNumber(500)

            // RESTORE
            delay(1000)
            Logger.i { "testRoomLiveData(): change #2 " }
            individualRepository.updateLastIndividualNumber(lastNumber)
        }
    }

    private fun testRoomFlow() = launch {
        // check to make sure there is individuals
        if (!hasRecords()) {
            Logger.e { "Cannot test.... no individuals" }
            return@launch
        }

        // Create LiveData
        val flow = individualRepository.findNumberByIdFlow()

        // OBSERVE and show last number
        Logger.i { "testRoomFlow(): Initial collect " }
        val job = lifecycleScope.launch {
            flow.collect { data ->
                val message = "COLLECT / Last Number Set: $data"
                Logger.i { "testRoomFlow(): $message " }
                Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
            }
        }

        // CHANGE NUMBER
        withContext(Dispatchers.IO) {
            delay(1000)
            Logger.i { "testRoomFlow(): change #1 " }
            val lastNumber = individualRepository.getLastIndividualNumber()
            individualRepository.updateLastIndividualNumber(500)

            // RESTORE
            delay(1000)
            Logger.i { "testRoomFlow(): change #2 " }
            individualRepository.updateLastIndividualNumber(lastNumber)
        }

        delay(1000) // allow time for change #2 show

        // cleanup
        job.cancel()
    }

    private fun testMergeDatabase() = launch {
        val results = withContext(Dispatchers.IO) {
            individualRepository.mergeDatabases()
        }

        if (results.success) {
            Logger.i { "Test Merge results: ${results.message}" }
            Toast.makeText(this@MainActivity, results.message, Toast.LENGTH_LONG).show()
        } else {
            Logger.e { "Test Merge results: ${results.message}" }
            Toast.makeText(this@MainActivity, results.message, Toast.LENGTH_LONG).show()
        }
    }

    private fun testValidateGoodDatabase() {
        val database1 = DatabaseUtil.copyDatabaseFromAssets(this@MainActivity, "merge1", true)

        val success = SqliteOrgDatabaseUtil.validateDatabaseFile(database1.absolutePath)
        Toast.makeText(this, "Database Valid: [$success]", Toast.LENGTH_SHORT).show()
    }

    private fun testValidateBadDatabase() {
        val database1 = DatabaseUtil.copyDatabaseFromAssets(this@MainActivity, "baddb", true)

        val success = SqliteOrgDatabaseUtil.validateDatabaseFile(database1.absolutePath)
        Toast.makeText(this, "Database Valid: [$success]", Toast.LENGTH_SHORT).show()
    }

    private fun testViewsDatabase() {
        val results = individualRepository.testViews()
        Toast.makeText(this@MainActivity, "Views test complete. Success = $results", Toast.LENGTH_LONG).show()
    }
}
