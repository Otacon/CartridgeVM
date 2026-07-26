@file:OptIn(ExperimentalWasmJsInterop::class)

package app

import frontend.*
import kotlinx.browser.document
import kotlinx.browser.window
import nes.Timing
import nes.di.NesComponent
import nes.di.create
import nes.input.NesController
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
    private val nesComponent = NesComponent::class.create()
    private val renderer = PlatformRenderer()
    private val audio = PlatformAudioPipeline()
    private val machine = nesComponent.nesMachine
    private val keyboardInput = PlatformKeyboardInput(machine.controller)
    private val controllerInput = PlatformControllerInput(machine.controller)
    private val input = WebCombinedInput(machine.controller, keyboardInput, controllerInput)
    private val romLoader = RomLoader(nesComponent.inesParser, machine)
    private val romInput = document.getElementById("rom") as HTMLInputElement
    private lateinit var canvas: HTMLCanvasElement
    private lateinit var statusElement: HTMLElement
    private lateinit var pauseItem: HTMLElement
    private lateinit var crtItem: HTMLElement
    private var status = "Choose a legally obtained .nes ROM. Keyboard and gamepads are active."
    private var crtEnabled = false
    private var paused = false
    private var running = false
    private var frameStart = 0.0

    fun run() {
        renderShell()
        configureRomPicker()
        renderer.attach(canvas)
        renderer.init(crtEnabled)
        keyboardInput.attach(canvas)
        canvas.focus()
        window.requestAnimationFrame(::frame)
    }

    private fun renderShell() {
        val app = document.getElementById("app") as HTMLElement
        app.innerHTML = """
            <div class="shell">
                <div class="menubar">
                    <div class="menu">
                        <button class="menu-title" type="button">File</button>
                        <div class="dropdown">
                            <button id="openRom" class="menu-item" type="button">Open ROM...</button>
                            <div class="separator"></div>
                            <button id="closeRom" class="menu-item" type="button">Close ROM</button>
                        </div>
                    </div>
                    <div class="menu">
                        <button class="menu-title" type="button">Emulation</button>
                        <div class="dropdown">
                            <button id="pause" class="menu-item" type="button">Pause</button>
                            <button id="reset" class="menu-item" type="button">Reset</button>
                        </div>
                    </div>
                    <div class="menu">
                        <button class="menu-title" type="button">View</button>
                        <div class="dropdown">
                            <button id="crt" class="menu-item" type="button">Enable CRT</button>
                        </div>
                    </div>
                    <div id="status" class="status"></div>
                </div>
                <div class="screen-wrap">
                    <canvas id="screen" width="256" height="240" tabindex="0"></canvas>
                </div>
            </div>
        """.trimIndent()

        canvas = document.getElementById("screen") as HTMLCanvasElement
        statusElement = document.getElementById("status") as HTMLElement
        pauseItem = document.getElementById("pause") as HTMLElement
        crtItem = document.getElementById("crt") as HTMLElement
        configureMenus()
        updateMenuState()
    }

    private fun configureMenus() {
        (document.getElementById("openRom") as HTMLElement).onclick = {
            audio.resume()
            romInput.value = ""
            romInput.click()
            canvas.focus()
        }

        (document.getElementById("closeRom") as HTMLElement).onclick = {
            running = false
            paused = false
            status = "No ROM loaded"
            updateMenuState()
            canvas.focus()
        }

        pauseItem.onclick = {
            audio.resume()
            paused = !paused
            updateMenuState()
            canvas.focus()
        }

        (document.getElementById("reset") as HTMLElement).onclick = {
            audio.resume()
            machine.reset()
            paused = false
            updateMenuState()
            canvas.focus()
        }

        crtItem.onclick = {
            audio.resume()
            crtEnabled = !crtEnabled
            renderer.init(crtEnabled)
            updateMenuState()
            canvas.focus()
        }
    }

    private fun updateMenuState() {
        statusElement.textContent = status
        pauseItem.textContent = if (paused) "Resume" else "Pause"
        crtItem.textContent = if (crtEnabled) "Disable CRT" else "Enable CRT"
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
            updateMenuState()
            canvas.focus()
        }
        reader.readAsArrayBuffer(file)
    }

    private fun frame(now: Double) {
        if (frameStart == 0.0) frameStart = now
        while (now - frameStart >= Timing.FRAME_NANOS / 1_000_000.0) {
            input.poll()
            if (input.consumePause()) {
                paused = !paused
                updateMenuState()
            }
            if (input.consumeReset()) {
                machine.reset()
                paused = false
                updateMenuState()
            }
            if (running && !paused) {
                machine.runUntilFrame(input::poll)
                audio.submit(machine.apu.samples, machine.apu.sampleCount)
                renderer.present(machine.ppu.framebuffer, canvas.clientWidth, canvas.clientHeight)
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
) : BaseEmulatorInput() {
    override fun poll() {
        keyboard.poll()
        controller.poll()
        nesController.setButtons(keyboard.buttonMask() or controller.buttonMask())
        updateControlEdges(
            keyboard.consumePause() || controller.consumePause(),
            keyboard.consumeReset() || controller.consumeReset()
        )
    }

    override fun quitRequested(): Boolean = false
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
