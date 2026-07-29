package frontend

import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import kotlinx.browser.window

actual class PlatformAtomicBoolean actual constructor(initial: Boolean) {
    private var value = initial

    actual fun get(): Boolean = value

    actual fun set(value: Boolean) {
        this.value = value
    }
}

actual fun <T> platformSynchronized(lock: Any, block: () -> T): T = block()

actual fun startComposeEmulatorLoop(
    frameNanos: Long,
    enableFrameLimit: Boolean,
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

            if (enableFrameLimit) {
                val result = step()
                if (result.quitRequested) {
                    active = false
                    onQuit()
                    return
                }
                frames++
            } else {
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
