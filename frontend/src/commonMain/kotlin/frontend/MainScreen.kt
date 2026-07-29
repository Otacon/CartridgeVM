package frontend

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import nes.NesMachine
import nes.Timing

@Composable
fun MainScreen(
    onOpenRomClick: () -> Unit,
    onExitClick: () -> Unit,
    onToggleCrt: () -> Unit,
    isCrtEnabled: Boolean,
    unlimited: Boolean,
    isRunning: Boolean,
    keyboardInput: PlatformKeyboardInput,
    keyboardEventsEnabled: Boolean,
    input: EmulatorInput?,
    machine: NesMachine,
    machineLock: Any,
    renderer: PlatformRenderer,
    audio: PlatformAudioPipeline,
) {
    var focusRequestKey by remember { mutableIntStateOf(0) }
    ComposeMenuBar(
        onOpenRom = onOpenRomClick,
        onExit = onExitClick,
        onMenuOpened = {
            if (input === keyboardInput) keyboardInput.releaseAll()
        },
        onMenuDismissed = { focusRequestKey++ },
        crtEnabled = isCrtEnabled,
        onToggleCrt = onToggleCrt,
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
                crt = isCrtEnabled,
                frameNanos = Timing.FRAME_NANOS,
                unlimited = unlimited,
                running = isRunning,
                modifier = contentModifier,
                onFps = { fps ->

                },
                onQuit = onExitClick,
            )
        }
    }
}
