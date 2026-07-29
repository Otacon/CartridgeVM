package app

import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import com.github.ajalt.clikt.core.main
import di.JvmFrontendComponent
import di.create

fun main(args: Array<String>) {
    val cliArgs = CliArgsParser()
    cliArgs.main(args)
    Logger.setMinSeverity(if (cliArgs.debug) Severity.Debug else Severity.Info)
    JvmFrontendComponent::class.create(cliArgs.asConfig()).emulatorApplication.run()
}

