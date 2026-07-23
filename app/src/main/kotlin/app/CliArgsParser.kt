package app

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.path
import me.tatarka.inject.annotations.Inject
import java.nio.file.Path

@Inject
class CliArgsParser : CliktCommand("cartridgevm") {
    val debug: Boolean by option(names = arrayOf("-d", "--debug"), help = "Enable debug logging")
        .flag()

    val unlimited: Boolean by option(names = arrayOf("-u", "--unlimited"), help = "Allow unlimited framerate")
        .flag()

    val rom: Path by argument(help = "Path to .nes ROM")
        .path(mustExist = true, canBeDir = false)

    override fun run() = Unit

}
