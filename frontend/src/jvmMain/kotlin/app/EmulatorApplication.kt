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
    private val machineLock = Any()

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
        val viewModel = MainScreenViewModel(
            config = cliArgs.asConfig(),
            machine = machine,
            parser = inesParser,
        )

        application {
            var input by remember { mutableStateOf<EmulatorInput?>(if (cliArgs.controller) null else keyboardInput) }
            var focusRequestKey by remember { mutableIntStateOf(0) }
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

            val windowState = remember { WindowState(size = DpSize(768.dp, 720.dp)) }

            Window(
                onCloseRequest = ::exitApplication,
                state = windowState,
            ) {
                val romPicker = remember(window) { FileChooser(window) }
                MainScreen(
                    onOpenRomClick = {
                        coroutineScope.launch {
                            val rom = romPicker.pickRom()
                            viewModel.onRomSelected(rom)
                            focusRequestKey++
                        }
                    },
                    onExitClick = ::exitApplication,
                    onTitleChanged = { window.title = it },
                    keyboardInput = keyboardInput,
                    keyboardEventsEnabled = input === keyboardInput,
                    input = input,
                    machine = machine,
                    machineLock = machineLock,
                    renderer = renderer,
                    audio = audio,
                    viewModel = viewModel,
                )
            }
        }
    }
}
