package frontend

import nes.input.NesController

interface Renderer {
    fun init(crt: Boolean)

    fun present(framebuffer: IntArray, windowWidth: Int, windowHeight: Int)

    fun close()
}

expect class PlatformRenderer() : Renderer {
    override fun init(crt: Boolean)

    override fun present(framebuffer: IntArray, windowWidth: Int, windowHeight: Int)

    override fun close()
}

expect class PlatformAudioPipeline() : AudioPipeline {
    override fun submit(samples: ShortArray, count: Int)
}

expect class PlatformKeyboardInput(controller: NesController) : BaseEmulatorInput {
    override fun poll()

    override fun quitRequested(): Boolean
}

expect class PlatformControllerInput(controller: NesController) : BaseEmulatorInput {
    override fun poll()

    override fun quitRequested(): Boolean
}
