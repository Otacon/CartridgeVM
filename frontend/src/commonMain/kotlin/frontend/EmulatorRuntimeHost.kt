package frontend

import co.touchlab.kermit.Logger
import nes.NesMachine

class EmulatorRuntimeHost(
    machine: NesMachine,
    audio: AudioPipeline,
    input: EmulatorInput,
    private val frameNanos: Long,
) : AutoCloseable {
    val frameBuffer = SharedFrameBuffer()
    val lock = Any()

    private val runtime = EmulatorRuntime(machine, audio, input, frameBuffer)
    private val runningFlag = PlatformAtomicBoolean(false)
    private var loop: ComposeEmulatorLoop? = null

    fun setRunning(running: Boolean) {
        runningFlag.set(running)
    }

    fun start(
        onFps: (Int) -> Unit,
        onQuit: () -> Unit,
        onError: (Throwable) -> Unit = { error -> log.e(error) { "Emulator loop failed" } },
    ) {
        check(loop == null) { "Emulator runtime host is already started" }
        loop = startPlatformEmulatorLoop(
            frameNanos = frameNanos,
            step = {
                platformSynchronized(lock) {
                    runtime.step(runningFlag.get())
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

    override fun close() {
        stop()
        runtime.close()
    }
}

expect fun <T> platformSynchronized(lock: Any, block: () -> T): T

expect class PlatformAtomicBoolean(initial: Boolean) {
    fun get(): Boolean

    fun set(value: Boolean)
}

interface ComposeEmulatorLoop : AutoCloseable

expect fun startPlatformEmulatorLoop(
    frameNanos: Long,
    step: () -> EmulatorStepResult,
    onFps: (Int) -> Unit,
    onQuit: () -> Unit,
    onError: (Throwable) -> Unit,
): ComposeEmulatorLoop

private val log = Logger.withTag("EmulatorRuntimeHost")
