package frontend.controllerSettings

import androidx.lifecycle.ViewModel
import dev.zacsweers.metro.Inject
import io.ControllerMappings
import io.Preferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import nes.input.NesController.Companion.NES_BUTTONS

@Inject
class ControllerSettingsViewModel(
    private val preferences: Preferences,
    private val inputMapper: ControllerInputMapper,
) : ViewModel() {
    private val _state = MutableStateFlow(ControllerSettingsState())
    val state = _state.asStateFlow()

    fun onCreate() {
        val mappings = preferences.mappings ?: defaultControllerMappings()
        _state.value = ControllerSettingsState(mappings = mappings)
    }

    fun onCaptureStarted(button: Int, device: InputDevice) {
        _state.update { it.copy(captureTarget = CaptureTarget(button, device)) }
    }

    fun onBindingCaptured(button: Int, device: InputDevice, binding: InputBinding) {
        _state.update { state ->
            state.copy(
                mappings = state.mappings.withValue(device, button, binding.code),
                labels = state.labels + ((device to binding.code) to binding.label),
                captureTarget = null,
            )
        }
    }

    fun onCaptureCancelled() {
        _state.update { it.copy(captureTarget = null) }
    }

    fun onSave() {
        val mappings = _state.value.mappings
        preferences.mappings = mappings
        inputMapper.updateMappings(mappings)
    }
}

data class ControllerSettingsState(
    val mappings: ControllerMappings = defaultControllerMappings(),
    val labels: Map<Pair<InputDevice, String>, String> = emptyMap(),
    val captureTarget: CaptureTarget? = null,
) {
    val rows: List<ButtonRow>
        get() = NES_BUTTONS.map { button ->
            ButtonRow(
                button = button,
                label = BUTTON_LABELS[button],
                keyboardBinding = if (captureTarget == CaptureTarget(button, InputDevice.Keyboard)) {
                    "Press keyboard input..."
                } else {
                    labelFor(InputDevice.Keyboard, mappings.valueFor(InputDevice.Keyboard, button))
                },
                gamepadBinding = if (captureTarget == CaptureTarget(button, InputDevice.Gamepad)) {
                    "Press gamepad input..."
                } else {
                    labelFor(InputDevice.Gamepad, mappings.valueFor(InputDevice.Gamepad, button))
                },
            )
        }

    private fun labelFor(device: InputDevice, code: String): String =
        labels[device to code] ?: code.bindingLabel(device)
}

data class CaptureTarget(
    val button: Int,
    val device: InputDevice,
)

private val BUTTON_LABELS = arrayOf(
    "A", "B", "Select", "Start", "Up", "Down", "Left", "Right",
)
