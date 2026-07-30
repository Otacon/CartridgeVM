package frontend

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import nes.input.NesController

actual class PlatformKeyboardInput actual constructor(
    private val controller: NesController,
) : EmulatorInput {
    private var pressedButtons = 0

    actual fun onKeyEvent(event: KeyEvent): Boolean {
        val button = event.key.toButton() ?: return false
        val mask = 1 shl button
        when (event.type) {
            KeyEventType.KeyDown -> pressedButtons = pressedButtons or mask
            KeyEventType.KeyUp -> pressedButtons = pressedButtons and mask.inv()
            else -> return false
        }
        return true
    }

    actual fun releaseAll() {
        pressedButtons = 0
    }

    actual override fun poll() {
        if (NesController.BUTTON_A.isPressed()) controller.press(NesController.BUTTON_A)
        if (NesController.BUTTON_B.isPressed()) controller.press(NesController.BUTTON_B)
        if (NesController.BUTTON_SELECT.isPressed()) controller.press(NesController.BUTTON_SELECT)
        if (NesController.BUTTON_START.isPressed()) controller.press(NesController.BUTTON_START)
        if (NesController.BUTTON_UP.isPressed()) controller.press(NesController.BUTTON_UP)
        if (NesController.BUTTON_DOWN.isPressed()) controller.press(NesController.BUTTON_DOWN)
        if (NesController.BUTTON_LEFT.isPressed()) controller.press(NesController.BUTTON_LEFT)
        if (NesController.BUTTON_RIGHT.isPressed()) controller.press(NesController.BUTTON_RIGHT)
    }

    override fun pause() {
        releaseAll()
    }

    override fun close() {
        pressedButtons = 0
    }

    private fun Int.isPressed(): Boolean = (pressedButtons and (1 shl this)) != 0

    private fun Key.toButton(): Int? = when (this) {
        Key.Z -> NesController.BUTTON_A
        Key.X -> NesController.BUTTON_B
        Key.ShiftLeft, Key.ShiftRight -> NesController.BUTTON_SELECT
        Key.Enter -> NesController.BUTTON_START
        Key.DirectionUp -> NesController.BUTTON_UP
        Key.DirectionDown -> NesController.BUTTON_DOWN
        Key.DirectionLeft -> NesController.BUTTON_LEFT
        Key.DirectionRight -> NesController.BUTTON_RIGHT
        else -> null
    }
}
