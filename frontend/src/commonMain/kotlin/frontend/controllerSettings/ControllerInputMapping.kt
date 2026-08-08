package frontend.controllerSettings

import androidx.compose.ui.input.key.Key
import io.ControllerMappings
import io.DeviceMappings
import nes.input.NesController

enum class NesButton(
    val label: String,
    val controllerButton: Int,
) {
    Up("Up", NesController.BUTTON_UP),
    Down("Down", NesController.BUTTON_DOWN),
    Left("Left", NesController.BUTTON_LEFT),
    Right("Right", NesController.BUTTON_RIGHT),
    A("A", NesController.BUTTON_A),
    B("B", NesController.BUTTON_B),
    Start("Start", NesController.BUTTON_START),
    Select("Select", NesController.BUTTON_SELECT),
}

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

    fun updateMappings(mappings: ControllerMappings) {
        this.mappings = mappings
    }

    fun map(device: InputDevice, code: String): Int? {
        val deviceMappings = when (device) {
            InputDevice.Keyboard -> mappings.keyboard
            InputDevice.Gamepad -> mappings.controller
        }
        return NesButton.entries.firstOrNull { button ->
            deviceMappings.valueFor(button) == code
        }?.controllerButton
    }
}

fun ControllerMappings.valueFor(device: InputDevice, button: NesButton): String = when (device) {
    InputDevice.Keyboard -> keyboard.valueFor(button)
    InputDevice.Gamepad -> controller.valueFor(button)
}

fun ControllerMappings.withValue(device: InputDevice, button: NesButton, value: String): ControllerMappings = when (device) {
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

private fun DeviceMappings.valueFor(button: NesButton): String = when (button) {
    NesButton.A -> a
    NesButton.B -> b
    NesButton.Select -> select
    NesButton.Start -> start
    NesButton.Up -> up
    NesButton.Down -> down
    NesButton.Left -> left
    NesButton.Right -> right
}

private fun DeviceMappings.withValue(button: NesButton, value: String): DeviceMappings = when (button) {
    NesButton.A -> copy(a = value)
    NesButton.B -> copy(b = value)
    NesButton.Select -> copy(select = value)
    NesButton.Start -> copy(start = value)
    NesButton.Up -> copy(up = value)
    NesButton.Down -> copy(down = value)
    NesButton.Left -> copy(left = value)
    NesButton.Right -> copy(right = value)
}

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
