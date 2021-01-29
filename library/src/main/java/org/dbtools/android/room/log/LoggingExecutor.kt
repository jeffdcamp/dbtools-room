package org.dbtools.android.room.log

import java.util.concurrent.Executor

/**
 * Simple Executor used for logging queries immediately
 */
object LoggingExecutor : Executor {
    override fun execute(command: Runnable?) {
        command?.run()
    }
}