@file:OptIn(ExperimentalWasmJsInterop::class, androidx.compose.ui.ExperimentalComposeUiApi::class)

package app

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.ComposeViewport
import frontend.BaseEmulatorInput
import frontend.ComposeMenuBar
import frontend.ComposeSkiaScreen
import frontend.PlatformAudioPipeline
import frontend.PlatformControllerInput
import frontend.PlatformKeyboardInput
import frontend.PlatformRenderer
import frontend.RomData
import frontend.RomLoader
import frontend.RomPicker
import kotlinx.browser.document
import kotlinx.coroutines.launch
import nes.Timing
import nes.di.NesComponent
import nes.di.create
import nes.input.NesController
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.asList
import org.w3c.files.File
import org.w3c.files.FileReader
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

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
    private val romLoader = RomLoader(nesComponent.inesParser, machine)
    private val romPicker = WebRomPicker()
    private val machineLock = Any()

    @Composable
    fun Content() {
        var loadedRom by remember { mutableStateOf(romLoader.currentRomName) }
        var crt by remember { mutableStateOf(false) }
        val coroutineScope = rememberCoroutineScope()

        ComposeMenuBar(
            onOpenRom = {
                coroutineScope.launch {
                    audio.resume()
                    val rom = romPicker.pickRom()
                    if (rom != null && romLoader.load(rom)) {
                        loadedRom = romLoader.currentRomName
                    }
                }
            },
            onExit = { loadedRom = null },
            onMenuOpened = { keyboardInput.releaseAll() },
            onMenuDismissed = {  },
            crtEnabled = crt,
            onToggleCrt = {
                audio.resume()
                crt = !crt
            },
            modifier = Modifier.fillMaxSize(),
        ) { contentModifier ->
            ComposeSkiaScreen(
                machine = machine,
                machineLock = machineLock,
                renderer = renderer,
                audio = audio,
                input = input,
                keyboardInput = keyboardInput,
                crt = crt,
                frameNanos = Timing.FRAME_NANOS,
                unlimited = false,
                running = loadedRom != null,
                modifier = contentModifier,
                onQuit = {
                    loadedRom = null
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

private class WebRomPicker : RomPicker {
    private val input = document.getElementById("rom") as HTMLInputElement

    override suspend fun pickRom(): RomData? = suspendCoroutine { continuation ->
        input.value = ""
        input.onchange = {
            val file = input.files?.asList()?.firstOrNull()
            if (file == null) {
                continuation.resume(null)
            } else {
                file.readRomData { continuation.resume(it) }
            }
        }
        input.click()
    }

    private fun File.readRomData(onLoaded: (RomData?) -> Unit) {
        val reader = FileReader()
        reader.onload = {
            onLoaded(RomData(name, reader.result.toByteArray()))
        }
        reader.onerror = {
            onLoaded(null)
        }
        reader.readAsArrayBuffer(this)
    }
}

private fun JsAny?.toByteArray(): ByteArray {
    val buffer = requireNotNull(this) { "Missing file contents" }
    return ByteArray(arrayBufferLength(buffer)) { index -> arrayBufferGet(buffer, index).toByte() }
}

@JsFun("(buffer) => new Uint8Array(buffer).length")
private external fun arrayBufferLength(buffer: JsAny): Int

@JsFun("(buffer, index) => new Uint8Array(buffer)[index]")
private external fun arrayBufferGet(buffer: JsAny, index: Int): Int
