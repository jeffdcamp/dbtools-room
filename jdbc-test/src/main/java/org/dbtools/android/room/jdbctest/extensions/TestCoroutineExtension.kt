package org.dbtools.android.room.jdbctest.extensions

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext

/**
 * JUnit5 extension to provide a coroutine scope and delegation for Dispatchers.Main. This will override [Dispatchers.Main]
 * in test situations to use a test coroutine dispatcher scoped to a test coroutine scope.
 *
 * This extension will automatically cleanup and reset for each individual test-case.
 *
 * See "Integrating tests with structured concurrency" at https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-test/
 * Useful when testing ViewModels that utilize viewModelScope
 *
 * ## Usage
 * ```
 * @ExtendWith(TestCoroutineExtension::class)
 * class MyTestClass {
 *
 *      @Test
 *      fun myTestCase() = runBlocking {
 *          // Test suspending code here
 *          return@runBlocking
 *      }
 * }
 * ```
 */
@Suppress("EXPERIMENTAL_API_USAGE")
class TestCoroutineExtension : BeforeEachCallback, AfterEachCallback {

    private val testCoroutineDispatcher = TestCoroutineDispatcher()
    private val testCoroutineScope = TestCoroutineScope(testCoroutineDispatcher)

    override fun beforeEach(context: ExtensionContext?) {
        Dispatchers.setMain(testCoroutineDispatcher)
    }

    override fun afterEach(context: ExtensionContext?) {
        Dispatchers.resetMain()
        testCoroutineScope.cleanupTestCoroutines()
    }
}