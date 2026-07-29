@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)

package app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.window.ComposeViewport
import di.FrontendComponent
import di.ProvideFrontendComponent
import di.create
import frontend.*
import kotlinx.browser.document
import kotlinx.coroutines.launch
import nes.input.NesController

fun main() {
    val root = document.getElementById("app") ?: document.body ?: error("Missing document body")
    ComposeViewport(root) {
        val application = remember { WebEmulatorApplication() }
        application.Content()
    }
}

private class WebEmulatorApplication {
    private val component = FrontendComponent::class.create(Config(debug = true))
    private val machine = component.nesMachine
    private val keyboardInput = component.keyboardInput
    private val controllerInput = PlatformControllerInput(machine.controller)
    private val input = WebCombinedInput(machine.controller, keyboardInput, controllerInput)
    private val romPicker = FileChooser()
    private val runtimeHost = component.runtimeHost
    private val viewModel = component.viewModel

    init {
        component.runtimeInput.current = input
    }

    @Composable
    fun Content() {
        val coroutineScope = rememberCoroutineScope()

        DisposableEffect(Unit) {
            runtimeHost.start(
                onFps = { fps -> coroutineScope.launch { viewModel.onFpsUpdated(fps) } },
                onQuit = { coroutineScope.launch { machine.powerOff() } },
                onError = { coroutineScope.launch { machine.powerOff() } },
            )
            onDispose(runtimeHost::stop)
        }

        DisposableEffect(Unit) {
            onDispose(runtimeHost::close)
        }

        ProvideFrontendComponent(component) {
            MainScreen(
                frameBuffer = runtimeHost.frameBuffer,
                renderer = component.renderer,
                keyboardInput = keyboardInput,
                keyboardEventsEnabled = true,
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
}

private class WebCombinedInput(
    private val nesController: NesController,
    private val keyboard: PlatformKeyboardInput,
    private val controller: PlatformControllerInput,
) : BaseEmulatorInput() {
    override fun poll() {
        keyboard.poll()
        controller.poll()
        nesController.setButtons(keyboard.buttonMask() or controller.buttonMask())
        updateControlEdges(keyboard.consumeReset() || controller.consumeReset())
    }

    override fun quitRequested(): Boolean = false

    override fun close() {
        keyboard.close()
        controller.close()
    }
}
