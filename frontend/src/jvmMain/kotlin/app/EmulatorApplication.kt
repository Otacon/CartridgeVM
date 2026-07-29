package app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import co.touchlab.kermit.Logger
import frontend.*
import me.tatarka.inject.annotations.Inject
import nes.NesMachine
import nes.Timing
import nes.cartridge.InesParserComposite
import java.nio.file.Path
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.io.path.readBytes
import kotlin.system.exitProcess

@Inject
class EmulatorApplication(
    private val cliArgs: CliArgsParser,
    inesParser: InesParserComposite,
    private val renderer: PlatformRenderer,
    private val audio: PlatformAudioPipeline,
    private val machine: NesMachine,
) {
    private val log = Logger.withTag("EmulatorApplication")
    private val machineLock = Any()
    private val state = EmulatorApplicationState(RomLoader(inesParser, machine))

    fun run() {
        try {
            log.i { "Emulation started" }
            cliArgs.rom?.let { loadRom(it.toRomData()) }
            runComposeWindow()
            log.i { "Emulation finished" }
        } catch (e: Exception) {
            log.e(e) { "Runtime error" }
            exitProcess(1)
        } finally {
            controllerInput?.close()
            audio.close()
        }
    }

    private var controllerInput: PlatformControllerInput? = null

    private fun runComposeWindow() {
        val keyboardInput = PlatformKeyboardInput(machine.controller)

        application {
            var loadedRom by remember { mutableStateOf(state.currentRomName) }
            val romName = loadedRom ?: "No ROM"
            var title by remember { mutableStateOf("CartridgeVM NES [$romName]") }
            var input by remember { mutableStateOf<EmulatorInput?>(if (cliArgs.controller) null else keyboardInput) }
            var focusRequestKey by remember { mutableIntStateOf(0) }
            var crt by remember { mutableStateOf(cliArgs.crt) }
            val coroutineScope = rememberCoroutineScope()

            LaunchedEffect(cliArgs.controller) {
                if (cliArgs.controller) {
                    withFrameNanos { }
                    log.d { "Initializing controller input" }
                    val initialized = withContext(Dispatchers.IO) { PlatformControllerInput(machine.controller) }
                    controllerInput = initialized
                    input = initialized
                }
            }

            val windowState = remember { WindowState(size = crtWindowSize(cliArgs.crt)) }
            LaunchedEffect(crt) {
                windowState.size = crtWindowSize(crt)
            }

            Window(
                onCloseRequest = ::exitApplication,
                title = title,
                state = windowState,
            ) {
                val romPicker = remember(window) { AwtRomPicker(window) }
                ComposeMenuBar(
                    onAction = { action ->
                        coroutineScope.launch {
                            when (action) {
                                MenuAction.OpenRom -> {
                                    val rom = romPicker.pickRom()
                                    if (rom != null && loadRom(rom)) {
                                        loadedRom = state.currentRomName
                                        title = "CartridgeVM NES [${state.currentRomName}]"
                                    }
                                    focusRequestKey++
                                }
                                MenuAction.Exit -> exitApplication()
                            }
                        }
                    },
                    onMenuOpened = {
                        if (input === keyboardInput) keyboardInput.releaseAll()
                    },
                    onMenuDismissed = { focusRequestKey++ },
                    crtEnabled = crt,
                    onToggleCrt = { crt = !crt },
                    modifier = Modifier.fillMaxSize(),
                ) { contentModifier ->
                    input?.let { activeInput ->
                        ComposeSkiaScreen(
                            machine = machine,
                            machineLock = machineLock,
                            renderer = renderer,
                            audio = audio,
                            input = activeInput,
                            keyboardInput = keyboardInput.takeIf { activeInput === it },
                            crt = crt,
                            frameNanos = Timing.FRAME_NANOS,
                            unlimited = cliArgs.unlimited,
                            running = loadedRom != null,
                            focusRequestKey = focusRequestKey,
                            modifier = contentModifier,
                            onFps = { fps ->
                                val currentName = state.currentRomName ?: "No ROM"
                                title = "CartridgeVM NES [$currentName | FPS: $fps]"
                            },
                            onQuit = ::exitApplication,
                        )
                    }
                }
            }
        }
    }

    private fun loadRom(rom: RomData): Boolean = synchronized(machineLock) { state.loadRom(rom) }

    private fun Path.toRomData() = RomData(fileName.toString(), readBytes())
}

private fun crtWindowSize(crt: Boolean): DpSize =
    if (crt) DpSize(1024.dp, 960.dp) else DpSize(768.dp, 720.dp)
