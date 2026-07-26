package app

import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import com.github.ajalt.clikt.core.main
import di.FrontendComponent
import di.create

fun main(args: Array<String>) {
    val appComponent = FrontendComponent::class.create()
    val cliArgs = appComponent.cliArgParser
    cliArgs.main(args)
    Logger.setMinSeverity(if (cliArgs.debug) Severity.Debug else Severity.Info)
    appComponent.emulatorApplication.run()
}
