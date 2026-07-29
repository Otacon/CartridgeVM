package frontend

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import nes.NesMachine
import nes.Timing

@Composable
fun MainScreen(
    onOpenRomClick: () -> Unit,
    keyboardInput: PlatformKeyboardInput,
    keyboardEventsEnabled: Boolean,
    input: EmulatorInput?,
    machine: NesMachine,
    machineLock: Any,
    renderer: PlatformRenderer,
    audio: PlatformAudioPipeline,
    onTitleChanged: (String) -> Unit,
    viewModel: MainScreenViewModel,
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
            if (input === keyboardInput) keyboardInput.releaseAll()
        },
        onMenuDismissed = { focusRequestKey = !focusRequestKey },
        crtEnabled = state.isCrtEnabled,
        onToggleCrt = { viewModel.setCrtEnabled(!state.isCrtEnabled) },
        modifier = Modifier.fillMaxSize(),
    ) { contentModifier ->
        input?.let { activeInput ->
            ComposeSkiaScreen(
                machine = machine,
                machineLock = machineLock,
                renderer = renderer,
                audio = audio,
                input = activeInput,
                keyboardInput = keyboardInput.takeIf { keyboardEventsEnabled },
                crt = state.isCrtEnabled,
                frameNanos = Timing.FRAME_NANOS,
                enableFrameLimit = state.isFrameLimiterEnabled,
                isRunning = state.isRunning,
                modifier = contentModifier,
                onFps = { viewModel.onFpsUpdated(it) },
                onQuit = { onExitClick?.invoke() },
            )
        }
    }
}
