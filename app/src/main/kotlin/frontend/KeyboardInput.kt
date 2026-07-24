package frontend

import nes.input.NesController
import org.lwjgl.glfw.GLFW.*
import org.slf4j.LoggerFactory

class KeyboardInput(
    private val window: Long,
    private val controller: NesController,
) {
    private var prevPause = false
    private var prevReset = false
    private var pauseEdge = false
    private var resetEdge = false
    private var previousButtons = 0

    private val log = LoggerFactory.getLogger("KeyboardInput")

    fun poll() {
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
        logPressedEdges()
        val p = GLFW_KEY_P.isKeyPressed()
        val r = GLFW_KEY_R.isKeyPressed()
        pauseEdge = pauseEdge || (p && !prevPause)
        resetEdge = resetEdge || (r && !prevReset)
        prevPause = p
        prevReset = r
    }

    fun consumePause(): Boolean {
        val v = pauseEdge
        pauseEdge = false
        return v
    }

    fun consumeReset(): Boolean {
        val v = resetEdge
        resetEdge = false
        return v
    }

    fun quitRequested() = GLFW_KEY_ESCAPE.isKeyPressed()

    private fun Int.isKeyPressed(): Boolean = glfwGetKey(window, this) == GLFW_PRESS

    private fun logPressedEdges() {
        val buttons = controller.snapshot()
        val pressed = buttons and previousButtons.inv()
        if ((pressed and (1 shl NesController.START)) != 0) log.debug("START pressed")
        if ((pressed and (1 shl NesController.A)) != 0) log.debug("A pressed")
        if ((pressed and (1 shl NesController.B)) != 0) log.debug("B pressed")
        if ((pressed and (1 shl NesController.SELECT)) != 0) log.debug("SELECT pressed")
        if ((pressed and (1 shl NesController.UP)) != 0) log.debug("UP pressed")
        if ((pressed and (1 shl NesController.DOWN)) != 0) log.debug("DOWN pressed")
        if ((pressed and (1 shl NesController.LEFT)) != 0) log.debug("LEFT pressed")
        if ((pressed and (1 shl NesController.RIGHT)) != 0) log.debug("RIGHT pressed")
        previousButtons = buttons
    }
}
