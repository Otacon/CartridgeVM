package frontend

import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import kotlinx.browser.window

actual fun <T> platformSynchronized(lock: Any, block: () -> T): T = block()

actual fun startPlatformEmulatorLoop(
    frameNanos: Long,
    step: () -> EmulatorStepResult,
    onFps: (Int) -> Unit,
    onQuit: () -> Unit,
    onError: (Throwable) -> Unit,
): ComposeEmulatorLoop {
    var active = true
    var frameStart = 0.0
    var frames = 0
    var fpsTime = 0.0
    val frameMillis = frameNanos / 1_000_000.0

    fun frame(now: Double) {
        if (!active) return
        try {
            if (frameStart == 0.0) frameStart = now
            if (fpsTime == 0.0) fpsTime = now

            while (now - frameStart >= frameMillis) {
                val result = step()
                if (result.quitRequested) {
                    active = false
                    onQuit()
                    return
                }
                frameStart += frameMillis
                frames++
            }

            if (now - fpsTime >= 1_000.0) {
                onFps(frames)
                frames = 0
                fpsTime = now
            }
            window.requestAnimationFrame(::frame)
        } catch (error: Throwable) {
            active = false
            onError(error)
        }
    }

    window.requestAnimationFrame(::frame)

    return object : ComposeEmulatorLoop {
        override fun close() {
            active = false
        }
    }
}

actual fun DrawScope.drawPlatformRenderer(renderer: PlatformRenderer) {
    renderer.draw(drawContext.canvas.nativeCanvas)
}
