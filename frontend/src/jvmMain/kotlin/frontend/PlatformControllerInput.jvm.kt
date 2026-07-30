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
) : BaseEmulatorInput() {
    private val log = Logger.withTag("PlatformControllerInput")
    private val gamepad = controllers()
        .onEach { log.d { "Input device: ${it.name} type=${it.type}" } }
        .firstOrNull { it.type == Controller.Type.GAMEPAD || it.type == Controller.Type.STICK }
        ?: throw IllegalStateException("--controller was requested, but no gamepad was detected")
    private val components = gamepad.components.associateBy { it.identifier.name }
    private var stateAvailable = false
    private var quitPressed = false

    init {
        log.i { "Using gamepad: ${gamepad.name}" }
        gamepad.components.forEach { component ->
            log.d { "Gamepad component: ${component.name} id=${component.identifier.name}" }
        }
    }

    actual override fun poll() {
        stateAvailable = gamepad.poll()
        if (!stateAvailable) {
            controller.setButtons(0)
            quitPressed = false
            return
        }

        var buttons = 0
        if (button(0)) buttons = buttons or (1 shl NesController.B)
        if (button(1)) buttons = buttons or (1 shl NesController.A)
        if (button(9)) buttons = buttons or (1 shl NesController.SELECT)
        if (button(8)) buttons = buttons or (1 shl NesController.START)

        val pov = components[Component.Identifier.Axis.POV.name]?.pollData
        val up = button(11) || pov == Component.POV.UP || pov == Component.POV.UP_LEFT || pov == Component.POV.UP_RIGHT
        val down = button(12) || pov == Component.POV.DOWN || pov == Component.POV.DOWN_LEFT || pov == Component.POV.DOWN_RIGHT
        val left = button(13) || pov == Component.POV.LEFT || pov == Component.POV.UP_LEFT || pov == Component.POV.DOWN_LEFT
        val right = button(14) || pov == Component.POV.RIGHT || pov == Component.POV.UP_RIGHT || pov == Component.POV.DOWN_RIGHT
        if (up) buttons = buttons or (1 shl NesController.UP)
        if (down) buttons = buttons or (1 shl NesController.DOWN)
        if (left) buttons = buttons or (1 shl NesController.LEFT)
        if (right) buttons = buttons or (1 shl NesController.RIGHT)

        controller.setButtons(buttons)
        updateControlEdges(button(5))
        quitPressed = button(10)
    }

    actual override fun quitRequested() = stateAvailable && quitPressed

    override fun pause() {
        stateAvailable = false
        quitPressed = false
        controller.setButtons(0)
    }

    override fun close() {
        controller.setButtons(0)
    }

    private fun button(index: Int): Boolean = components[index.toString()]?.pollData == 1f

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
                val directory = Files.createTempDirectory("cartridgevm-jinput-")
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
