package app

import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import com.cyanotic.kassette.BuildKonfig
import com.github.ajalt.clikt.core.main
import di.JvmFrontendComponent
import dev.zacsweers.metro.createGraphFactory

fun main(args: Array<String>) {
    val cliArgs = CliArgsParser()
    cliArgs.main(args)
    Logger.setMinSeverity(Severity.entries[BuildKonfig.loggingLevel])
    createGraphFactory<JvmFrontendComponent.Factory>()
        .create(cliArgs.asConfig())
        .emulatorApplication
        .run()
}
