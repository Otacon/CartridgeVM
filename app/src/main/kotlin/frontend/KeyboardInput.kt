package frontend

import nes.input.NesController
import org.lwjgl.glfw.GLFW.*

class KeyboardInput(
    private val window: Long,
    private val controller: NesController,
    private val debug: Boolean = false,
) {
    private var prevPause = false
    private var prevReset = false
    private var pauseEdge = false
    private var resetEdge = false
    private var previousButtons = 0

    fun poll() {
        controller.setButton(NesController.A, down(GLFW_KEY_Z))
        controller.setButton(NesController.B, down(GLFW_KEY_X))
        controller.setButton(NesController.SELECT, down(GLFW_KEY_RIGHT_SHIFT))
        controller.setButton(NesController.START, down(GLFW_KEY_ENTER) || down(GLFW_KEY_KP_ENTER))
        controller.setButton(NesController.UP, down(GLFW_KEY_UP))
        controller.setButton(NesController.DOWN, down(GLFW_KEY_DOWN))
        controller.setButton(NesController.LEFT, down(GLFW_KEY_LEFT))
        controller.setButton(NesController.RIGHT, down(GLFW_KEY_RIGHT))
        if (debug) logPressedEdges()
        val p = down(GLFW_KEY_P)
        val r = down(GLFW_KEY_R)
        pauseEdge = p && !prevPause
        resetEdge = r && !prevReset
        prevPause = p; prevReset = r
    }

    fun consumePause(): Boolean { val v = pauseEdge; pauseEdge = false; return v }
    fun consumeReset(): Boolean { val v = resetEdge; resetEdge = false; return v }
    fun quitRequested() = down(GLFW_KEY_ESCAPE)
    private fun down(key: Int) = glfwGetKey(window, key) == GLFW_PRESS

    private fun logPressedEdges() {
        val buttons = controller.snapshot()
        val pressed = buttons and previousButtons.inv()
        if ((pressed and (1 shl NesController.START)) != 0) println("Input: START pressed")
        if ((pressed and (1 shl NesController.A)) != 0) println("Input: A pressed")
        if ((pressed and (1 shl NesController.B)) != 0) println("Input: B pressed")
        if ((pressed and (1 shl NesController.SELECT)) != 0) println("Input: SELECT pressed")
        if ((pressed and (1 shl NesController.UP)) != 0) println("Input: UP pressed")
        if ((pressed and (1 shl NesController.DOWN)) != 0) println("Input: DOWN pressed")
        if ((pressed and (1 shl NesController.LEFT)) != 0) println("Input: LEFT pressed")
        if ((pressed and (1 shl NesController.RIGHT)) != 0) println("Input: RIGHT pressed")
        previousButtons = buttons
    }
}
