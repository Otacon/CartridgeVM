package di

import app.CliArgsParser
import frontend.GlfwWindow
import frontend.OpenAlAudio
import frontend.OpenGlRenderer
import me.tatarka.inject.annotations.Component
import me.tatarka.inject.annotations.Scope
import nes.NesMachine
import nes.cartridge.InesParser

@AppScope
@Component
abstract class AppComponent {

    abstract val cliArgParser: CliArgsParser
    abstract val inesParser: InesParser
    abstract val glfwWindow: GlfwWindow
    abstract val openGlRenderer: OpenGlRenderer
    abstract val openAlAudio: OpenAlAudio
    abstract val nesMachine: NesMachine

}

@Scope
annotation class AppScope
