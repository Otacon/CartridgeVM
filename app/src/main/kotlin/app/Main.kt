package app

import com.github.ajalt.clikt.core.main
import di.AppComponent
import di.create
import org.slf4j.LoggerFactory
import org.slf4j.event.Level

fun main(args: Array<String>) {
    val appComponent = AppComponent::class.create()
    val cliArgs = appComponent.cliArgParser
    cliArgs.main(args)
    val log = LoggerFactory.getLogger("Main")
    val loggingLevel = if (cliArgs.debug) Level.DEBUG else Level.INFO
    log.atLevel(loggingLevel)
    appComponent.emulatorApplication.run()
}
