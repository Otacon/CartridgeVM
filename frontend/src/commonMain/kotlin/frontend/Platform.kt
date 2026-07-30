package frontend

import androidx.compose.ui.input.key.KeyEvent
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

expect class PlatformKeyboardInput(controller: NesController) : EmulatorInput {
    fun onKeyEvent(event: KeyEvent): Boolean

    fun releaseAll()

    override fun poll()
}

expect class PlatformControllerInput(controller: NesController) : EmulatorInput {
    override fun poll()
}
