package di

import app.CliArgsParser
import app.EmulatorApplication
import frontend.PlatformAudioPipeline
import frontend.PlatformRenderer
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
    fun renderer(): PlatformRenderer = PlatformRenderer()

    @AppScope
    @Provides
    fun audio(): PlatformAudioPipeline = PlatformAudioPipeline()

    @AppScope
    @Provides
    fun emulatorApplication(
        cliArgs: CliArgsParser,
        inesParser: InesParserComposite,
        renderer: PlatformRenderer,
        audio: PlatformAudioPipeline,
        machine: NesMachine,
    ): EmulatorApplication = EmulatorApplication(cliArgs, inesParser, renderer, audio, machine)

}

@Scope
annotation class AppScope
