package com.keeftalk.chat.util.job

import android.content.Context
import com.keeftalk.chat.util.KeeftalkExecutors
import com.keeftalk.chat.util.PerformanceProfiler
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages the persistent job queue and background loop.
 */
class JobController private constructor(
    private val storage: JobStorage,
    private val jobInstantiator: JobInstantiator
) {
    private val runningJobs = ConcurrentHashMap<String, Job>()
    private val executor = KeeftalkExecutors.SERIAL

    companion object {
        @Volatile
        private var instance: JobController? = null

        fun init(context: Context, instantiator: JobInstantiator): JobController {
            return instance ?: synchronized(this) {
                val storage = JobStorage(context)
                instance ?: JobController(storage, instantiator).also { instance = it }
            }
        }

        fun getInstance(): JobController {
            return instance ?: throw IllegalStateException("JobController not initialized")
        }
    }

    /**
     * Resets stuck jobs on startup and starts the loop.
     */
    fun start() {
        PerformanceProfiler.logEvent("JobController: Starting loop")
        executor.execute {
            storage.resetRunningJobs()
            wakeUp()
        }
    }

    /**
     * Adds a new job to the persistent queue.
     */
    fun addJob(spec: JobSpec) {
        executor.execute {
            storage.insertJob(spec)
            wakeUp()
        }
    }

    /**
     * Checks for eligible jobs and runs them.
     */
    @Synchronized
    fun wakeUp() {
        executor.execute {
            val now = System.currentTimeMillis()
            val nextJobSpec = storage.getNextEligibleJob(now)
            if (nextJobSpec != null && !runningJobs.containsKey(nextJobSpec.id)) {
                runJob(nextJobSpec)
            }
        }
    }

    private fun runJob(spec: JobSpec) {
        val job = jobInstantiator.create(spec)
        runningJobs[spec.id] = job

        KeeftalkExecutors.BOUNDED.execute {
            try {
                storage.markRunning(spec.id)
                val result = job.onRun()
                if (result == JobResult.SUCCESS) {
                    storage.deleteJob(spec.id)
                } else {
                    storage.updateRetry(spec.id, backoff = job.getNextBackoff())
                }
            } catch (e: Exception) {
                storage.updateRetry(spec.id, backoff = job.getNextBackoff())
            } finally {
                runningJobs.remove(spec.id)
                wakeUp() // Check for the next job
            }
        }
    }
}
