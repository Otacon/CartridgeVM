package frontend.controllerSettings

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import frontend.components.Dialog
import frontend.components.HorizontalDivider
import frontend.components.TextField
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun ControllerSettingsDialog(
    viewModel: ControllerSettingsViewModel,
    controllerInput: frontend.PlatformControllerInput,
    onDismiss: () -> Unit,
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.onCreate()
    }

    LaunchedEffect(state.captureTarget) {
        val target = state.captureTarget ?: return@LaunchedEffect
        if (target.device != InputDevice.Gamepad) return@LaunchedEffect
        while (true) {
            controllerInput.pressedBindings().firstOrNull()?.let { binding ->
                viewModel.onBindingCaptured(target.button, target.device, binding)
            }
            delay(50.milliseconds)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        title = "Controller Settings",
        onPositive = {
            viewModel.onSave()
            onDismiss()
        },
        onNegative = { onDismiss() },
        positiveText = "OK",
        negativeText = "Cancel",
    ) {
        ButtonTable(
            rows = state.rows,
            onCaptureStarted = viewModel::onCaptureStarted,
            onKeyboardCaptured = { button, binding ->
                viewModel.onBindingCaptured(button, InputDevice.Keyboard, binding)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(all = 16.dp)
        )
    }
}

@Composable
fun ButtonTable(
    rows: List<ButtonRow>,
    onCaptureStarted: (NesButton, InputDevice) -> Unit,
    onKeyboardCaptured: (NesButton, InputBinding) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
        ) {
            TableHeaderCell(modifier = Modifier.weight(1.0f), text = "Button")
            TableHeaderCell(modifier = Modifier.weight(1.0f), text = "Keyboard")
            TableHeaderCell(modifier = Modifier.weight(1.0f), text = "Gamepad")
        }

        HorizontalDivider()

        // Rows
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
            ) {
                TableCell(modifier = Modifier.weight(1.0f)) {
                    BasicText(row.button)
                }

                TableCell(modifier = Modifier.weight(1.0f)) {
                    TextField(
                        value = row.keyboard,
                        onValueChange = {},
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged {
                                if (it.isFocused) onCaptureStarted(row.nesButton, InputDevice.Keyboard)
                            }
                            .onPreviewKeyEvent { event ->
                                if (event.type == KeyEventType.KeyDown) {
                                    onKeyboardCaptured(row.nesButton, event.key.mappingBinding())
                                }
                                event.type == KeyEventType.KeyDown || event.type == KeyEventType.KeyUp
                            }
                            .focusable(),
                    )
                }

                TableCell(modifier = Modifier.weight(1.0f)) {
                    TextField(
                        value = row.gamepad,
                        onValueChange = {},
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged {
                                if (it.isFocused) onCaptureStarted(row.nesButton, InputDevice.Gamepad)
                            }
                            .focusable(),
                    )
                }
            }
        }
    }
}

@Composable
private fun TableHeaderCell(
    text: String,
    modifier: Modifier = Modifier,
) {
    TableCell(modifier) {
        BasicText(text)
    }
}

@Composable
private fun TableCell(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier
            .padding(
                horizontal = 8.dp,
                vertical = 6.dp,
            ),
    ) {
        content()
    }
}

data class ButtonRow(
    val nesButton: NesButton,
    val button: String,
    val keyboard: String,
    val gamepad: String,
)
