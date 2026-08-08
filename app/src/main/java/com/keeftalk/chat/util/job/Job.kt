package com.keeftalk.chat.util.job

import java.util.concurrent.TimeUnit

interface Job {
    /**
     * The actual work to be performed.
     * Returns SUCCESS to delete, FAILURE to retry.
     */
    fun onRun(): JobResult

    /**
     * Called when the job is being enqueued.
     */
    fun onAdded() {}

    /**
     * Called when the job is being retried.
     */
    fun getNextBackoff(): Long = TimeUnit.SECONDS.toMillis(5)
}
