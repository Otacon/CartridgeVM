package di

import app.CliArgsParser
import app.EmulatorApplication
import frontend.OpenAlAudio
import frontend.OpenGlRenderer
import frontend.SwtWindow
import me.tatarka.inject.annotations.Component
import me.tatarka.inject.annotations.Scope
import nes.NesMachine
import nes.cartridge.InesParserComposite

@AppScope
@Component
abstract class AppComponent {

    abstract val cliArgParser: CliArgsParser
    abstract val inesParser: InesParserComposite
    abstract val swtWindow: SwtWindow
    abstract val openGlRenderer: OpenGlRenderer
    abstract val openAlAudio: OpenAlAudio
    abstract val nesMachine: NesMachine
    abstract val emulatorApplication: EmulatorApplication

}

@Scope
annotation class AppScope
