package frontend

import co.touchlab.kermit.Logger
import nes.input.NesController
import net.java.games.input.Component
import net.java.games.input.Controller
import net.java.games.input.ControllerEnvironment
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.logging.Level

actual class PlatformControllerInput actual constructor(
    private val controller: NesController,
) : EmulatorInput {
    private val log = Logger.withTag("PlatformControllerInput")
    private val gamepad = controllers()
        .onEach { log.d { "Input device: ${it.name} type=${it.type}" } }
        .firstOrNull { it.type == Controller.Type.GAMEPAD || it.type == Controller.Type.STICK }
    private val buttons = arrayOfNulls<Component>(16)
    private val pov = gamepad?.components?.firstOrNull { it.identifier == Component.Identifier.Axis.POV }

    init {
        if (gamepad == null) {
            log.i { "No gamepad detected" }
        } else {
            log.i { "Using gamepad: ${gamepad.name}" }
            gamepad.components.forEach { component ->
                log.d { "Gamepad component: ${component.name} id=${component.identifier.name}" }
                component.identifier.name.toIntOrNull()?.takeIf { it in buttons.indices }?.let { index ->
                    buttons[index] = component
                }
            }
        }
    }

    actual override fun poll() {
        val gamepad = gamepad ?: return
        if (!gamepad.poll()) return

        if (button(0)) controller.press(NesController.BUTTON_B)
        if (button(1)) controller.press(NesController.BUTTON_A)
        if (button(9)) controller.press(NesController.BUTTON_SELECT)
        if (button(8)) controller.press(NesController.BUTTON_START)

        val pov = pov?.pollData
        val up = button(11) || pov == Component.POV.UP || pov == Component.POV.UP_LEFT || pov == Component.POV.UP_RIGHT
        val down = button(12) || pov == Component.POV.DOWN || pov == Component.POV.DOWN_LEFT || pov == Component.POV.DOWN_RIGHT
        val left = button(13) || pov == Component.POV.LEFT || pov == Component.POV.UP_LEFT || pov == Component.POV.DOWN_LEFT
        val right = button(14) || pov == Component.POV.RIGHT || pov == Component.POV.UP_RIGHT || pov == Component.POV.DOWN_RIGHT
        if (up) controller.press(NesController.BUTTON_UP)
        if (down) controller.press(NesController.BUTTON_DOWN)
        if (left) controller.press(NesController.BUTTON_LEFT)
        if (right) controller.press(NesController.BUTTON_RIGHT)
    }

    override fun pause() = Unit

    override fun close() = Unit

    private fun button(index: Int): Boolean = buttons[index]?.pollData == 1f

    private companion object {
        fun controllers(): Array<Controller> {
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
            if (libraries.isNotEmpty()) {
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
            java.util.logging.Logger.getLogger("net.java.games.input.ControllerEnvironment").level = Level.WARNING
            return ControllerEnvironment.getDefaultEnvironment().controllers
        }
    }
}
