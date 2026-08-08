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

    private var ignoredBindings = emptySet<String>()

    actual override fun init() = Unit

    actual override fun poll() {
        pressedBindings().forEach { binding ->
            inputMapper.map(InputDevice.Gamepad, binding.code)?.let(controller::press)
        }
    }

    actual fun pressedBindings(): List<InputBinding> {
        val current = currentPressedBindings()
        if (current.none { it.code in ignoredBindings }) ignoredBindings = emptySet()
        return current.filterNot { it.code in ignoredBindings }
    }

    private fun currentPressedBindings(): List<InputBinding> {
        val gamepad = firstGamepad() ?: return emptyList()
        return buildList {
            for (index in 0 until gamepadButtonCount(gamepad)) {
                if (gamepadButton(gamepad, index)) add(gamepadButtonBinding(index))
            }
            for (index in 0 until gamepadAxisCount(gamepad)) {
                val value = gamepadAxis(gamepad, index)
                if (value < -0.5) add(gamepadAxisBinding(index, negative = true))
                if (value > 0.5) add(gamepadAxisBinding(index, negative = false))
            }
        }
    }

    actual fun clearPressedBindings() {
        ignoredBindings = emptySet()
        ignoredBindings = currentPressedBindings().mapTo(mutableSetOf()) { it.code }
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

@JsFun("(pad) => pad.axes.length")
private external fun gamepadAxisCount(pad: JsAny): Int

@JsFun("(pad, index) => pad.axes[index] || 0")
private external fun gamepadAxis(pad: JsAny, index: Int): Double
