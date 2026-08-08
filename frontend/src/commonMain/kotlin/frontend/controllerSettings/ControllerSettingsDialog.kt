package frontend.controllerSettings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import frontend.components.Dialog
import frontend.components.HorizontalDivider
import frontend.components.TextField

@Composable
fun ControllerSettingsDialog(
    onDismiss: () -> Unit,
) {
    var rows by remember {
        mutableStateOf(
            listOf(
                ButtonRow("Up", "", ""),
                ButtonRow("Down", "", ""),
                ButtonRow("Left", "", ""),
                ButtonRow("Right", "", ""),
                ButtonRow("A", "", ""),
                ButtonRow("B", "", ""),
                ButtonRow("Start", "", ""),
                ButtonRow("Select", "", ""),
            )
        )
    }
    Dialog(
        onDismissRequest = onDismiss,
        title = "Controller Settings",
        onPositive = { onDismiss() },
        onNegative = { onDismiss() },
        positiveText = "OK",
        negativeText = "Cancel",
    ) {
        ButtonTable(
            rows = rows,
            onPrimaryChange = { _, _ -> },
            onSecondaryChange = { _, _ -> },
            modifier = Modifier
                .fillMaxWidth()
                .padding(all = 16.dp)
        )
    }
}

@Composable
fun ButtonTable(
    rows: List<ButtonRow>,
    onPrimaryChange: (index: Int, value: String) -> Unit,
    onSecondaryChange: (index: Int, value: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
        ) {
            TableHeaderCell(modifier = Modifier.weight(1.0f), text = "Button")
            TableHeaderCell(modifier = Modifier.weight(1.0f), text = "Primary")
            TableHeaderCell(modifier = Modifier.weight(1.0f), text = "Secondary")
        }

        HorizontalDivider()

        // Rows
        rows.forEachIndexed { index, row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
            ) {
                TableCell(modifier = Modifier.weight(1.0f)) {
                    BasicText(row.button)
                }

                TableCell(modifier = Modifier.weight(1.0f)) {
                    TextField(
                        value = row.primary,
                        onValueChange = {
                            onPrimaryChange(index, it)
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                TableCell(modifier = Modifier.weight(1.0f)) {
                    TextField(
                        value = row.secondary,
                        onValueChange = {
                            onSecondaryChange(index, it)
                        },
                        modifier = Modifier.fillMaxWidth(),
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
    val button: String,
    val primary: String,
    val secondary: String,
)