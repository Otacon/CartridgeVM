package frontend

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import nes.input.NesController
import java.awt.event.KeyEvent as AwtKeyEvent

class ComposeKeyboardInput(
    private val controller: NesController,
) : BaseEmulatorInput() {
    private val pressedKeys = mutableSetOf<Key>()

    @Synchronized
    fun onKeyEvent(event: KeyEvent): Boolean {
        when (event.type) {
            KeyEventType.KeyDown -> pressedKeys += event.key
            KeyEventType.KeyUp -> pressedKeys -= event.key
            else -> return false
        }
        return event.key in handledKeys
    }

    @Synchronized
    fun onAwtKeyEvent(event: AwtKeyEvent): Boolean {
        val key = event.toComposeKey() ?: return false
        when (event.id) {
            AwtKeyEvent.KEY_PRESSED -> pressedKeys += key
            AwtKeyEvent.KEY_RELEASED -> pressedKeys -= key
            else -> return false
        }
        return true
    }

    @Synchronized
    override fun poll() {
        var buttons = 0
        if (Key.Z.isPressed()) buttons = buttons or (1 shl NesController.A)
        if (Key.X.isPressed()) buttons = buttons or (1 shl NesController.B)
        if (Key.ShiftLeft.isPressed() || Key.ShiftRight.isPressed()) buttons = buttons or (1 shl NesController.SELECT)
        if (Key.Enter.isPressed()) buttons = buttons or (1 shl NesController.START)
        if (Key.DirectionUp.isPressed()) buttons = buttons or (1 shl NesController.UP)
        if (Key.DirectionDown.isPressed()) buttons = buttons or (1 shl NesController.DOWN)
        if (Key.DirectionLeft.isPressed()) buttons = buttons or (1 shl NesController.LEFT)
        if (Key.DirectionRight.isPressed()) buttons = buttons or (1 shl NesController.RIGHT)
        controller.setButtons(buttons)
        updateControlEdges(Key.P.isPressed(), Key.R.isPressed())
    }

    @Synchronized
    override fun quitRequested() = Key.Escape.isPressed()

    @Synchronized
    override fun close() {
        pressedKeys.clear()
        controller.setButtons(0)
    }

    private fun Key.isPressed(): Boolean = this in pressedKeys

    private companion object {
        val handledKeys = setOf(
            Key.Z,
            Key.X,
            Key.ShiftLeft,
            Key.ShiftRight,
            Key.Enter,
            Key.DirectionUp,
            Key.DirectionDown,
            Key.DirectionLeft,
            Key.DirectionRight,
            Key.P,
            Key.R,
            Key.Escape,
        )

        fun AwtKeyEvent.toComposeKey(): Key? = when (keyCode) {
            AwtKeyEvent.VK_Z -> Key.Z
            AwtKeyEvent.VK_X -> Key.X
            AwtKeyEvent.VK_SHIFT -> if (keyLocation == AwtKeyEvent.KEY_LOCATION_RIGHT) Key.ShiftRight else Key.ShiftLeft
            AwtKeyEvent.VK_ENTER -> Key.Enter
            AwtKeyEvent.VK_UP -> Key.DirectionUp
            AwtKeyEvent.VK_DOWN -> Key.DirectionDown
            AwtKeyEvent.VK_LEFT -> Key.DirectionLeft
            AwtKeyEvent.VK_RIGHT -> Key.DirectionRight
            AwtKeyEvent.VK_P -> Key.P
            AwtKeyEvent.VK_R -> Key.R
            AwtKeyEvent.VK_ESCAPE -> Key.Escape
            else -> null
        }
    }
}
