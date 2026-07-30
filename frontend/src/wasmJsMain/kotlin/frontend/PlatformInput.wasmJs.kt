@file:OptIn(ExperimentalWasmJsInterop::class)

package frontend

import androidx.compose.ui.input.key.*
import nes.input.NesController

actual class PlatformKeyboardInput actual constructor(
    private val controller: NesController,
) : BaseEmulatorInput() {
    private val pressedKeys = mutableSetOf<String>()
    private var currentButtons = 0

    actual fun onKeyEvent(event: KeyEvent): Boolean {
        val code = event.key.toCode() ?: return false
        when (event.type) {
            KeyEventType.KeyDown -> pressedKeys += code
            KeyEventType.KeyUp -> pressedKeys -= code
            else -> return false
        }
        return true
    }

    actual fun releaseAll() {
        pressedKeys.clear()
        currentButtons = 0
        controller.setButtons(0)
    }

    actual override fun poll() {
        var buttons = 0
        if ("KeyZ".isPressed()) buttons = buttons or (1 shl NesController.A)
        if ("KeyX".isPressed()) buttons = buttons or (1 shl NesController.B)
        if ("ShiftLeft".isPressed() || "ShiftRight".isPressed()) buttons = buttons or (1 shl NesController.SELECT)
        if ("Enter".isPressed()) buttons = buttons or (1 shl NesController.START)
        if ("ArrowUp".isPressed()) buttons = buttons or (1 shl NesController.UP)
        if ("ArrowDown".isPressed()) buttons = buttons or (1 shl NesController.DOWN)
        if ("ArrowLeft".isPressed()) buttons = buttons or (1 shl NesController.LEFT)
        if ("ArrowRight".isPressed()) buttons = buttons or (1 shl NesController.RIGHT)
        currentButtons = buttons
        controller.setButtons(currentButtons)
        updateControlEdges("KeyR".isPressed())
    }

    fun buttonMask(): Int = currentButtons

    actual override fun quitRequested(): Boolean = false

    override fun pause() {
        releaseAll()
    }

    override fun close() {
        pressedKeys.clear()
        currentButtons = 0
        controller.setButtons(0)
    }

    private fun String.isPressed(): Boolean = this in pressedKeys

    private fun Key.toCode(): String? = when (this) {
        Key.Z -> "KeyZ"
        Key.X -> "KeyX"
        Key.ShiftLeft -> "ShiftLeft"
        Key.ShiftRight -> "ShiftRight"
        Key.Enter -> "Enter"
        Key.DirectionUp -> "ArrowUp"
        Key.DirectionDown -> "ArrowDown"
        Key.DirectionLeft -> "ArrowLeft"
        Key.DirectionRight -> "ArrowRight"
        Key.P -> "KeyP"
        Key.R -> "KeyR"
        else -> null
    }
}

actual class PlatformControllerInput actual constructor(
    private val controller: NesController,
) : BaseEmulatorInput() {
    private var currentButtons = 0

    actual override fun poll() {
        val gamepad = firstGamepad()
        if (gamepad == null) {
            currentButtons = 0
            controller.setButtons(0)
            updateControlEdges(reset = false)
            return
        }
        var buttons = 0
        if (gamepadButton(gamepad, 1)) buttons = buttons or (1 shl NesController.A)
        if (gamepadButton(gamepad, 0)) buttons = buttons or (1 shl NesController.B)
        if (gamepadButton(gamepad, 8)) buttons = buttons or (1 shl NesController.SELECT)
        if (gamepadButton(gamepad, 9)) buttons = buttons or (1 shl NesController.START)
        if (gamepadButton(gamepad, 12) || gamepadAxis(gamepad, 1) < -0.45) buttons = buttons or (1 shl NesController.UP)
        if (gamepadButton(gamepad, 13) || gamepadAxis(gamepad, 1) > 0.45) buttons =
            buttons or (1 shl NesController.DOWN)
        if (gamepadButton(gamepad, 14) || gamepadAxis(gamepad, 0) < -0.45) buttons =
            buttons or (1 shl NesController.LEFT)
        if (gamepadButton(gamepad, 15) || gamepadAxis(gamepad, 0) > 0.45) buttons =
            buttons or (1 shl NesController.RIGHT)
        currentButtons = buttons
        controller.setButtons(currentButtons)
        updateControlEdges(gamepadButton(gamepad, 5))
    }

    fun buttonMask(): Int = currentButtons

    actual override fun quitRequested(): Boolean = false

    override fun pause() {
        currentButtons = 0
        controller.setButtons(0)
    }

    override fun close() {
        currentButtons = 0
        controller.setButtons(0)
    }
}

@JsFun(
    """
    () => {
        const pads = navigator.getGamepads ? navigator.getGamepads() : [];
        for (const pad of pads) {
            if (pad && pad.connected) return pad;
        }
        return null;
    }
    """
)
private external fun firstGamepad(): JsAny?

@JsFun("(pad, index) => !!(pad.buttons[index] && pad.buttons[index].pressed)")
private external fun gamepadButton(pad: JsAny, index: Int): Boolean

@JsFun("(pad, index) => pad.axes[index] || 0")
private external fun gamepadAxis(pad: JsAny, index: Int): Double
