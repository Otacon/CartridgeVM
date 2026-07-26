package frontend

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties

@Composable
fun ComposeMenuBar(
    onAction: (MenuAction) -> Unit,
    onMenuOpened: () -> Unit,
    onMenuDismissed: () -> Unit,
    crtEnabled: Boolean,
    onToggleCrt: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (Modifier) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    val popupOffset = remember(density) {
        with(density) { IntOffset(4.dp.roundToPx(), MENU_HEIGHT.roundToPx()) }
    }

    Column(modifier.fillMaxSize().background(Color.Black)) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(MENU_HEIGHT)
                .background(MENU_BAR_COLOR)
                .border(1.dp, MENU_BORDER_COLOR)
                .padding(horizontal = 4.dp),
        ) {
            MenuButton(
                label = emulatorMainMenu.label,
                selected = expanded,
                onClick = {
                    expanded = !expanded
                    if (expanded) onMenuOpened()
                },
            )
            Box(Modifier.fillMaxHeight().width(1.dp).background(MENU_BORDER_COLOR))
            ToggleButton(
                label = "CRT",
                selected = crtEnabled,
                onClick = onToggleCrt,
            )
        }
        Box(Modifier.fillMaxWidth().weight(1f)) {
            content(Modifier.fillMaxSize())
        }
    }

    if (expanded) {
        Popup(
            alignment = Alignment.TopStart,
            offset = popupOffset,
            onDismissRequest = {
                expanded = false
                onMenuDismissed()
            },
            properties = PopupProperties(focusable = true),
        ) {
            Column(
                Modifier
                    .width(180.dp)
                    .shadow(6.dp)
                    .background(MENU_POPUP_COLOR)
                    .border(1.dp, MENU_BORDER_COLOR)
                    .padding(vertical = 4.dp),
            ) {
                emulatorMainMenu.entries.forEach { entry ->
                    when (entry) {
                        is MenuEntry.Item -> MenuItem(entry.label) {
                            expanded = false
                            onAction(entry.action)
                        }
                        MenuEntry.Separator -> Box(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .height(1.dp)
                                .background(MENU_BORDER_COLOR),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MenuButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        Modifier
            .fillMaxHeight()
            .width(48.dp)
            .background(if (selected) MENU_SELECTION_COLOR else Color.Transparent)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(label, style = MENU_TEXT_STYLE)
    }
}

@Composable
private fun ToggleButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        Modifier
            .fillMaxHeight()
            .width(48.dp)
            .background(if (selected) MENU_SELECTION_COLOR else Color.Transparent)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(label, style = MENU_TEXT_STYLE)
    }
}

@Composable
private fun MenuItem(label: String, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        Modifier
            .fillMaxWidth()
            .height(30.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        BasicText(label, style = MENU_TEXT_STYLE)
    }
}

private val MENU_HEIGHT = 30.dp
private val MENU_BAR_COLOR = Color(0xFFF1F1F1)
private val MENU_POPUP_COLOR = Color(0xFFF7F7F7)
private val MENU_BORDER_COLOR = Color(0xFFB8B8B8)
private val MENU_SELECTION_COLOR = Color(0xFFD9E8F8)
private val MENU_TEXT_STYLE = TextStyle(color = Color(0xFF161616), fontSize = 13.sp)
