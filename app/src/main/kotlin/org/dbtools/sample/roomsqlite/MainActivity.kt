package org.dbtools.sample.roomsqlite

import android.arch.persistence.room.Room
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.withContext
import org.dbtools.android.room.sqliteorg.SqliteOrgSQLiteOpenHelperFactory
import org.dbtools.sample.roomsqlite.datasource.database.main.MainDatabase
import org.dbtools.sample.roomsqlite.datasource.database.main.individual.Individual
import org.dbtools.sample.roomsqlite.datasource.database.main.individual.IndividualDao
import java.util.Date
import java.util.GregorianCalendar

class MainActivity : AppCompatActivity() {

    private lateinit var mainDatabase: MainDatabase
    private lateinit var individualDao: IndividualDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

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

        createButton.setOnClickListener { createIndividual() }
        updateButton.setOnClickListener { updateLastIndividual() }
        deleteButton.setOnClickListener { deleteLastIndividual() }
        showButton.setOnClickListener { showLastIndividualName() }
    }

    private fun createIndividual() = launch(UI) {
        val count = withContext(CommonPool) {
            val individual = Individual()
            individual.firstName = "Jeff"
            individual.lastName = "Campbell"
            individual.sampleDateTime = Date()
            individual.birthDate = GregorianCalendar(1970, 1, 1).time
            individual.lastModified = Date()
            individual.number = 1234
            individual.phone = "555-555-1234"
            individual.email = "test@test.com"
            individual.amount1 = 19.95f
            individual.amount2 = 1000000000.25
            individual.enabled = true

            individualDao.insert(individual)

            return@withContext individualDao.findCount()
        }

        Toast.makeText(this@MainActivity, "Individual Count: " + count, Toast.LENGTH_SHORT).show()
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
        Toast.makeText(this@MainActivity, "Last Individual First Name: " + firstName, Toast.LENGTH_SHORT).show()
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
}
