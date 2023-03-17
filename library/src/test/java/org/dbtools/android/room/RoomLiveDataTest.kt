package org.dbtools.android.room

import android.content.Context
import androidx.lifecycle.Observer
import androidx.room.Room
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.dbtools.android.room.database.Foo
import org.dbtools.android.room.database.TestDatabase
import org.dbtools.android.room.jdbc.JdbcSQLiteOpenHelperFactory
import org.dbtools.android.room.util.TestFilesystem
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.RegisterExtension
import timber.log.Timber

@ExtendWith(MockKExtension::class)
internal class RoomLiveDataTest {

    @JvmField
    @RegisterExtension
    val instantTaskExecutorExtension = InstantTaskExecutorExtension()

    @MockK
    lateinit var context: Context

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

        TestFilesystem.deleteFilesystem()

        every { context.filesDir } returns TestFilesystem.INTERNAL_FILES_DIR
    }

    @Test
    fun toLiveData() = runBlocking {
        withTimeout(10000L) {
            val database = Room.databaseBuilder(context, TestDatabase::class.java, "test.db")
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
                override fun onChanged(it: List<Foo>) {
                    println(it)
                    if (it.size == max) {
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