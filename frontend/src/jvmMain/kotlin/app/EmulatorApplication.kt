package app

import androidx.compose.runtime.*
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import co.touchlab.kermit.Logger
import frontend.*
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Inject
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import kotlin.system.exitProcess

@Inject
class EmulatorApplication(
    private val keyboardInput: PlatformKeyboardInput,
    private val runtimeHost: EmulatorRuntimeHost,
    private val audio: PlatformAudioPipeline,
    private val renderer: PlatformRenderer,
    private val viewModel: MainScreenViewModel,
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
            audio.close()
        }
    }

    private fun runComposeWindow() {
        application {
            val coroutineScope = rememberCoroutineScope()

            val windowState = remember { WindowState(size = DpSize(768.dp, 720.dp)) }

            DisposableEffect(Unit) {
                runtimeHost.start(
                    onFps = { fps -> coroutineScope.launch { viewModel.onFpsUpdated(fps) } },
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
                DisposableEffect(window) {
                    val listener = object : WindowAdapter() {
                        override fun windowActivated(event: WindowEvent) {
                            runtimeHost.resume()
                        }

                        override fun windowDeactivated(event: WindowEvent) {
                            runtimeHost.pause()
                        }
                    }
                    window.addWindowListener(listener)
                    onDispose { window.removeWindowListener(listener) }
                }

                val romPicker = remember(window) { FileChooser(window) }
                MainScreen(
                    viewModel = viewModel,
                    frameBuffer = runtimeHost.frameBuffer,
                    renderer = renderer,
                    keyboardInput = keyboardInput,
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
