package app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import androidx.compose.ui.window.MenuBar
import co.touchlab.kermit.Logger
import frontend.*
import me.tatarka.inject.annotations.Inject
import nes.NesMachine
import nes.Timing
import nes.cartridge.InesParserComposite
import nes.cartridge.RomFormatException
import java.awt.FileDialog
import java.awt.Frame
import java.nio.file.Path
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.io.path.readBytes
import kotlin.system.exitProcess

@Inject
class EmulatorApplication(
    private val cliArgs: CliArgsParser,
    private val inesParser: InesParserComposite,
    private val renderer: OpenGlRenderer,
    private val audio: OpenAlAudio,
    private val machine: NesMachine,
) {
    private val log = Logger.withTag("EmulatorApplication")
    private var currentRom: Path? = null

    fun run() {
        try {
            log.i { "Emulation started" }
            cliArgs.rom?.let { loadRom(it) }
            runComposeWindow()
            log.i { "Emulation finished" }
        } catch (e: RomFormatException) {
            log.e(e) { "Unable to load rom" }
            exitProcess(2)
        } catch (e: Exception) {
            log.e(e) { "Runtime error" }
            exitProcess(1)
        } finally {
            controllerInput?.close()
            audio.close()
        }
    }

    private var controllerInput: ControllerInput? = null

    private fun runComposeWindow() {
        val keyboardInput = ComposeKeyboardInput(machine.controller)

        application {
            var loadedRom by remember { mutableStateOf(currentRom) }
            val romName = loadedRom?.fileName?.toString() ?: "No ROM"
            var title by remember { mutableStateOf("CartridgeVM NES [$romName]") }
            var input by remember { mutableStateOf<EmulatorInput?>(if (cliArgs.controller) null else keyboardInput) }

            LaunchedEffect(cliArgs.controller) {
                if (cliArgs.controller) {
                    withFrameNanos { }
                    log.d { "Initializing controller input" }
                    val initialized = withContext(Dispatchers.IO) { ControllerInput(machine.controller) }
                    controllerInput = initialized
                    input = initialized
                }
            }

            Window(
                onCloseRequest = ::exitApplication,
                title = title,
                state = WindowState(size = if (cliArgs.crt) DpSize(1024.dp, 960.dp) else DpSize(768.dp, 720.dp)),
            ) {
                MenuBar {
                    Menu("File") {
                        Item("Open ROM...") {
                            openRomDialog(window)?.let { path ->
                                if (loadRom(path)) {
                                    loadedRom = path
                                    title = "CartridgeVM NES [${path.fileName}]"
                                }
                            }
                        }
                        Separator()
                        Item("Exit", onClick = ::exitApplication)
                    }
                }

                input?.let { activeInput ->
                    ComposeOpenGlScreen(
                        machine = machine,
                        renderer = renderer,
                        audio = audio,
                        input = activeInput,
                        keyboardInput = keyboardInput,
                        crt = cliArgs.crt,
                        frameNanos = Timing.FRAME_NANOS,
                        unlimited = cliArgs.unlimited,
                        running = loadedRom != null,
                        pollInputOnEmulatorThread = true,
                        modifier = Modifier.fillMaxSize(),
                        onFps = { fps ->
                            val currentName = currentRom?.fileName?.toString() ?: "No ROM"
                            title = "CartridgeVM NES [$currentName | FPS: $fps]"
                        },
                        onQuit = ::exitApplication,
                    )
                }
            }
        }
    }

    private fun loadRom(path: Path): Boolean {
        try {
            val cartridge = inesParser.parse(path.readBytes())
            machine.insert(cartridge)
            machine.reset()
            currentRom = path
            log.i { "Loaded ROM: ${path.fileName}" }
            return true
        } catch (e: RomFormatException) {
            log.e(e) { "Unable to load rom: $path" }
            return false
        }
    }

    private fun openRomDialog(parent: Frame): Path? {
        val dialog = FileDialog(parent, "Open NES ROM", FileDialog.LOAD).apply {
            filenameFilter = java.io.FilenameFilter { _, name -> name.endsWith(".nes", ignoreCase = true) }
            isVisible = true
        }
        val file = dialog.file ?: return null
        return Path.of(dialog.directory, file)
    }

}
