package frontend

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
) : EmulatorInput {

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
        val gamepad = gamepad ?: return
        if (!gamepad.poll()) return
        with(gamepad) {
            if (getComponent(Button._1).isPressed) controller.press(NesController.BUTTON_A)
            if (getComponent(Button._0).isPressed) controller.press(NesController.BUTTON_B)
            if (getComponent(Button._9).isPressed) controller.press(NesController.BUTTON_SELECT)
            if (getComponent(Button._8).isPressed) controller.press(NesController.BUTTON_START)

            val y = getComponent(Axis.Y).poll
            if (getComponent(Button._11).isPressed || y < -0.5f) controller.press(NesController.BUTTON_UP)
            if (getComponent(Button._12).isPressed || y > 0.5f) controller.press(NesController.BUTTON_DOWN)

            val x = getComponent(Axis.X).poll
            if (getComponent(Button._13).isPressed || x < -0.5f) controller.press(NesController.BUTTON_LEFT)
            if (getComponent(Button._14).isPressed || x > 0.5f) controller.press(NesController.BUTTON_RIGHT)
        }

    }

    override fun pause() = Unit

    override fun close() = Unit

    private val Component?.poll
        get() = this?.pollData ?: 0.0f

    private val Component?.isPressed
        get() = this.poll == 1f

}
