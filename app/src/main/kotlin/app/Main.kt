package app

import com.github.ajalt.clikt.core.main
import frontend.*
import nes.NesMachine
import nes.Timing
import nes.cartridge.InesParser
import nes.cartridge.RomFormatException
import java.nio.file.Files
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    val parser = CliArgsParser()
    parser.main(args)

    try {
        val cartridge = InesParser.parse(Files.readAllBytes(parser.rom))
        if (parser.debug) {
            println("ROM: ${parser.rom}")
            println("Mapper: ${cartridge.mapperNumber}")
            println("PRG ROM: ${cartridge.prgRom.size / 1024} KiB")
            println("CHR: ${cartridge.chr.size / 1024} KiB ${if (cartridge.chrRam) "RAM" else "ROM"}")
            println("Mirroring: ${cartridge.mirroring}")
        }

        val machine = NesMachine(cartridge)
        GlfwWindow("CartridgeVM NES", 256 * 3, 240 * 3).use { window ->
            val renderer = OpenGlRenderer()
            renderer.init()
            val audio = OpenAlAudio()
            val input = KeyboardInput(window.handle, machine.controller, parser.debug)
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
                renderer.present(machine.ppu.framebuffer, window.width(), window.height())
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

    } catch (e: RomFormatException) {
        System.err.println(e.message)
        exitProcess(2)
    } catch (e: Exception) {
        if (parser.debug) {
            e.printStackTrace()
        } else {
            System.err.println(e.message ?: e::class.simpleName)
        }
        exitProcess(1)
    }
}
