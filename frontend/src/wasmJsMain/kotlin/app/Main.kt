@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class, ExperimentalWasmJsInterop::class)

package app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.window.ComposeViewport
import di.WasmFrontendComponent
import di.create
import frontend.*
import kotlinx.browser.document
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Inject
import nes.NesMachine

fun main() {
    val appComponent = WasmFrontendComponent::class.create(Config(debug = true))
    val root = document.getElementById("app") ?: document.body ?: error("Missing document body")
    ComposeViewport(root) {
        val application = remember { appComponent.webEmulatorApplication }
        application.Content()
    }
}

@Inject
class WebEmulatorApplication(
    private val machine: NesMachine,
    private val keyboardInput: PlatformKeyboardInput,
    private val runtimeHost: EmulatorRuntimeHost,
    private val viewModel: MainScreenViewModel,
    private val renderer: PlatformRenderer,
) {
    private val romPicker = FileChooser()

    @Composable
    fun Content() {
        val coroutineScope = rememberCoroutineScope()

        DisposableEffect(Unit) {
            val activityListener = addPageActivityListener {
                coroutineScope.launch {
                    if (isPageActive()) runtimeHost.resume() else runtimeHost.pause()
                }
            }
            runtimeHost.start(
                onFps = { fps -> coroutineScope.launch { viewModel.onFpsUpdated(fps) } },
                onError = { coroutineScope.launch { machine.powerOff() } },
            )
            onDispose {
                removePageActivityListener(activityListener)
                runtimeHost.stop()
            }
        }

        DisposableEffect(Unit) {
            onDispose(runtimeHost::close)
        }

        MainScreen(
            viewModel = viewModel,
            frameBuffer = runtimeHost.frameBuffer,
            renderer = renderer,
            keyboardInput = keyboardInput,
            onTitleChanged = { document.title = it },
            onOpenRomClick = {
                coroutineScope.launch {
                    val rom = romPicker.pickRom()
                    viewModel.onRomSelected(rom)
                }
            },
        )
    }
}
