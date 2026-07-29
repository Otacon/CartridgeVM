package di

import app.WebEmulatorApplication
import frontend.Config
import frontend.DelegatingEmulatorInput
import frontend.EmulatorRuntimeHost
import frontend.MainScreenViewModel
import frontend.PlatformKeyboardInput
import frontend.PlatformRenderer
import me.tatarka.inject.annotations.Component
import me.tatarka.inject.annotations.Provides
import nes.NesMachine

@AppScope
@Component
abstract class WasmFrontendComponent(
    @get:Provides val config: Config,
) {
    abstract val webEmulatorApplication: WebEmulatorApplication

    @AppScope
    @Provides
    fun frontendComponent(config: Config): FrontendComponent = FrontendComponent::class.create(config)

    @AppScope
    @Provides
    fun nesMachine(component: FrontendComponent): NesMachine = component.nesMachine

    @AppScope
    @Provides
    fun keyboardInput(component: FrontendComponent): PlatformKeyboardInput = component.keyboardInput

    @AppScope
    @Provides
    fun runtimeInput(component: FrontendComponent): DelegatingEmulatorInput = component.runtimeInput

    @AppScope
    @Provides
    fun runtimeHost(component: FrontendComponent): EmulatorRuntimeHost = component.runtimeHost

    @AppScope
    @Provides
    fun renderer(component: FrontendComponent): PlatformRenderer = component.renderer

    @AppScope
    @Provides
    fun viewModel(component: FrontendComponent): MainScreenViewModel = component.viewModel
}