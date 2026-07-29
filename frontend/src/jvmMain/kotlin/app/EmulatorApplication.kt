package app

import androidx.compose.runtime.*
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import co.touchlab.kermit.Logger
import frontend.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.tatarka.inject.annotations.Inject
import nes.NesMachine
import nes.cartridge.InesParserComposite
import kotlin.system.exitProcess

@Inject
class EmulatorApplication(
    private val cliArgs: CliArgsParser,
    private val inesParser: InesParserComposite,
    private val renderer: PlatformRenderer,
    private val audio: PlatformAudioPipeline,
    private val machine: NesMachine,
) {
    private val log = Logger.withTag("EmulatorApplication")

    fun run() {
        try {
            log.i { "Emulation started" }
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
        val runtimeInput = DelegatingEmulatorInput(if (cliArgs.controller) null else keyboardInput)
        val runtimeHost = EmulatorRuntimeHost(
            machine = machine,
            audio = audio,
            input = runtimeInput,
            frameNanos = nes.Timing.FRAME_NANOS,
        )
        val viewModel = MainScreenViewModel(
            config = cliArgs.asConfig(),
            machine = machine,
            parser = inesParser,
            runtime = runtimeHost,
        )

        application {
            var keyboardEventsEnabled by remember { mutableStateOf(!cliArgs.controller) }
            val coroutineScope = rememberCoroutineScope()

            LaunchedEffect(cliArgs.controller) {
                if (cliArgs.controller) {
                    withFrameNanos { }
                    log.d { "Initializing controller input" }
                    val initialized = withContext(Dispatchers.IO) { PlatformControllerInput(machine.controller) }
                    controllerInput = initialized
                    runtimeInput.current = initialized
                    keyboardEventsEnabled = false
                }
            }

            val windowState = remember { WindowState(size = DpSize(768.dp, 720.dp)) }

            DisposableEffect(Unit) {
                runtimeHost.start(
                    onFps = { fps -> coroutineScope.launch { viewModel.onFpsUpdated(fps) } },
                    onQuit = { coroutineScope.launch { exitApplication() } },
                    onError = { coroutineScope.launch { exitApplication() } },
                )
                onDispose(runtimeHost::stop)
            }

            DisposableEffect(Unit) {
                onDispose(runtimeHost::close)
            }

            Window(
                onCloseRequest = ::exitApplication,
                state = windowState,
            ) {
                val romPicker = remember(window) { FileChooser(window) }
                MainScreen(
                    viewModel = viewModel,
                    frameBuffer = runtimeHost.frameBuffer,
                    renderer = renderer,
                    keyboardInput = keyboardInput,
                    keyboardEventsEnabled = keyboardEventsEnabled,
                    onTitleChanged = { window.title = it },
                    onOpenRomClick = {
                        coroutineScope.launch {
                            val rom = romPicker.pickRom()
                            viewModel.onRomSelected(rom)
                        }
                    },
                    onExitClick = ::exitApplication,
                )
            }
        }
    }
}
