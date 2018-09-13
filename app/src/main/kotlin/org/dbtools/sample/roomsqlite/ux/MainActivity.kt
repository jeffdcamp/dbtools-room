package org.dbtools.sample.roomsqlite.ux

import android.arch.lifecycle.Observer
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.Toast
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.withContext
import org.dbtools.android.room.sqliteorg.SqliteOrgDatabaseUtil
import org.dbtools.android.room.util.DatabaseUtil
import org.dbtools.sample.roomsqlite.R
import org.dbtools.sample.roomsqlite.databinding.ActivityMainBinding
import org.dbtools.sample.roomsqlite.model.repository.IndividualRepository

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val individualRepository by lazy { IndividualRepository(application) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        individualRepository.init()


        binding.createButton.setOnClickListener { insertIndividual() }
        binding.updateButton.setOnClickListener { updateLastIndividual() }
        binding.deleteButton.setOnClickListener { deleteLastIndividual() }
        binding.showButton.setOnClickListener { showLastIndividualName() }
        binding.testDatabaseRepositoryButton.setOnClickListener { testDatabaseRepository() }
        binding.testRoomLiveDataButton.setOnClickListener { testRoomLiveData() }
        binding.testMergeDatabaseButton.setOnClickListener { testMergeDatabase() }
        binding.logIndividualsButton.setOnClickListener { individualRepository.showAllIndividuals() }
        binding.deleteAllIndividualsButton.setOnClickListener { individualRepository.deleteAllIndividuals() }
        binding.validateDatabaseButton.setOnClickListener { testValidateDatabase() }
    }

    private fun insertIndividual() = launch(UI) {
        val count = withContext(CommonPool) {
            individualRepository.addIndividual("Jeff", "Campbell")
            return@withContext individualRepository.getIndividualCount()
        }

        Toast.makeText(this@MainActivity, "Individual Count: $count", Toast.LENGTH_SHORT).show()
    }

    private fun deleteLastIndividual() = launch(UI) {
        if (!hasRecords()) {
            return@launch
        }

        val deleteCount = withContext(coroutineContext + CommonPool) {
            individualRepository.deleteLastIndividual()
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
            individualRepository.updateLastIndividualName()
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
            return@withContext individualRepository.getLastIndividualFirstName()
        }
        Toast.makeText(this@MainActivity, "Last Individual First Name: $firstName", Toast.LENGTH_SHORT).show()
    }


    private suspend fun hasRecords(): Boolean {
        return withContext(CommonPool) {
            val count = individualRepository.findCount()

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
        individualRepository.validateDatabases()
    }

    private fun testRoomLiveData() = launch {
        // check to make sure there is individuals
        if (!hasRecords()) {
            return@launch
        }

        // Create LiveData
        val liveData = individualRepository.findNumberByIdLiveData()

        // OBSERVE and show last number
        liveData.observe(this@MainActivity, Observer { data ->
            data ?: return@Observer
            Toast.makeText(this@MainActivity, "Last Number Set: $data", Toast.LENGTH_SHORT).show()
        })

        // CHANGE NUMBER
        delay(1000)
        val lastNumber = individualRepository.getLastIndividualNumber()
        individualRepository.updateLastIndividualNumber(1)


        // RESTORE
        delay(1000)
        individualRepository.updateLastIndividualNumber(lastNumber)
    }

    private fun testMergeDatabase() = launch {
        individualRepository.mergeDatabases()
    }

    private fun testValidateDatabase() {
        val database1 = DatabaseUtil.copyDatabaseFromAssets(this@MainActivity, "merge1", true)

        val success = SqliteOrgDatabaseUtil.validDatabaseFile(database1.absolutePath)
        Toast.makeText(this, "Database Valid: [$success]", Toast.LENGTH_SHORT).show()
    }
}
