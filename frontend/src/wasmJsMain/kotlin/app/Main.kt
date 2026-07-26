@file:OptIn(ExperimentalWasmJsInterop::class)

package app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.darkColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import frontend.PlatformAudioPipeline
import frontend.PlatformControllerInput
import frontend.PlatformKeyboardInput
import frontend.PlatformRenderer
import frontend.RomData
import frontend.RomLoader
import kotlinx.browser.document
import kotlinx.browser.window
import nes.NesMachine
import nes.Timing
import nes.apu.DmcDma
import nes.apu.NesApu
import nes.cartridge.CartridgeSocket
import nes.cartridge.InesParserComposite
import nes.cartridge.InesParserUtils
import nes.cartridge.InesParserV1
import nes.cartridge.InesParserV2
import nes.cpu.Cpu6502
import nes.cpu.CpuBus
import nes.cpu.CpuStall
import nes.input.NesController
import nes.ppu.Ppu
import nes.ppu.PpuBus
import androidx.compose.ui.window.ComposeViewport
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Uint8Array
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.asList
import org.w3c.files.FileReader

fun main() {
    WebEmulatorApplication().run()
}

private class WebEmulatorApplication {
    private val graph = NesGraph()
    private val renderer = PlatformRenderer()
    private val audio = PlatformAudioPipeline()
    private val keyboardInput = PlatformKeyboardInput(graph.controller)
    private val controllerInput = PlatformControllerInput(graph.controller)
    private val input = WebCombinedInput(graph.controller, keyboardInput, controllerInput)
    private val romLoader = RomLoader(graph.parser, graph.machine)
    private val canvas = document.getElementById("screen") as HTMLCanvasElement
    private val romInput = document.getElementById("rom") as HTMLInputElement
    private var status by mutableStateOf("Choose a legally obtained .nes ROM. Keyboard and gamepads are active.")
    private var crtEnabled by mutableStateOf(false)
    private var paused by mutableStateOf(false)
    private var running = false
    private var frameStart = 0.0

    fun run() {
        renderer.attach(canvas)
        renderer.init(crtEnabled)
        keyboardInput.attach(canvas)
        configureRomPicker()
        @OptIn(ExperimentalComposeUiApi::class)
        ComposeViewport(document.getElementById("menu") as HTMLElement) {
            WebEmulatorMenu()
        }
        canvas.focus()
        window.requestAnimationFrame(::frame)
    }

