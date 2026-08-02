package frontend

import androidx.compose.ui.input.key.KeyEvent
import nes.input.NesController
import org.jetbrains.skia.Canvas

interface Renderer {
    fun init(crt: Boolean, castShadow: Boolean)

    fun present(frame: VideoFrame, windowWidth: Int, windowHeight: Int)

    fun draw(canvas: Canvas)

    fun close()
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
