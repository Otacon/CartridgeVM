package frontend

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

@Composable
fun MainScreen(
    viewModel: MainScreenViewModel,
    frameBuffer: SharedFrameBuffer?,
    renderer: PlatformRenderer,
    keyboardInput: PlatformKeyboardInput,
    keyboardEventsEnabled: Boolean,
    onOpenRomClick: () -> Unit,
    onTitleChanged: (String) -> Unit,
    onRunningChanged: (Boolean) -> Unit,
    onExitClick: (() -> Unit)? = null,
) {
    var focusRequestKey by remember { mutableStateOf(true) }
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.onCreate()
    }

    LaunchedEffect(state.windowTitle) {
        onTitleChanged(state.windowTitle)
    }

    LaunchedEffect(state.isRunning) {
        onRunningChanged(state.isRunning)
    }

    ComposeMenuBar(
        onOpenRom = onOpenRomClick,
        onExit = onExitClick,
        onMenuOpened = {
            if (keyboardEventsEnabled) keyboardInput.releaseAll()
        },
        onMenuDismissed = { focusRequestKey = !focusRequestKey },
        crtEnabled = state.isCrtEnabled,
        onToggleCrt = { viewModel.setCrtEnabled(!state.isCrtEnabled) },
        modifier = Modifier.fillMaxSize(),
    ) { contentModifier ->
        if (frameBuffer != null) {
            ComposeSkiaScreen(
                frameBuffer = frameBuffer,
                renderer = renderer,
                keyboardInput = keyboardInput.takeIf { keyboardEventsEnabled },
                crt = state.isCrtEnabled,
                focusRequestKey = focusRequestKey,
                modifier = contentModifier,
            )
        }
    }
}
