package org.dbtools.sample.roomsqlite.datasource.database.main.individual

import android.app.Application
import android.arch.persistence.db.SupportSQLiteDatabase
import android.arch.persistence.room.Room
import android.arch.persistence.room.RoomDatabase
import org.dbtools.android.room.jdbc.JdbcSQLiteOpenHelperFactory
import org.dbtools.sample.roomsqlite.model.db.main.MainDatabase
import org.dbtools.sample.roomsqlite.model.db.main.individual.Individual
import org.dbtools.sample.roomsqlite.model.db.main.individual.IndividualDao
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import java.io.File


class IndividualDaoTest {

    @Mock
    lateinit var application: Application

    lateinit var mainDatabase: MainDatabase

    lateinit var individualDao: IndividualDao

    @Before
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