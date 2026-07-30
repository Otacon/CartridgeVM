package frontend

import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import java.util.concurrent.atomic.AtomicBoolean

actual fun <T> platformSynchronized(lock: Any, block: () -> T): T = synchronized(lock, block)

actual fun startPlatformEmulatorLoop(
    frameNanos: () -> Long,
    step: () -> EmulatorStepResult,
    onFps: (Int) -> Unit,
    onError: (Throwable) -> Unit,
): ComposeEmulatorLoop {
    val keepRunning = AtomicBoolean(true)
    val thread = Thread({
        val pacer = FramePacer(frameNanos())
        var frames = 0
        var fpsTime = System.nanoTime()

        try {
            while (keepRunning.get()) {
                val result = step()
                if (!result.frameRendered) Thread.sleep(8)
                pacer.setFrameNanos(frameNanos())
                pacer.waitForNextFrame()

                if (result.frameRendered) frames++
                val now = System.nanoTime()
                if (now - fpsTime >= 1_000_000_000L) {
                    onFps(frames)
                    frames = 0
                    fpsTime = now
                }
            }
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
        } catch (error: Throwable) {
            onError(error)
        }
    }, "compose-skiko-emulator")
    thread.isDaemon = true
    thread.start()

    return object : ComposeEmulatorLoop {
        override fun close() {
            keepRunning.set(false)
            thread.interrupt()
            thread.join()
        }
    }
}

actual fun DrawScope.drawPlatformRenderer(renderer: PlatformRenderer) {
    renderer.draw(drawContext.canvas.nativeCanvas)
}
