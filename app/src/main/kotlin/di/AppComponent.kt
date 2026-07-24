package di

import app.CliArgsParser
import app.EmulatorApplication
import frontend.GlfwWindow
import frontend.OpenAlAudio
import frontend.OpenGlRenderer
import me.tatarka.inject.annotations.Component
import me.tatarka.inject.annotations.Scope
import nes.NesMachine
import nes.cartridge.InesParserComposite

@AppScope
@Component
abstract class AppComponent {

    abstract val cliArgParser: CliArgsParser
    abstract val inesParser: InesParserComposite
    abstract val glfwWindow: GlfwWindow
    abstract val openGlRenderer: OpenGlRenderer
    abstract val openAlAudio: OpenAlAudio
    abstract val nesMachine: NesMachine
    abstract val emulatorApplication: EmulatorApplication

}

@Scope
annotation class AppScope
