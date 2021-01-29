package org.dbtools.android.room.jdbctest.extensions

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

/**
 * The following test cases provide simple examples for how to use the [MainDispatcherSurrogateExtension]
 * with suspending code.
 */
@Suppress("EXPERIMENTAL_API_USAGE")
@ExtendWith(MainDispatcherSurrogateExtension::class)
class MainDispatcherSurrogateExtensionTest {

    private lateinit var subject: Subject

    @BeforeEach
    fun setup() {
        subject = Subject()
    }

    @Test
    fun `launching on Main dispatcher and running suspend fun with delay`() = runBlocking {
        launch(Dispatchers.Main) {
            subject.launchingOnMainDispatcherAndRunningSuspendFunWithDelay()

        }

        return@runBlocking
    }

    @Test
    fun `calling suspend fun that uses withContext to the IO Dispatcher`() = runBlocking {
        println("Call directly from runBlocking:")
        subject.suspendFunWithContextToIODispatcher()

        println("Call from Main Dispatcher:")
        launch(Dispatchers.Main) {
            subject.suspendFunWithContextToIODispatcher()
        }

        return@runBlocking
    }

    @Test
    fun `calling suspend fun with internal await and join`() = runBlocking {
        subject.complexCoroutineCalling(this)
        return@runBlocking
    }

    class Subject {
        suspend fun launchingOnMainDispatcherAndRunningSuspendFunWithDelay() {
            println("Delaying foo 2 seconds...")
            delay(2000)
            println("foo is now finished")
        }

        suspend fun suspendFunWithContextToIODispatcher() {
            println("We are on the calling thread")
            withContext(Dispatchers.IO) {
                println("Doing some proccessing on IO thread")
                delay(2000)
                println("Done processing")
            }
            println("Back on the calling thread")
        }

        suspend fun complexCoroutineCalling(scope: CoroutineScope) {
            println("Step 1")
            val deferredOne = scope.async(Dispatchers.IO) {
                println("async one...")
                delay(3000)
            }
            val deferredTwo = scope.async(Dispatchers.Default) {
                println("async two...")
                delay(2000)
            }

            scope.launch(Dispatchers.Main) {
                println("job one...")
                delay(500)
            }.join()
            println("Job one complete... no longer blocking...")

            deferredOne.await()
            println("async one complete... no longer blocking...")

            deferredTwo.await()
            println("async two complete... no longer blocking...")

        }
    }
}