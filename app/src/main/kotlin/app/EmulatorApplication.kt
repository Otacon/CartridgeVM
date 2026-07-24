package app

import di.AppScope
import frontend.FramePacer
import frontend.GlfwWindow
import frontend.KeyboardInput
import frontend.OpenAlAudio
import frontend.OpenGlRenderer
import frontend.ControllerInput
import me.tatarka.inject.annotations.Inject
import nes.NesMachine
import nes.Timing
import nes.cartridge.InesParser
import nes.cartridge.RomFormatException
import org.slf4j.LoggerFactory
import kotlin.system.exitProcess

@Inject
@AppScope
class EmulatorApplication(
    private val cliArgs: CliArgsParser,
    private val inesParser: InesParser,
    private val renderer: OpenGlRenderer,
    private val audio: OpenAlAudio,
    private val machine: NesMachine,
    private val window: GlfwWindow,
) {
    private val log = LoggerFactory.getLogger("EmulatorApplication")

    fun run() {
        try {
            val cartridge = inesParser.parse(cliArgs.rom)
            machine.insert(cartridge)
            machine.reset()
            log.info("Emulation started")
            window.use { runWindowLoop() }
            log.info("Emulation finished")
        } catch (e: RomFormatException) {
            log.error("Unable to load rom", e)
            exitProcess(2)
        } catch (e: Exception) {
            log.error("Runtime error", e)
            exitProcess(1)
        } finally {
            audio.close()
            renderer.close()
        }
    }

    private fun runWindowLoop() {
        val handle = window.create(256 * 3, 240 * 3)
        renderer.init()
        val input = if (cliArgs.controller) {
            ControllerInput(machine.controller)
        } else {
            KeyboardInput(handle, machine.controller)
        }
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
                if (!paused) {
                    machine.runUntilFrame(pollInput)
                    audio.submit(machine.apu.samples, machine.apu.sampleCount)
                } else {
                    Thread.sleep(8)
                }
                renderer.present(machine.ppu.framebuffer, window.width, window.height)
                window.swapBuffers()
                if (!cliArgs.unlimited) {
                    pacer.waitForNextFrame()
                }
                frames++
                val now = System.nanoTime()
                if (now - fpsTime >= 1_000_000_000L) {
                    window.title = "CartridgeVM NES [${cliArgs.rom.fileName} | FPS: $frames]"
                    frames = 0
                    fpsTime = now
                }
            }
        }
    }
}
