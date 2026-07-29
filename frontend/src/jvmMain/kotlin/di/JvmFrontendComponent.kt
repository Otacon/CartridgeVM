package di

import app.EmulatorApplication
import frontend.Config
import frontend.DelegatingEmulatorInput
import frontend.EmulatorRuntimeHost
import frontend.MainScreenViewModel
import frontend.PlatformAudioPipeline
import frontend.PlatformKeyboardInput
import frontend.PlatformRenderer
import me.tatarka.inject.annotations.Component
import me.tatarka.inject.annotations.Provides
import nes.NesMachine

@AppScope
@Component
abstract class JvmFrontendComponent(
    @get:Provides val config: Config,
) {
    abstract val emulatorApplication: EmulatorApplication

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
    fun audio(component: FrontendComponent): PlatformAudioPipeline = component.audio

    @AppScope
    @Provides
    fun renderer(component: FrontendComponent): PlatformRenderer = component.renderer

    @AppScope
    @Provides
    fun viewModel(component: FrontendComponent): MainScreenViewModel = component.viewModel
}