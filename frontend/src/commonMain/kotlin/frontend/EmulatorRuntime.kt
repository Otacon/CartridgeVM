package frontend

import nes.NesMachine

class EmulatorRuntime(
    private val machine: NesMachine,
    private val audio: AudioPipeline,
    private val input: EmulatorInput,
    private val video: VideoOutput,
) {
    private var runState = EmulatorRunState.Running

    fun step(running: Boolean): EmulatorStepResult {
        input.poll()
        if (input.consumePause()) {
            runState = if (runState == EmulatorRunState.Running) EmulatorRunState.Paused else EmulatorRunState.Running
        }
        if (input.consumeReset()) {
            machine.reset()
            runState = EmulatorRunState.Running
        }

        var frameRendered = false
        if (running && runState == EmulatorRunState.Running) {
            machine.runUntilFrame(input::poll)
            audio.submit(machine.apu.samples, machine.apu.sampleCount)
            video.submit(machine.ppu.framebuffer)
            frameRendered = true
        }
        return EmulatorStepResult(frameRendered, input.quitRequested())
    }

    fun close() {
        input.close()
    }
}

data class EmulatorStepResult(
    val frameRendered: Boolean,
    val quitRequested: Boolean,
)

private enum class EmulatorRunState { Running, Paused }
