package frontend

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier

@Composable
fun MainScreen(
    viewModel: MainScreenViewModel,
    frameBuffer: SharedFrameBuffer,
    renderer: PlatformRenderer,
    keyboardInput: PlatformKeyboardInput,
    keyboardEventsEnabled: Boolean,
    onOpenRomClick: () -> Unit,
    onTitleChanged: (String) -> Unit,
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
