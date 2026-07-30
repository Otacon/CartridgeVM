package frontend

import nes.NesMachine

class EmulatorRuntime(
    private val machine: NesMachine,
    private val audio: AudioPipeline,
    private val input: EmulatorInput,
    private val video: VideoOutput,
) {

    fun step(): EmulatorStepResult {
        input.poll()
        if (input.consumeReset()) {
            machine.reset()
        }

        var frameRendered = false
        if (machine.isPoweredOn.value) {
            machine.runUntilFrame(input::poll)
            audio.submit(machine.apu.samples, machine.apu.sampleCount)
            video.submit(machine.ppu.framebuffer)
            frameRendered = true
        }
        return EmulatorStepResult(frameRendered, input.quitRequested())
    }

    fun pause() {
        input.pause()
        audio.pause()
    }

    fun close() {
        input.close()
    }
}

data class EmulatorStepResult(
    val frameRendered: Boolean,
    val quitRequested: Boolean,
)
