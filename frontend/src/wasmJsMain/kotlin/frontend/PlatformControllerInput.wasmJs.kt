@file:OptIn(ExperimentalWasmJsInterop::class)

package frontend

import frontend.controllerSettings.ControllerInputMapper
import frontend.controllerSettings.InputBinding
import frontend.controllerSettings.InputDevice
import frontend.controllerSettings.gamepadAxisBinding
import frontend.controllerSettings.gamepadButtonBinding
import nes.input.NesController

actual class PlatformControllerInput actual constructor(
    private val controller: NesController,
    private val inputMapper: ControllerInputMapper,
) : EmulatorInput {

    actual override fun init() = Unit

    actual override fun poll() {
        pressedBindings().forEach { binding ->
            inputMapper.map(InputDevice.Gamepad, binding.code)?.let(controller::press)
        }
    }

    actual fun pressedBindings(): List<InputBinding> {
        val gamepad = firstGamepad() ?: return emptyList()
        return buildList {
            for (index in 0 until gamepadButtonCount(gamepad)) {
                if (gamepadButton(gamepad, index)) add(gamepadButtonBinding(index))
            }
            val x = gamepadAxis(gamepad, 0)
            if (x < -0.5) add(gamepadAxisBinding(0, negative = true))
            if (x > 0.5) add(gamepadAxisBinding(0, negative = false))

            val y = gamepadAxis(gamepad, 1)
            if (y < -0.5) add(gamepadAxisBinding(1, negative = true))
            if (y > 0.5) add(gamepadAxisBinding(1, negative = false))
        }
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

@JsFun("(pad) => pad.buttons.length")
private external fun gamepadButtonCount(pad: JsAny): Int

@JsFun("(pad, index) => pad.axes[index] || 0")
private external fun gamepadAxis(pad: JsAny, index: Int): Double
