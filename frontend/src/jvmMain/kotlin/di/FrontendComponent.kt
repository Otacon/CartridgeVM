package di

import app.CliArgsParser
import app.EmulatorApplication
import frontend.OpenAlAudio
import frontend.OpenGlRenderer
import frontend.SwtWindow
import me.tatarka.inject.annotations.Component
import me.tatarka.inject.annotations.Provides
import me.tatarka.inject.annotations.Scope
import nes.NesMachine
import nes.cartridge.InesParserComposite
import nes.di.NesComponent
import nes.di.create

@AppScope
@Component
abstract class FrontendComponent {

    abstract val cliArgParser: CliArgsParser
    abstract val emulatorApplication: EmulatorApplication

    @AppScope
    @Provides
    fun nesComponent(): NesComponent = NesComponent::class.create()

    @AppScope
    @Provides
    fun inesParser(nesComponent: NesComponent): InesParserComposite = nesComponent.inesParser

    @AppScope
    @Provides
    fun nesMachine(nesComponent: NesComponent): NesMachine = nesComponent.nesMachine

    @AppScope
    @Provides
    fun cliArgsParser(): CliArgsParser = CliArgsParser()

    @AppScope
    @Provides
    fun swtWindow(): SwtWindow = SwtWindow()

    @AppScope
    @Provides
    fun openGlRenderer(): OpenGlRenderer = OpenGlRenderer()

    @AppScope
    @Provides
    fun openAlAudio(): OpenAlAudio = OpenAlAudio()

    @AppScope
    @Provides
    fun emulatorApplication(
        cliArgs: CliArgsParser,
        inesParser: InesParserComposite,
        renderer: OpenGlRenderer,
        audio: OpenAlAudio,
        machine: NesMachine,
        window: SwtWindow,
    ): EmulatorApplication = EmulatorApplication(cliArgs, inesParser, renderer, audio, machine, window)

}

@Scope
annotation class AppScope
