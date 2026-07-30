package frontend

import java.util.concurrent.locks.LockSupport

class FramePacer(private var frameNanos: Long) {
    private var nextDeadline = System.nanoTime() + frameNanos

    fun setFrameNanos(value: Long) {
        if (value == frameNanos) return
        frameNanos = value
        nextDeadline = System.nanoTime() + frameNanos
    }

    fun waitForNextFrame() {
        var now = System.nanoTime()
        if (now - nextDeadline > frameNanos * 4) {
            nextDeadline = now + frameNanos
        }
        while (true) {
            val remaining = nextDeadline - now
            if (remaining <= 0) break
            when {
                remaining > 2_000_000L -> LockSupport.parkNanos(remaining - 1_000_000L)
                remaining > 100_000L -> LockSupport.parkNanos(remaining / 2)
                else -> Thread.onSpinWait()
            }
            now = System.nanoTime()
        }
        nextDeadline += frameNanos
    }
}
