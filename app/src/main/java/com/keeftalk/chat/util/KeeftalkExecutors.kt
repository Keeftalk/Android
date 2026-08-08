package com.keeftalk.chat.util

import android.os.Process
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicInteger

/**
 * Specialized startup and general-purpose executors for Keeftalk.
 * Avoids thread starvation and provides predictable performance.
 */
object KeeftalkExecutors {

    /**
     * UNBOUNDED: For tasks that should never block (e.g. logging, ephemeral events).
     */
    val UNBOUNDED: ExecutorService = Executors.newCachedThreadPool(NumberedThreadFactory("keeftalk-unbounded", Process.THREAD_PRIORITY_BACKGROUND))

    /**
     * BOUNDED: Core pool for general background tasks.
     */
    val BOUNDED: ExecutorService = Executors.newFixedThreadPool(4, NumberedThreadFactory("keeftalk-bounded", Process.THREAD_PRIORITY_BACKGROUND))

    /**
     * SERIAL: SINGLE-THREADED pool for database writes or ordered tasks.
     */
    val SERIAL: ExecutorService = Executors.newSingleThreadExecutor(NumberedThreadFactory("keeftalk-serial", Process.THREAD_PRIORITY_BACKGROUND))

    /**
     * BOUNDED_IO: Specialized pool for startup IO or intensive disk operations.
     */
    val BOUNDED_IO: ExecutorService = newCachedBoundedExecutor(
        name = "keeftalk-io-bounded",
        priority = Process.THREAD_PRIORITY_BACKGROUND,
        minThreads = 1,
        maxThreads = 32,
        timeoutSeconds = 30
    )

    private fun newCachedBoundedExecutor(
        name: String,
        priority: Int,
        minThreads: Int,
        maxThreads: Int,
        timeoutSeconds: Long
    ): ExecutorService {
        val threadPool = ThreadPoolExecutor(
            minThreads, maxThreads, timeoutSeconds, TimeUnit.SECONDS,
            object : LinkedBlockingQueue<Runnable>() {
                override fun offer(r: Runnable): Boolean = if (isEmpty()) super.offer(r) else false
            }, NumberedThreadFactory(name, priority)
        )

        threadPool.rejectedExecutionHandler = RejectedExecutionHandler { runnable, executor ->
            try {
                executor.queue.put(runnable)
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
            }
        }

        return threadPool
    }

    class NumberedThreadFactory(private val baseName: String, private val priority: Int) : ThreadFactory {
        private val counter = AtomicInteger()

        override fun newThread(r: Runnable): Thread {
            return object : Thread(r, "$baseName-${counter.getAndIncrement()}") {
                override fun run() {
                    Process.setThreadPriority(priority)
                    super.run()
                }
            }
        }
    }
}
