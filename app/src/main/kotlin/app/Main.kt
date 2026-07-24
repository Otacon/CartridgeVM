package app

import com.github.ajalt.clikt.core.main
import di.AppComponent
import di.create
import frontend.FramePacer
import frontend.KeyboardInput
import nes.Timing
import nes.cartridge.RomFormatException
import org.slf4j.LoggerFactory
import org.slf4j.event.Level
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    val log = LoggerFactory.getLogger("Main")
    val appComponent = AppComponent::class.create()
    val cliArgs = appComponent.cliArgParser
    cliArgs.main(args)
    val loggingLevel = if (cliArgs.debug) Level.DEBUG else Level.INFO
    log.atLevel(loggingLevel)
    val inesParser = appComponent.inesParser
    val renderer = appComponent.openGlRenderer
    val audio = appComponent.openAlAudio
    try {
        val cartridge = inesParser.parse(cliArgs.rom)
        val machine = appComponent.nesMachine.also {
            it.insert(cartridge)
            it.reset()
        }
        log.info("Emulation started")
        appComponent.glfwWindow.use { window ->
            val handle = window.create(256 * 3, 240 * 3)
            renderer.init()
            val input = KeyboardInput(handle, machine.controller)
            val pacer = FramePacer(Timing.FRAME_NANOS)
            var paused = false
            var frames = 0
            var fpsTime = System.nanoTime()
            while (!window.shouldClose()) {
                window.pollEvents()
                input.poll()
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
                    machine.runUntilFrame()
                } else {
                    Thread.sleep(8)
                }
                if (!paused) {
                    audio.submit(machine.apu.samples, machine.apu.sampleCount)
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
