package frontend.controllerSettings

import androidx.compose.ui.input.key.Key
import io.ControllerMappings
import io.DeviceMappings
import nes.input.NesController

enum class InputDevice {
    Keyboard,
    Gamepad,
}

data class InputBinding(
    val code: String,
    val label: String,
)

class ControllerInputMapper(
    initialMappings: ControllerMappings?,
) {
    var mappings: ControllerMappings = initialMappings ?: defaultControllerMappings()
        private set

    private var keyboardLookup = mappings.keyboard.toButtonLookup()
    private var gamepadLookup = mappings.controller.toButtonLookup()

    fun updateMappings(mappings: ControllerMappings) {
        this.mappings = mappings
        keyboardLookup = mappings.keyboard.toButtonLookup()
        gamepadLookup = mappings.controller.toButtonLookup()
    }

    fun map(device: InputDevice, code: String): Int? = when (device) {
        InputDevice.Keyboard -> keyboardLookup[code]
        InputDevice.Gamepad -> gamepadLookup[code]
    }
}

fun ControllerMappings.valueFor(device: InputDevice, button: Int): String = when (device) {
    InputDevice.Keyboard -> keyboard.valueFor(button)
    InputDevice.Gamepad -> controller.valueFor(button)
}

fun ControllerMappings.withValue(device: InputDevice, button: Int, value: String): ControllerMappings =
    when (device) {
        InputDevice.Keyboard -> copy(keyboard = keyboard.withValue(button, value))
        InputDevice.Gamepad -> copy(controller = controller.withValue(button, value))
    }

fun defaultControllerMappings(): ControllerMappings = ControllerMappings(
    keyboard = DeviceMappings(
        a = Key.Z.mappingCode(),
        b = Key.X.mappingCode(),
        select = Key.ShiftLeft.mappingCode(),
        start = Key.Enter.mappingCode(),
        up = Key.DirectionUp.mappingCode(),
        down = Key.DirectionDown.mappingCode(),
        left = Key.DirectionLeft.mappingCode(),
        right = Key.DirectionRight.mappingCode(),
    ),
    controller = DeviceMappings(
        a = gamepadButtonCode(1),
        b = gamepadButtonCode(0),
        select = gamepadButtonCode(8),
        start = gamepadButtonCode(9),
        up = gamepadAxisCode(1, negative = true),
        down = gamepadAxisCode(1, negative = false),
        left = gamepadAxisCode(0, negative = true),
        right = gamepadAxisCode(0, negative = false),
    ),
)

fun Key.mappingBinding(): InputBinding = InputBinding(
    code = mappingCode(),
    label = mappingCode().bindingLabel(InputDevice.Keyboard),
)

fun Key.mappingCode(): String = toString()

fun gamepadButtonBinding(index: Int): InputBinding = InputBinding(
    code = gamepadButtonCode(index),
    label = "Button $index",
)

fun gamepadAxisBinding(index: Int, negative: Boolean): InputBinding = InputBinding(
    code = gamepadAxisCode(index, negative),
    label = "Axis $index ${if (negative) "-" else "+"}",
)

fun gamepadPovBinding(direction: String): InputBinding = InputBinding(
    code = gamepadPovCode(direction),
    label = "D-pad ${direction.replaceFirstChar { it.uppercase() }}",
)

private fun DeviceMappings.valueFor(button: Int): String = valuesByButton().getOrElse(button) {
    throw IllegalArgumentException("Button $button is not supported.")
}

private fun DeviceMappings.withValue(button: Int, value: String): DeviceMappings = when (button) {
    NesController.BUTTON_A -> copy(a = value)
    NesController.BUTTON_B -> copy(b = value)
    NesController.BUTTON_SELECT -> copy(select = value)
    NesController.BUTTON_START -> copy(start = value)
    NesController.BUTTON_UP -> copy(up = value)
    NesController.BUTTON_DOWN -> copy(down = value)
    NesController.BUTTON_LEFT -> copy(left = value)
    NesController.BUTTON_RIGHT -> copy(right = value)
    else -> throw IllegalArgumentException("Button $button is not supported.")
}

private fun DeviceMappings.valuesByButton()= arrayOf(
    a, b, select, start, up, down, left, right,
)

private fun DeviceMappings.toButtonLookup(): Map<String, Int> = valuesByButton()
    .mapIndexed { button, code -> code to button }
    .toMap()

private fun gamepadButtonCode(index: Int): String = "button:$index"

private fun gamepadAxisCode(index: Int, negative: Boolean): String = "axis:$index:${if (negative) "-" else "+"}"

private fun gamepadPovCode(direction: String): String = "pov:$direction"

fun String.bindingLabel(device: InputDevice): String = when (device) {
    InputDevice.Keyboard -> substringAfterLast('.')
    InputDevice.Gamepad -> when {
        startsWith("button:") -> "Button ${substringAfter(':')}"
        startsWith("axis:") -> split(':').let { parts ->
            if (parts.size == 3) "Axis ${parts[1]} ${parts[2]}" else this
        }

        startsWith("pov:") -> "D-pad ${substringAfter(':').replaceFirstChar { it.uppercase() }}"
        else -> this
    }
}
