package org.dbtools.sample.roomsqlite.datasource.database.main.individual

import android.app.Application
import android.arch.persistence.room.Room
import org.dbtools.android.room.jdbc.JdbcSQLiteOpenHelperFactory
import org.dbtools.sample.roomsqlite.datasource.database.main.MainDatabase
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations


class IndividualDaoTest {

    @Mock
    lateinit var application: Application

    lateinit var mainDatabase: MainDatabase

    lateinit var individualDao: IndividualDao

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)

        mainDatabase = Room.databaseBuilder(application, MainDatabase::class.java, MainDatabase.DATABASE_NAME)
                .allowMainThreadQueries()
                .openHelperFactory(JdbcSQLiteOpenHelperFactory("app/build/test-db"))
                .fallbackToDestructiveMigration()
                .build()

        // In Memory Database
//        mainDatabase = Room.inMemoryDatabaseBuilder(application, MainDatabase::class.java)
//                .allowMainThreadQueries()
//                .openHelperFactory(JdbcSQLiteOpenHelperFactory())
//                .build()

        individualDao = mainDatabase.individualDao()
    }

    @After
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

        assertEquals(1, individualDao.findCount())
        assertEquals("Testy", individualDao.findFirstNameById(individualId))
        assertEquals("Testy", individualDao.findById(individualId)?.firstName)
    }
}