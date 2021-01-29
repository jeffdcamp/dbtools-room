package org.dbtools.android.room.jdbctest.extensions

import android.annotation.SuppressLint
import androidx.arch.core.executor.ArchTaskExecutor
import androidx.arch.core.executor.TaskExecutor
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext

/**
 * A JUnit5 Test Extension that swaps the background executor used by the Architecture Components with a
 * different one which executes each task synchronously.
 *
 * NOTE: This is equivalent to InstantTaskExecutorRule if you were using JUnit4.
 *
 * ## Usage:
 * ```
 * @ExtendWith(InstantTaskExecutorExtension::class)
 * class MyTestClass
 * ```
 *
 * ## Example Use-Case:
 * When testing code that uses LiveData, you will want to use this extension. Otherwise you may get an error
 * along the lines of "Method getMainLooper in android.os.Looper not mocked". Such a case might be a LiveData
 * postValue. This resolves the issue by forcing the executor to run on the main thread synchronously.
 */
@SuppressLint("RestrictedApi")
class InstantTaskExecutorExtension : BeforeEachCallback, AfterEachCallback {

    override fun beforeEach(context: ExtensionContext?) {
        ArchTaskExecutor.getInstance().setDelegate(object : TaskExecutor() {
            override fun executeOnDiskIO(runnable: Runnable) {
                runnable.run()
            }

            override fun postToMainThread(runnable: Runnable) {
                runnable.run()
            }

            override fun isMainThread(): Boolean {
                return true
            }
        })
    }

    override fun afterEach(context: ExtensionContext?) {
        ArchTaskExecutor.getInstance().setDelegate(null)
    }
}