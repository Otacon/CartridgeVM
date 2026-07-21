package app

import frontend.FramePacer
import frontend.GlfwWindow
import frontend.KeyboardInput
import frontend.OpenGlRenderer
import nes.NesMachine
import nes.Timing
import nes.cartridge.InesParser
import nes.cartridge.RomFormatException
import java.nio.file.Files
import java.nio.file.Path
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    val cli = try { CliArguments.parse(args) } catch (e: IllegalArgumentException) { System.err.println(e.message); System.err.println(CliArguments.usage()); exitProcess(2) }
    if (cli.romPath == null) {
        System.err.println(CliArguments.usage())
        exitProcess(2)
    }
    try {
        val path = Path.of(cli.romPath)
        if (!Files.exists(path)) throw IllegalArgumentException("File not found: $path")
        if (!Files.isReadable(path)) throw IllegalArgumentException("Unreadable file: $path")
        val cartridge = InesParser.parse(Files.readAllBytes(path))
        if (cli.debug) {
            println("ROM: $path")
            println("Mapper: ${cartridge.mapperNumber}")
            println("PRG ROM: ${cartridge.prgRom.size / 1024} KiB")
            println("CHR: ${cartridge.chr.size / 1024} KiB ${if (cartridge.chrRam) "RAM" else "ROM"}")
            println("Mirroring: ${cartridge.mirroring}")
        }
        val machine = NesMachine(cartridge)
        GlfwWindow("CartridgeVM NES", 256 * 3, 240 * 3).use { window ->
            val renderer = OpenGlRenderer()
            renderer.init()
            val input = KeyboardInput(window.handle, machine.controller, cli.debug)
            val pacer = FramePacer(Timing.FRAME_NANOS)
            if (cli.debug) {
                println("Frame limiter: ${if (cli.unlimited) "disabled" else "enabled"}")
                println("Target FPS: ${"%.3f".format(Timing.FRAME_RATE)}")
            }
            var paused = false
            var frames = 0
            var fpsTime = System.nanoTime()
            while (!window.shouldClose()) {
                window.pollEvents()
                input.poll()
                if (input.consumePause()) paused = !paused
                if (input.consumeReset()) { machine.reset(); paused = false }
                if (input.quitRequested()) window.requestClose()
                if (!paused) machine.runUntilFrame() else Thread.sleep(8)
                renderer.present(machine.ppu.framebuffer, window.width(), window.height())
                window.swapBuffers()
                if (!cli.unlimited) pacer.waitForNextFrame()
                frames++
                if (cli.debug) {
                    val now = System.nanoTime()
                    if (now - fpsTime >= 1_000_000_000L) { println("FPS: $frames"); frames = 0; fpsTime = now }
                }
            }
            renderer.close()
        }
    } catch (e: RomFormatException) {
        System.err.println(e.message); exitProcess(2)
    } catch (e: IllegalArgumentException) {
        System.err.println(e.message); exitProcess(2)
    } catch (e: Exception) {
        if (cli.debug) e.printStackTrace() else System.err.println(e.message ?: e::class.simpleName)
        exitProcess(1)
    }
}
