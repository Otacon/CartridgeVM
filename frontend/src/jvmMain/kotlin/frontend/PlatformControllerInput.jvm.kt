package frontend

import frontend.controllerSettings.ControllerInputMapper
import frontend.controllerSettings.InputBinding
import frontend.controllerSettings.InputDevice
import frontend.controllerSettings.gamepadAxisBinding
import frontend.controllerSettings.gamepadButtonBinding
import frontend.controllerSettings.gamepadPovBinding
import nes.input.NesController
import net.java.games.input.Component
import net.java.games.input.Component.Identifier.Axis
import net.java.games.input.Component.Identifier.Button
import net.java.games.input.Controller
import net.java.games.input.ControllerEnvironment
import java.nio.file.Files
import java.nio.file.StandardCopyOption

actual class PlatformControllerInput actual constructor(
    private val controller: NesController,
    private val inputMapper: ControllerInputMapper,
) : EmulatorInput {

    private var ignoredBindings = emptySet<String>()

    private val gamepad: Controller?
        get() = ControllerEnvironment.getDefaultEnvironment().controllers.firstOrNull()

    actual override fun init() {
        val os = System.getProperty("os.name").lowercase()
        val libraries = when {
            os.contains("mac") -> listOf("libjinput-osx.jnilib" to System.mapLibraryName("jinput-osx"))
            os.contains("linux") -> listOf("libjinput-linux64.so" to System.mapLibraryName("jinput-linux64"))
            os.contains("win") -> listOf(
                "jinput-raw_64.dll" to System.mapLibraryName("jinput-raw_64"),
                "jinput-dx8_64.dll" to System.mapLibraryName("jinput-dx8_64"),
            )

            else -> emptyList()
        }
        if (libraries.isEmpty()) return
        val directory = Files.createTempDirectory("kassette-jinput-")
        directory.toFile().deleteOnExit()
        libraries.forEach { (resource, fileName) ->
            val target = directory.resolve(fileName)
            PlatformControllerInput::class.java.getResourceAsStream("/$resource").use { input ->
                requireNotNull(input) { "Missing JInput native library: $resource" }
                Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING)
            }
            target.toFile().deleteOnExit()
        }
        System.setProperty("net.java.games.input.librarypath", directory.toString())
    }

    actual override fun poll() {
        pressedBindings().forEach { binding ->
            inputMapper.map(InputDevice.Gamepad, binding.code)?.let(controller::press)
        }

    }

    actual fun pressedBindings(): List<InputBinding> {
        val current = currentPressedBindings()
        if (current.none { it.code in ignoredBindings }) ignoredBindings = emptySet()
        return current.filterNot { it.code in ignoredBindings }
    }

    private fun currentPressedBindings(): List<InputBinding> {
        val gamepad = gamepad ?: return emptyList()
        if (!gamepad.poll()) return emptyList()
        return buildList {
            with(gamepad) {
                if (getComponent(Button._0).isPressed) add(gamepadButtonBinding(0))
                if (getComponent(Button._1).isPressed) add(gamepadButtonBinding(1))
                if (getComponent(Button._2).isPressed) add(gamepadButtonBinding(2))
                if (getComponent(Button._3).isPressed) add(gamepadButtonBinding(3))
                if (getComponent(Button._4).isPressed) add(gamepadButtonBinding(4))
                if (getComponent(Button._5).isPressed) add(gamepadButtonBinding(5))
                if (getComponent(Button._6).isPressed) add(gamepadButtonBinding(6))
                if (getComponent(Button._7).isPressed) add(gamepadButtonBinding(7))
                if (getComponent(Button._8).isPressed) add(gamepadButtonBinding(8))
                if (getComponent(Button._9).isPressed) add(gamepadButtonBinding(9))
                if (getComponent(Button._10).isPressed) add(gamepadButtonBinding(10))
                if (getComponent(Button._11).isPressed) add(gamepadButtonBinding(11))
                if (getComponent(Button._12).isPressed) add(gamepadButtonBinding(12))
                if (getComponent(Button._13).isPressed) add(gamepadButtonBinding(13))
                if (getComponent(Button._14).isPressed) add(gamepadButtonBinding(14))
                if (getComponent(Button._15).isPressed) add(gamepadButtonBinding(15))

                components.forEach { component ->
                    val buttonIndex = component.identifier.name.toIntOrNull()
                    if (buttonIndex != null && component.isPressed) add(gamepadButtonBinding(buttonIndex))
                }

                addPovBindings(getComponent(Axis.POV).poll)

                val x = getComponent(Axis.X).poll
                if (x < -0.5f) add(gamepadAxisBinding(0, negative = true))
                if (x > 0.5f) add(gamepadAxisBinding(0, negative = false))

                val y = getComponent(Axis.Y).poll
                if (y < -0.5f) add(gamepadAxisBinding(1, negative = true))
                if (y > 0.5f) add(gamepadAxisBinding(1, negative = false))
            }
        }.distinctBy { it.code }
    }

    private fun MutableList<InputBinding>.addPovBindings(value: Float) {
        when (value) {
            Component.POV.UP -> add(gamepadPovBinding("up"))
            Component.POV.DOWN -> add(gamepadPovBinding("down"))
            Component.POV.LEFT -> add(gamepadPovBinding("left"))
            Component.POV.RIGHT -> add(gamepadPovBinding("right"))
            Component.POV.UP_LEFT -> {
                        add(gamepadPovBinding("up"))
                        add(gamepadPovBinding("left"))
            }
            Component.POV.UP_RIGHT -> {
                add(gamepadPovBinding("up"))
                add(gamepadPovBinding("right"))
            }
            Component.POV.DOWN_LEFT -> {
                add(gamepadPovBinding("down"))
                add(gamepadPovBinding("left"))
            }
            Component.POV.DOWN_RIGHT -> {
                add(gamepadPovBinding("down"))
                add(gamepadPovBinding("right"))
            }
        }
    }

    actual fun clearPressedBindings() {
        ignoredBindings = emptySet()
        ignoredBindings = currentPressedBindings().mapTo(mutableSetOf()) { it.code }
    }

    override fun pause() = Unit

    override fun close() = Unit

    private val Component?.poll
        get() = this?.pollData ?: 0.0f

    private val Component?.isPressed
        get() = this.poll == 1f

}
