package org.lasantha.fxplanets

import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicInteger

class GameThreadFactory(prefix: String = "p") : ThreadFactory  {
    companion object {
        private val poolNumber = AtomicInteger(1)
    }

    private val group = ThreadGroup("game")
    private val threadNumber = AtomicInteger(1)
    private val namePrefix = "$prefix${poolNumber.getAndIncrement()}-t"

    override fun newThread(r: Runnable): Thread {
        val name = namePrefix + threadNumber.getAndIncrement()
        val t = Thread(group, r, name, 0)
        if (t.isDaemon) {
            t.setDaemon(false)
        }
        if (t.priority != Thread.NORM_PRIORITY) {
            t.setPriority(Thread.NORM_PRIORITY)
        }
        return t
    }
}