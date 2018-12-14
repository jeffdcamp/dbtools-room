package org.dbtools.sample.roomsqlite.datasource.database.main.individual

import android.app.Application
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.room.Room
import androidx.room.RoomDatabase
import org.dbtools.android.room.jdbc.JdbcSQLiteOpenHelperFactory
import org.dbtools.sample.roomsqlite.model.db.main.MainDatabase
import org.dbtools.sample.roomsqlite.model.db.main.individual.Individual
import org.dbtools.sample.roomsqlite.model.db.main.individual.IndividualDao
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import java.io.File


class IndividualDaoTest {

    @Mock
    lateinit var application: Application

    private lateinit var mainDatabase: MainDatabase
    private lateinit var individualDao: IndividualDao

    @BeforeEach
    fun setup() {
        MockitoAnnotations.initMocks(this)

        val dbFile = "build/test-db/${MainDatabase.DATABASE_NAME}"
        File(dbFile).delete()

        mainDatabase = Room.databaseBuilder(application, MainDatabase::class.java, MainDatabase.DATABASE_NAME)
                .allowMainThreadQueries()
                .openHelperFactory(JdbcSQLiteOpenHelperFactory("build/test-db"))
                .fallbackToDestructiveMigration()
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        println("OnCreate")
                    }

                    override fun onOpen(db: SupportSQLiteDatabase) {
                        println("OnOpen")
                    }
                })
                .build()

        // In Memory Database
//        mainDatabase = Room.inMemoryDatabaseBuilder(application, MainDatabase::class.java)
//                .allowMainThreadQueries()
//                .openHelperFactory(JdbcSQLiteOpenHelperFactory())
//                .build()

        individualDao = mainDatabase.individualDao
    }

    @AfterEach
    fun tearDown() {
        mainDatabase.close()
    }

    @Test
    fun basic() {
        val individual = Individual().apply {
            firstName = "Testy"
            lastName = "McTestFace"
        }

        individualDao.insert(individual)
        val individualId = individualDao.findLastIndividualId()

        if (individualId == null) {
            assertNotNull(individualId)
            return
        }

        assertEquals(1, individualDao.findCount())
        assertEquals("Testy", individualDao.findFirstNameById(individualId))
        assertEquals("Testy", individualDao.findById(individualId)?.firstName)
    }

    @Test
    fun reentrantTransaction() {
        mainDatabase.runInTransaction {
            mainDatabase.runInTransaction {
                individualDao.insert(List(3) {
                    Individual().apply {
                        firstName = "R$it"
                        lastName = "D$it"
                    }
                })
            }
        }
    }
}