    @Composable
    private fun WebEmulatorMenu() {
        MaterialTheme(colors = darkColors(primary = Color(0xFF8FD7FF), secondary = Color(0xFFF6F2E8))) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF191922))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(
                    onClick = {
                        audio.resume()
                        romInput.value = ""
                        romInput.click()
                    },
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFF6F2E8), contentColor = Color(0xFF101014)),
                    contentPadding = ButtonDefaults.ContentPadding,
                    modifier = Modifier.height(28.dp),
                ) { Text("Open ROM...", fontSize = 12.sp) }
                Button(
                    onClick = {
                        audio.resume()
                        crtEnabled = !crtEnabled
                        renderer.init(crtEnabled)
                        canvas.focus()
                    },
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = if (crtEnabled) Color(0xFF8FD7FF) else Color(0xFFF6F2E8),
                        contentColor = Color(0xFF101014),
                    ),
                    contentPadding = ButtonDefaults.ContentPadding,
                    modifier = Modifier.height(28.dp),
                ) { Text(if (crtEnabled) "CRT: On" else "CRT: Off", fontSize = 12.sp) }
                Button(
                    onClick = {
                        audio.resume()
                        paused = !paused
                        canvas.focus()
                    },
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = if (paused) Color(0xFF8FD7FF) else Color(0xFFF6F2E8),
                        contentColor = Color(0xFF101014),
                    ),
                    contentPadding = ButtonDefaults.ContentPadding,
                    modifier = Modifier.height(28.dp),
                ) { Text(if (paused) "Resume" else "Pause", fontSize = 12.sp) }
                Button(
                    onClick = {
                        audio.resume()
                        graph.machine.reset()
                        paused = false
                        canvas.focus()
                    },
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFF6F2E8), contentColor = Color(0xFF101014)),
                    contentPadding = ButtonDefaults.ContentPadding,
                    modifier = Modifier.height(28.dp),
                ) { Text("Reset", fontSize = 12.sp) }
                Text(status, color = Color(0xFFF6F2E8), fontSize = 12.sp)
            }
        }
    }

    private fun configureRomPicker() {
        romInput.onchange = {
            loadSelectedRom()
        }
    }

    private fun loadSelectedRom() {
        audio.resume()
        val file = romInput.files?.asList()?.firstOrNull() ?: return
        val reader = FileReader()
        reader.onload = {
            val bytes = (reader.result as ArrayBuffer).toByteArray()
            running = romLoader.load(RomData(file.name, bytes))
            paused = false
            status = if (running) "Loaded ${file.name}" else "Unable to load ${file.name}"
            canvas.focus()
        }
        reader.readAsArrayBuffer(file)
    }

    private fun frame(now: Double) {
        if (frameStart == 0.0) frameStart = now
        while (now - frameStart >= Timing.FRAME_NANOS / 1_000_000.0) {
            input.poll()
            if (input.consumePause()) paused = !paused
            if (input.consumeReset()) {
                graph.machine.reset()
                paused = false
            }
            if (running && !paused) {
                graph.machine.runUntilFrame(input::poll)
                audio.submit(graph.machine.apu.samples, graph.machine.apu.sampleCount)
                renderer.present(graph.machine.ppu.framebuffer, canvas.clientWidth, canvas.clientHeight)
            }
            frameStart += Timing.FRAME_NANOS / 1_000_000.0
        }
        window.requestAnimationFrame(::frame)
    }
}

private class WebCombinedInput(
    private val nesController: NesController,
    private val keyboard: PlatformKeyboardInput,
    private val controller: PlatformControllerInput,
) : frontend.BaseEmulatorInput() {
    override fun poll() {
        keyboard.poll()
        controller.poll()
        nesController.setButtons(keyboard.buttonMask() or controller.buttonMask())
        updateControlEdges(keyboard.consumePause() || controller.consumePause(), keyboard.consumeReset() || controller.consumeReset())
    }

    override fun quitRequested(): Boolean = false
}

private class NesGraph {
    private val cartridgeSocket = CartridgeSocket()
    val controller = NesController()
    private val cpuStall = CpuStall()
    private val dmcDma = DmcDma(cartridgeSocket, cpuStall)
    private val ppuBus = PpuBus(cartridgeSocket)
    private val ppu = Ppu(ppuBus)
    private val apu = NesApu(dmcDma)
    private val cpuBus = CpuBus(cartridgeSocket, ppu, controller, apu, cpuStall)
    private val cpu = Cpu6502(cpuBus)
    val machine = NesMachine(controller, cartridgeSocket, ppu, apu, cpu)
    private val parserUtils = InesParserUtils()
    val parser = InesParserComposite(InesParserV1(parserUtils), InesParserV2(parserUtils), parserUtils)
}

private fun ArrayBuffer.toByteArray(): ByteArray {
    val view = arrayBufferToUint8Array(this)
    return ByteArray(uint8ArrayLength(view)) { index -> uint8ArrayGet(view, index).toByte() }
}

@JsFun("(buffer) => new Uint8Array(buffer)")
private external fun arrayBufferToUint8Array(buffer: ArrayBuffer): Uint8Array

@JsFun("(array) => array.length")
private external fun uint8ArrayLength(array: Uint8Array): Int

@JsFun("(array, index) => array[index]")
private external fun uint8ArrayGet(array: Uint8Array, index: Int): Int
