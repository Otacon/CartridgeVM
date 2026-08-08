package frontend.controllerSettings

import androidx.lifecycle.ViewModel
import dev.zacsweers.metro.Inject
import io.ControllerMappings
import io.Preferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

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

    fun onCaptureStarted(button: NesButton, device: InputDevice) {
        _state.update { it.copy(captureTarget = CaptureTarget(button, device)) }
    }

    fun onBindingCaptured(button: NesButton, device: InputDevice, binding: InputBinding) {
        _state.update { state ->
            state.copy(
                mappings = state.mappings.withValue(device, button, binding.code),
                labels = state.labels + ((device to binding.code) to binding.label),
            )
        }
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
        get() = NesButton.entries.map { button ->
            ButtonRow(
                nesButton = button,
                button = button.label,
                keyboard = labelFor(InputDevice.Keyboard, mappings.valueFor(InputDevice.Keyboard, button)),
                gamepad = labelFor(InputDevice.Gamepad, mappings.valueFor(InputDevice.Gamepad, button)),
            )
        }

    private fun labelFor(device: InputDevice, code: String): String = labels[device to code] ?: code.bindingLabel(device)
}

data class CaptureTarget(
    val button: NesButton,
    val device: InputDevice,
)
