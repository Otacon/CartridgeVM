package di

import frontend.*
import io.Nes20Db
import io.Nes20DbCsv
import me.tatarka.inject.annotations.Component
import me.tatarka.inject.annotations.Provides
import me.tatarka.inject.annotations.Scope
import nes.NesMachine
import nes.cartridge.InesParserComposite
import nes.cartridge.InesParserUtils
import nes.cartridge.InesParserV1
import nes.cartridge.InesParserV2
import nes.di.NesComponent
import nes.di.create

@AppScope
@Component
abstract class FrontendComponent(
    private val providedConfig: Config,
) {
    abstract val inesParser: InesParserComposite
    abstract val nesMachine: NesMachine
    abstract val renderer: PlatformRenderer
    abstract val audio: PlatformAudioPipeline
    abstract val keyboardInput: PlatformKeyboardInput
    abstract val runtimeInput: DelegatingEmulatorInput
    abstract val runtimeHost: EmulatorRuntimeHost
    abstract val viewModel: MainScreenViewModel

    @AppScope
    @Provides
    fun nesComponent(): NesComponent = NesComponent::class.create()

    @AppScope
    @Provides
    fun inesParserUtils(): InesParserUtils = InesParserUtils()

    @AppScope
    @Provides
    fun inesParserV1(utils: InesParserUtils): InesParserV1 = InesParserV1(utils)

    @AppScope
    @Provides
    fun inesParserV2(utils: InesParserUtils): InesParserV2 = InesParserV2(utils)

    @AppScope
    @Provides
    fun nes20Db(): Nes20Db = Nes20DbCsv("nes20db.csv")

    @AppScope
    @Provides
    fun inesParser(
        inesParserV1: InesParserV1,
        inesParserV2: InesParserV2,
        nes20Db: Nes20Db,
        utils: InesParserUtils,
    ): InesParserComposite = InesParserComposite(inesParserV1, inesParserV2, nes20Db, utils)

    @AppScope
    @Provides
    fun nesMachine(nesComponent: NesComponent): NesMachine = nesComponent.nesMachine

    @AppScope
    @Provides
    fun renderer(): PlatformRenderer = PlatformRenderer()

    @AppScope
    @Provides
    fun audio(): PlatformAudioPipeline = PlatformAudioPipeline()

    @AppScope
    @Provides
    fun config(): Config = providedConfig

    @AppScope
    @Provides
    fun keyboardInput(
        machine: NesMachine,
    ): PlatformKeyboardInput = PlatformKeyboardInput(machine.controller)

    @AppScope
    @Provides
    fun runtimeInput(
        config: Config,
        keyboardInput: PlatformKeyboardInput,
    ): DelegatingEmulatorInput = DelegatingEmulatorInput(keyboardInput.takeUnless { config.controller })

    @AppScope
    @Provides
    fun runtimeHost(
        machine: NesMachine,
        audio: PlatformAudioPipeline,
        input: DelegatingEmulatorInput,
    ): EmulatorRuntimeHost = EmulatorRuntimeHost(
        machine = machine,
        audio = audio,
        input = input,
    )
}

@Scope
annotation class AppScope
