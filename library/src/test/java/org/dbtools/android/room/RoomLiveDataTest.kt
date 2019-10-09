package org.dbtools.android.room

import android.app.Application
import androidx.lifecycle.Observer
import androidx.room.Room
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.dbtools.android.room.database.Foo
import org.dbtools.android.room.database.TestDatabase
import org.dbtools.android.room.jdbc.JdbcSQLiteOpenHelperFactory
import org.dbtools.android.room.util.TestFilesystem
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.extension.RegisterExtension
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import timber.log.Timber

internal class RoomLiveDataTest {

    @JvmField
    @RegisterExtension
    val instantTaskExecutorExtension = InstantTaskExecutorExtension()

    @Mock
    lateinit var application: Application

    @BeforeEach
    fun setUp() {
        Timber.plant(object : Timber.Tree() {
            override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
                val logMessage: String = when {
                    tag != null && tag.isNotEmpty() -> "[$tag] $message"
                    else -> message
                }

                println(logMessage)
            }

        })
        MockitoAnnotations.initMocks(this)

        TestFilesystem.deleteFilesystem()

        Mockito.doReturn(TestFilesystem.INTERNAL_FILES_DIR).whenever(application).filesDir


    }

    @Test
    fun toLiveData() = runBlocking {
        withTimeout(10000L) {
            val database = Room.databaseBuilder(application, TestDatabase::class.java, "test.db")
                .allowMainThreadQueries()
                .setTransactionExecutor(TestDatabase.transactionExecutor)
                .openHelperFactory(JdbcSQLiteOpenHelperFactory(TestFilesystem.INTERNAL_DATABASES_DIR_PATH))
                .fallbackToDestructiveMigration()
                .build()

            val liveData = RoomLiveData.toLiveData(listOf(database.tableChangeReferences("Foo"))) {
                delay(100)
                database.fooDao.findAll()
            }
            val max = 2
            val observer = object : Observer<List<Foo>> {
                override fun onChanged(it: List<Foo>?) {
                    println(it)
                    if (it?.size == max) {
                        liveData.removeObserver(this)
                    }
                }
            }
            liveData.observeForever(observer)
            val deferreds = (1..max).map {
                async {
                    database.fooDao.insert(Foo(value = it))
                }
            }
            deferreds.awaitAll()
            while (liveData.hasActiveObservers()) {
                delay(100)
            }
        }
    }
}