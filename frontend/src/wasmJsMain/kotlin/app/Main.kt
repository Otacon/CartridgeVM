@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)

package app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.window.ComposeViewport
import frontend.*
import kotlinx.browser.document
import kotlinx.coroutines.launch
import nes.di.NesComponent
import nes.di.create
import nes.input.NesController

fun main() {
    val root = document.getElementById("app") ?: document.body ?: error("Missing document body")
    ComposeViewport(root) {
        val application = remember { WebEmulatorApplication() }
        application.Content()
    }
}

private class WebEmulatorApplication {
    private val nesComponent = NesComponent::class.create()
    private val renderer = PlatformRenderer()
    private val audio = PlatformAudioPipeline()
    private val machine = nesComponent.nesMachine
    private val keyboardInput = PlatformKeyboardInput(machine.controller)
    private val controllerInput = PlatformControllerInput(machine.controller)
    private val input = WebCombinedInput(machine.controller, keyboardInput, controllerInput)
    private val romPicker = FileChooser()
    private val machineLock = Any()
    private val viewModel = MainScreenViewModel(config = Config(debug = true), machine, nesComponent.inesParser)

    @Composable
    fun Content() {
        val coroutineScope = rememberCoroutineScope()

        MainScreen(
            onOpenRomClick = {
                coroutineScope.launch {
                    val rom = romPicker.pickRom()
                    viewModel.onRomSelected(rom)
                }
            },
            onTitleChanged = { document.title = it },
            unlimited = false,
            keyboardInput = keyboardInput,
            keyboardEventsEnabled = true,
            input = input,
            machine = machine,
            machineLock = machineLock,
            renderer = renderer,
            audio = audio,
            viewModel = viewModel,
        )
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
        updateControlEdges(
            keyboard.consumePause() || controller.consumePause(),
            keyboard.consumeReset() || controller.consumeReset(),
        )
    }

    override fun quitRequested(): Boolean = false

    override fun close() {
        keyboard.close()
        controller.close()
    }
}

