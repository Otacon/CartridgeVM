package app

import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import com.cyanotic.kassette.BuildKonfig
import com.github.ajalt.clikt.core.main
import dev.zacsweers.metro.createGraphFactory
import di.JvmFrontendComponent

fun main(args: Array<String>) {
    val cliArgs = CliArgsParser()
    cliArgs.main(args)
    val minSeverity = if (cliArgs.debug) Severity.Debug else Severity.entries[BuildKonfig.loggingLevel]
    Logger.setMinSeverity(minSeverity)
    createGraphFactory<JvmFrontendComponent.Factory>()
        .create(cliArgs.asConfig())
        .emulatorApplication
        .run()
}
