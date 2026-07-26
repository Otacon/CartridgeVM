package frontend

import nes.input.NesController

interface Renderer {
    fun init(crt: Boolean)

    fun present(framebuffer: IntArray, windowWidth: Int, windowHeight: Int)

    fun close()
}

expect class PlatformRenderer() : Renderer

expect class PlatformAudioPipeline() : AudioPipeline

expect class PlatformKeyboardInput(controller: NesController) : BaseEmulatorInput

expect class PlatformControllerInput(controller: NesController) : BaseEmulatorInput
