@file:OptIn(ExperimentalWasmJsInterop::class)

package frontend

import nes.input.NesController

actual class PlatformControllerInput actual constructor(
    private val controller: NesController,
) : EmulatorInput {
    actual override fun poll() {
        val gamepad = firstGamepad() ?: return
        if (gamepadButton(gamepad, 1)) controller.press(NesController.BUTTON_A)
        if (gamepadButton(gamepad, 0)) controller.press(NesController.BUTTON_B)
        if (gamepadButton(gamepad, 8)) controller.press(NesController.BUTTON_SELECT)
        if (gamepadButton(gamepad, 9)) controller.press(NesController.BUTTON_START)
        if (gamepadButton(gamepad, 12) || gamepadAxis(gamepad, 1) < -0.45) controller.press(NesController.BUTTON_UP)
        if (gamepadButton(gamepad, 13) || gamepadAxis(gamepad, 1) > 0.45) controller.press(NesController.BUTTON_DOWN)
        if (gamepadButton(gamepad, 14) || gamepadAxis(gamepad, 0) < -0.45) controller.press(NesController.BUTTON_LEFT)
        if (gamepadButton(gamepad, 15) || gamepadAxis(gamepad, 0) > 0.45) controller.press(NesController.BUTTON_RIGHT)
    }

    override fun pause() = Unit

    override fun close() = Unit
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
