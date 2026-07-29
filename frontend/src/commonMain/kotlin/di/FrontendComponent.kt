package di

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import frontend.Config
import frontend.DelegatingEmulatorInput
import frontend.EmulatorRuntimeHost
import frontend.MainScreenViewModel
import frontend.PlatformAudioPipeline
import frontend.PlatformKeyboardInput
import frontend.PlatformRenderer
import me.tatarka.inject.annotations.Component
import me.tatarka.inject.annotations.Provides
import me.tatarka.inject.annotations.Scope
import nes.NesMachine
import nes.Timing
import nes.cartridge.InesParserComposite
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
    fun inesParser(nesComponent: NesComponent): InesParserComposite = nesComponent.inesParser

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
        frameNanos = Timing.FRAME_NANOS,
    )
}

@Scope
annotation class AppScope

val LocalFrontendComponent = staticCompositionLocalOf<FrontendComponent> {
    error("FrontendComponent was not provided")
}

@Composable
fun ProvideFrontendComponent(
    component: FrontendComponent,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalFrontendComponent provides component) {
        content()
    }
}
