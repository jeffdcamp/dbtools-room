package org.dbtools.sample.roomsqlite.datasource.database.main.foo

import android.app.Application
import android.arch.core.executor.testing.InstantTaskExecutorRule
import android.arch.lifecycle.Observer
import android.arch.persistence.room.Room
import org.dbtools.android.room.jdbc.JdbcSQLiteOpenHelperFactory
import org.dbtools.sample.roomsqlite.datasource.database.main.MainDatabase
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations
import java.io.File

@Suppress("UNCHECKED_CAST")
@RunWith(JUnit4::class)
class FooDaoTest {

    @Mock
    lateinit var application: Application
    @Captor
    lateinit var fooListCaptor1: ArgumentCaptor<List<Foo>>
    @Captor
    lateinit var fooListCaptor2: ArgumentCaptor<List<Foo>>

    lateinit var mainDatabase: MainDatabase
    lateinit var fooDao: FooDao

    @Suppress("unused") // Required to run test on main thread
    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)

        val dbFile = File("build/test-db/DynamicQueryTest.db")
        if (dbFile.exists()) {
            dbFile.delete()
        }

        mainDatabase = Room.databaseBuilder(application, MainDatabase::class.java, "DynamicQueryTest.db")
                .allowMainThreadQueries()
                .openHelperFactory(JdbcSQLiteOpenHelperFactory("build/test-db"))
                .fallbackToDestructiveMigration()
                .build()

        fooDao = mainDatabase.fooDao()
    }

    @After
    fun tearDown() {
        mainDatabase.close()
    }

    @Test
    fun filterAll() {
        val foo1 = Foo().apply {
            name = "Fooy McFooface 1"
            location = Location().apply {
                latitude = 0.0
                longitude = 0.0
            }
            enabled = true
        }

        val foo2 = Foo().apply {
            name = "Fooy McFooface 2"
            location = Location().apply {
                latitude = 2.0
                longitude = 2.0
            }
            enabled = true
        }
        val foo3 = Foo().apply {
            name = "Fooy McFooface 3"
            location = Location().apply {
                latitude = -2.0
                longitude = 2.0
            }
            enabled = false
        }
        val foo4 = Foo().apply {
            name = "Fooy McFooface 4"
            location = Location().apply {
                latitude = 0.5
                longitude = -0.5
            }
            enabled = false
        }

        val observer1 = mock(Observer::class.java) as Observer<List<Foo>>
        val observer2 = mock(Observer::class.java) as Observer<List<Foo>>

        val notFiltered = fooDao.filterAll()
        val filtered = fooDao.filterAll(enabled = true)
        notFiltered.observeForever(observer1)
        filtered.observeForever(observer2)

        verify(observer1).onChanged(fooListCaptor1.capture())
        assertEquals("EmptyList1", true, fooListCaptor1.value.isEmpty())
        verify(observer2).onChanged(fooListCaptor2.capture())
        assertEquals("EmptyList2", true, fooListCaptor2.value.isEmpty())

        foo1.id = fooDao.insert(foo1)
        foo2.id = fooDao.insert(foo2)

        verify(observer1, times(3)).onChanged(fooListCaptor1.capture())
        verify(observer2, times(3)).onChanged(fooListCaptor2.capture())
        assertEquals("List size 2", 2, fooListCaptor1.value.size)
        assertEquals("List size 2", 2, fooListCaptor2.value.size)
        assertEquals("NonFiltered Foo1", foo1, fooListCaptor1.value[0])
        assertEquals("NonFiltered Foo2", foo2, fooListCaptor1.value[1])
        assertEquals("Filtered Foo1", foo1, fooListCaptor2.value[0])
        assertEquals("Filtered Foo2", foo2, fooListCaptor2.value[1])

        foo3.id = fooDao.insert(foo3)
        foo4.id = fooDao.insert(foo4)

        verify(observer1, times(5)).onChanged(fooListCaptor1.capture())
        verify(observer2, times(5)).onChanged(fooListCaptor2.capture())
        assertEquals("List size 4", 4, fooListCaptor1.value.size)
        assertEquals("List size 2", 2, fooListCaptor2.value.size)
        assertEquals("NonFiltered Foo1", foo1, fooListCaptor1.value[0])
        assertEquals("NonFiltered Foo2", foo2, fooListCaptor1.value[1])
        assertEquals("NonFiltered Foo3", foo3, fooListCaptor1.value[2])
        assertEquals("NonFiltered Foo4", foo4, fooListCaptor1.value[3])
        assertEquals("Filtered Foo1", foo1, fooListCaptor2.value[0])
        assertEquals("Filtered Foo2", foo2, fooListCaptor2.value[1])

        fooDao.deleteById(foo2.id)
        fooDao.delete(foo3)

        verify(observer1, times(7)).onChanged(fooListCaptor1.capture())
        verify(observer2, times(7)).onChanged(fooListCaptor2.capture())
        assertEquals("List size 2", 2, fooListCaptor1.value.size)
        assertEquals("List size 1", 1, fooListCaptor2.value.size)
        assertEquals("NonFiltered Foo1", foo1, fooListCaptor1.value[0])
        assertEquals("NonFiltered Foo4", foo4, fooListCaptor1.value[1])
        assertEquals("Filtered Foo1", foo1, fooListCaptor2.value[0])
    }

    @Test
    fun findFirst() {
        val foo1 = Foo().apply {
            name = "Fooy McFooface 1"
            location = Location().apply {
                latitude = 0.0
                longitude = 0.0
            }
            enabled = true
        }

        val foo2 = Foo().apply {
            name = "Fooy McFooface 2"
            location = Location().apply {
                latitude = 2.0
                longitude = 2.0
            }
            enabled = true
        }
        val foo3 = Foo().apply {
            name = "Fooy McFooface 3"
            location = Location().apply {
                latitude = -2.0
                longitude = 2.0
            }
            enabled = false
        }
        val foo4 = Foo().apply {
            name = "Fooy McFooface 4"
            location = Location().apply {
                latitude = 0.5
                longitude = -0.5
            }
            enabled = false
        }

        fooDao.insert(foo1)
        fooDao.insert(foo2)
        fooDao.insert(foo3)
        fooDao.insert(foo4)

        val startLocation = Location().apply {
            latitude = -1.0
            longitude = -1.0
        }
        val endLocation = Location().apply {
            latitude = 1.0
            longitude = 1.0
        }
        assertEquals("No Filters", foo1.name, fooDao.findFirst()?.name)
        assertEquals("Enabled True", foo1.name, fooDao.findFirst(enabled = true)?.name)
        assertEquals("Enabled False", foo3.name, fooDao.findFirst(enabled = false)?.name)
        assertEquals("Location", foo1.name, fooDao.findFirst(startLocation = startLocation, endLocation = endLocation)?.name)
        assertEquals("Location Enabled False", foo4.name, fooDao.findFirst(startLocation = startLocation, endLocation = endLocation, enabled = false)?.name)
        assertNull("No Values", fooDao.findFirst(startLocation = endLocation, endLocation = startLocation))
    }

}