package frontend

import co.touchlab.kermit.Logger
import nes.NesMachine

class EmulatorRuntimeHost(
    private val machine: NesMachine,
    audio: AudioPipeline,
    input: EmulatorInput,
) : AutoCloseable {
    val frameBuffer = SharedFrameBuffer()
    val lock = Any()

    private val runtime = EmulatorRuntime(machine, audio, input, frameBuffer)
    private var loop: ComposeEmulatorLoop? = null
    private var paused = false

    fun start(
        onFps: (Int) -> Unit,
        onQuit: () -> Unit,
        onError: (Throwable) -> Unit = { error -> log.e(error) { "Emulator loop failed" } },
    ) {
        check(loop == null) { "Emulator runtime host is already started" }
        loop = startPlatformEmulatorLoop(
            frameNanos = {
                platformSynchronized(lock) {
                    machine.timing.frameNanos
                }
            },
            step = {
                platformSynchronized(lock) {
                    if (paused) EmulatorStepResult(frameRendered = false, quitRequested = false) else runtime.step()
                }
            },
            onFps = onFps,
            onQuit = onQuit,
            onError = onError,
        )
    }

    fun stop() {
        loop?.close()
        loop = null
    }

    fun pause() {
        platformSynchronized(lock) {
            if (!paused) {
                paused = true
                runtime.pause()
            }
        }
    }

    fun resume() {
        platformSynchronized(lock) {
            paused = false
        }
    }

    override fun close() {
        stop()
        runtime.close()
    }
}

expect fun <T> platformSynchronized(lock: Any, block: () -> T): T

interface ComposeEmulatorLoop : AutoCloseable

expect fun startPlatformEmulatorLoop(
    frameNanos: () -> Long,
    step: () -> EmulatorStepResult,
    onFps: (Int) -> Unit,
    onQuit: () -> Unit,
    onError: (Throwable) -> Unit,
): ComposeEmulatorLoop

private val log = Logger.withTag("EmulatorRuntimeHost")
