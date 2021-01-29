package org.dbtools.android.room.jdbctest.extensions

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import java.util.concurrent.Executors

/**
 * JUnit5 extension to provide a delegation for Dispatchers.Main. This will override [Dispatchers.Main]
 * in test situations to use a single thread executor.
 *
 * This extension will automatically reset for each individual test-case.
 *
 * When testing coroutines that use structured concurrency such as inside ViewModels, like viewModelScope, use TestCoroutineExtension instead.
 *
 * ## Usage
 * ```
 * @ExtendWith(MainDispatcherSurrogateExtension::class)
 * class MyTestClass {
 *
 *      @Test
 *      fun myTestCase() = runBlocking {
 *          // Test suspending code here
 *          return@runBlocking
 *      }
 * }
 * ```
 *
 * When using this extension you should test your suspending code within a `runBlocking` block.
 * As of `org.jetbrains.kotlinx:kotlinx-coroutines-test:1.2.1`, there are issues with `runBlockingTest`
 * that cause complications with code thay calls `delay`. To resolve these complications, it would
 * require undesired refactoring of the code under test.
 */
@Suppress("EXPERIMENTAL_API_USAGE")
class MainDispatcherSurrogateExtension : BeforeEachCallback, AfterEachCallback {

    private val mainThreadSurrogate = Executors.newSingleThreadExecutor().asCoroutineDispatcher()

    override fun beforeEach(context: ExtensionContext?) {
        Dispatchers.setMain(mainThreadSurrogate)
    }

    override fun afterEach(context: ExtensionContext?) {
        Dispatchers.resetMain()
        mainThreadSurrogate.close()
    }
}