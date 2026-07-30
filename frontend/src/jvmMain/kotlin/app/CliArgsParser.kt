package app

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.path
import frontend.Config
import me.tatarka.inject.annotations.Inject
import nes.cartridge.RomData
import java.nio.file.Path
import kotlin.io.path.readBytes

@Inject
class CliArgsParser : CliktCommand("cartridgevm") {
    val debug: Boolean by option(names = arrayOf("-d", "--debug"), help = "Enable debug logging")
        .flag()

    val crt: Boolean by option(names = arrayOf("--crt"), help = "Enable consumer CRT display emulation")
        .flag()

    val rom: Path? by argument(help = "Path to .nes ROM")
        .path(mustExist = true, canBeDir = false)
        .optional()

    override fun run() = Unit

    fun asConfig(): Config {
        val romData = rom?.let { RomData(it.fileName.toString(), it.readBytes()) }
        return Config(
            debug = debug,
            crt = crt,
            rom = romData,
        )
    }

}
