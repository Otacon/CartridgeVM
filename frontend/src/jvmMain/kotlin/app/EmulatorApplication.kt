package app

import co.touchlab.kermit.Logger
import frontend.*
import me.tatarka.inject.annotations.Inject
import nes.NesMachine
import nes.Timing
import nes.cartridge.InesParserComposite
import nes.cartridge.RomFormatException
import java.nio.file.Path
import kotlin.io.path.readBytes
import kotlin.system.exitProcess

@Inject
class EmulatorApplication(
    private val cliArgs: CliArgsParser,
    private val inesParser: InesParserComposite,
    private val renderer: OpenGlRenderer,
    private val audio: OpenAlAudio,
    private val machine: NesMachine,
    private val window: SwtWindow,
) {
    private val log = Logger.withTag("EmulatorApplication")
    private var currentRom: Path? = null

    fun run() {
        try {
            log.i { "Emulation started" }
            window.use {
                renderer.use {
                    runWindowLoop()
                }
            }
            log.i { "Emulation finished" }
        } catch (e: RomFormatException) {
            log.e(e) { "Unable to load rom" }
            exitProcess(2)
        } catch (e: Exception) {
            log.e(e) { "Runtime error" }
            exitProcess(1)
        } finally {
            audio.close()
        }
    }

    private fun runWindowLoop() {
        val controllerInput = if (cliArgs.controller) {
            log.d { "Initializing GLFW controller input" }
            ControllerInput(machine.controller)
        } else {
            null
        }
        log.d { "Creating SWT OpenGL window" }
        val canvas = if (cliArgs.crt) window.create(256 * 4, 240 * 4) else window.create(256 * 3, 240 * 3)
        window.onRomSelected = { loadRom(it) }
        log.d { "SWT OpenGL window created" }
        log.d { "Initializing OpenGL renderer" }
        renderer.init(canvas, cliArgs.crt)
        log.d { "OpenGL renderer initialized" }
        cliArgs.rom?.let { loadRom(it) }
        val input = controllerInput ?: KeyboardInput(window, machine.controller)
        log.d { "Input initialized" }
        val pollInput = {
            window.pollEvents()
            input.poll()
        }
        val pacer = FramePacer(Timing.FRAME_NANOS)
        var paused = false
        var frames = 0
        var fpsTime = System.nanoTime()
        input.use {
            while (!window.shouldClose()) {
                pollInput()
                if (input.consumePause()) {
                    paused = !paused
                }
                if (input.consumeReset()) {
                    machine.reset()
                    paused = false
                }
                if (input.quitRequested()) {
                    window.requestClose()
                }
                if (window.shouldClose()) break
                if (currentRom != null && !paused) {
                    machine.runUntilFrame(pollInput)
                    audio.submit(machine.apu.samples, machine.apu.sampleCount)
                } else {
                    Thread.sleep(8)
                }
                if (window.shouldClose()) break
                window.makeCurrent()
                renderer.present(machine.ppu.framebuffer, window.width, window.height)
                window.swapBuffers()
                if (!cliArgs.unlimited) {
                    pacer.waitForNextFrame()
                }
                frames++
                val now = System.nanoTime()
                if (now - fpsTime >= 1_000_000_000L) {
                    val romName = currentRom?.fileName?.toString() ?: "No ROM"
                    window.title = "CartridgeVM NES [$romName | FPS: $frames]"
                    frames = 0
                    fpsTime = now
                }
            }
        }
    }

    private fun loadRom(path: Path) {
        try {
            val cartridge = inesParser.parse(path.readBytes())
            machine.insert(cartridge)
            machine.reset()
            currentRom = path
            window.title = "CartridgeVM NES [${path.fileName}]"
            log.i { "Loaded ROM: ${path.fileName}" }
        } catch (e: RomFormatException) {
            log.e(e) { "Unable to load rom: $path" }
        }
    }

}
