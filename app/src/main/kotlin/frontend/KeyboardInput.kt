package frontend

import nes.input.NesController
import org.lwjgl.glfw.GLFW.*

class KeyboardInput(
    private val window: Long,
    private val controller: NesController,
) : BaseEmulatorInput() {

    override fun poll() {
        var buttons = 0
        if (GLFW_KEY_Z.isKeyPressed()) buttons = buttons or (1 shl NesController.A)
        if (GLFW_KEY_X.isKeyPressed()) buttons = buttons or (1 shl NesController.B)
        if (GLFW_KEY_RIGHT_SHIFT.isKeyPressed()) buttons = buttons or (1 shl NesController.SELECT)
        if (GLFW_KEY_ENTER.isKeyPressed() || GLFW_KEY_KP_ENTER.isKeyPressed()) {
            buttons = buttons or (1 shl NesController.START)
        }
        if (GLFW_KEY_UP.isKeyPressed()) buttons = buttons or (1 shl NesController.UP)
        if (GLFW_KEY_DOWN.isKeyPressed()) buttons = buttons or (1 shl NesController.DOWN)
        if (GLFW_KEY_LEFT.isKeyPressed()) buttons = buttons or (1 shl NesController.LEFT)
        if (GLFW_KEY_RIGHT.isKeyPressed()) buttons = buttons or (1 shl NesController.RIGHT)
        controller.setButtons(buttons)
        updateControlEdges(GLFW_KEY_P.isKeyPressed(), GLFW_KEY_R.isKeyPressed())
    }

    override fun quitRequested() = GLFW_KEY_ESCAPE.isKeyPressed()

    private fun Int.isKeyPressed(): Boolean = glfwGetKey(window, this) == GLFW_PRESS
}
