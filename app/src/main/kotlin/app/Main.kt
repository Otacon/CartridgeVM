package app

import com.github.ajalt.clikt.core.main
import frontend.*
import nes.NesMachine
import nes.Timing
import nes.cartridge.InesParser
import nes.cartridge.RomFormatException
import org.slf4j.LoggerFactory
import org.slf4j.event.Level
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    val parser = CliArgsParser()
    parser.main(args)
    val log = LoggerFactory.getLogger("Main")
    val loggingLevel = if (parser.debug) Level.DEBUG else Level.INFO
    log.atLevel(loggingLevel)
    try {
        val cartridge = InesParser.parse(parser.rom)
        val machine = NesMachine(cartridge)
        log.info("Emulation started")
        GlfwWindow().use { window ->
            val handle = window.create(256 * 3, 240 * 3)
            val renderer = OpenGlRenderer()
            renderer.init()
            val audio = OpenAlAudio()
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
                if (!parser.unlimited) {
                    pacer.waitForNextFrame()
                }
                frames++
                val now = System.nanoTime()
                if (now - fpsTime >= 1_000_000_000L) {
                    window.title = "CartridgeVM NES [${parser.rom.fileName} | FPS: $frames]"
                    frames = 0
                    fpsTime = now
                }
            }
            // TODO ensure this is closed regardless of try/catch so no leaks are present
            audio.close()
            renderer.close()
        }
        log.info("Emulation finished")
    } catch (e: RomFormatException) {
        log.error("Unable to load rom", e)
        System.err.println(e.message)
        exitProcess(2)
    } catch (e: Exception) {
        log.error("Runtime error", e)
        exitProcess(1)
    }
}
