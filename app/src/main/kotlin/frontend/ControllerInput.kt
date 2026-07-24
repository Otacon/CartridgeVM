package frontend

import nes.input.NesController
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.glfw.GLFWGamepadState
import org.lwjgl.system.MemoryStack
import org.slf4j.LoggerFactory

class ControllerInput(
    private val controller: NesController,
) : EmulatorInput {
    private val joystick = (GLFW_JOYSTICK_1..GLFW_JOYSTICK_LAST).firstOrNull {
        glfwJoystickPresent(it)
    } ?: throw IllegalStateException(
        "--controller was requested, but GLFW did not detect a connected controller",
    )
    private val mappedGamepad = MemoryStack.stackPush().use { stack ->
        glfwUpdateGamepadMappings(stack.UTF8(MACOS_XBOX_360_MAPPING))
        glfwJoystickIsGamepad(joystick)
    }
    private val state = GLFWGamepadState.calloc()
    private var prevPause = false
    private var prevReset = false
    private var pauseEdge = false
    private var resetEdge = false
    private var previousButtons = 0
    private var stateAvailable = false
    private var quitPressed = false

    private val log = LoggerFactory.getLogger("ControllerInput")

    init {
        if (mappedGamepad) {
            log.info("Using mapped gamepad: {}", glfwGetGamepadName(joystick))
        } else {
            log.warn(
                "Using raw Xbox-compatible controller mapping for '{}' (GUID: {})",
                glfwGetJoystickName(joystick),
                glfwGetJoystickGUID(joystick),
            )
        }
    }

    override fun poll() {
        if (!glfwJoystickPresent(joystick)) {
            stateAvailable = false
            controller.setButtons(0)
            quitPressed = false
            return
        }
        if (!mappedGamepad) {
            pollRawJoystick()
            return
        }

        stateAvailable = glfwGetGamepadState(joystick, state)
        if (!stateAvailable) {
            controller.setButtons(0)
            quitPressed = false
            return
        }

        var buttons = 0
        if (GLFW_GAMEPAD_BUTTON_A.isPressed()) buttons = buttons or (1 shl NesController.B)
        if (GLFW_GAMEPAD_BUTTON_B.isPressed()) buttons = buttons or (1 shl NesController.A)
        if (GLFW_GAMEPAD_BUTTON_BACK.isPressed()) buttons = buttons or (1 shl NesController.SELECT)
        if (GLFW_GAMEPAD_BUTTON_START.isPressed()) buttons = buttons or (1 shl NesController.START)
        if (GLFW_GAMEPAD_BUTTON_DPAD_UP.isPressed()) {
            buttons = buttons or (1 shl NesController.UP)
        }
        if (GLFW_GAMEPAD_BUTTON_DPAD_DOWN.isPressed()) {
            buttons = buttons or (1 shl NesController.DOWN)
        }
        if (GLFW_GAMEPAD_BUTTON_DPAD_LEFT.isPressed()) {
            buttons = buttons or (1 shl NesController.LEFT)
        }
        if (GLFW_GAMEPAD_BUTTON_DPAD_RIGHT.isPressed()) {
            buttons = buttons or (1 shl NesController.RIGHT)
        }
        val pause = GLFW_GAMEPAD_BUTTON_RIGHT_BUMPER.isPressed()
        val reset = GLFW_GAMEPAD_BUTTON_LEFT_BUMPER.isPressed()
        quitPressed = GLFW_GAMEPAD_BUTTON_GUIDE.isPressed()
        updateController(buttons, pause, reset)
    }

    private fun pollRawJoystick() {
        val buttonsState = glfwGetJoystickButtons(joystick)
        val hats = glfwGetJoystickHats(joystick)
        if (buttonsState == null) {
            stateAvailable = false
            controller.setButtons(0)
            quitPressed = false
            return
        }

        stateAvailable = true
        fun button(index: Int) = index < buttonsState.remaining() && buttonsState[index].toInt() == GLFW_PRESS
        fun hat(direction: Int) = hats != null && hats.hasRemaining() && hats[0].toInt() and direction != 0

        var buttons = 0
        if (button(RAW_BUTTON_A)) buttons = buttons or (1 shl NesController.A)
        if (button(RAW_BUTTON_B)) buttons = buttons or (1 shl NesController.B)
        if (button(RAW_BUTTON_BACK)) buttons = buttons or (1 shl NesController.SELECT)
        if (button(RAW_BUTTON_START)) buttons = buttons or (1 shl NesController.START)
        if (hat(GLFW_HAT_UP)) {
            buttons = buttons or (1 shl NesController.UP)
        }
        if (hat(GLFW_HAT_DOWN)) {
            buttons = buttons or (1 shl NesController.DOWN)
        }
        if (hat(GLFW_HAT_LEFT)) {
            buttons = buttons or (1 shl NesController.LEFT)
        }
        if (hat(GLFW_HAT_RIGHT)) {
            buttons = buttons or (1 shl NesController.RIGHT)
        }
        quitPressed = button(RAW_BUTTON_GUIDE)
        updateController(buttons, button(RAW_BUTTON_RIGHT_BUMPER), button(RAW_BUTTON_LEFT_BUMPER))
    }

    private fun updateController(buttons: Int, pause: Boolean, reset: Boolean) {
        controller.setButtons(buttons)
        logPressedEdges()
        pauseEdge = pauseEdge || (pause && !prevPause)
        resetEdge = resetEdge || (reset && !prevReset)
        prevPause = pause
        prevReset = reset
    }

    override fun consumePause(): Boolean {
        val value = pauseEdge
        pauseEdge = false
        return value
    }

    override fun consumeReset(): Boolean {
        val value = resetEdge
        resetEdge = false
        return value
    }

    override fun quitRequested() = stateAvailable && quitPressed

    override fun close() {
        state.free()
    }

    private fun Int.isPressed(): Boolean = state.buttons(this).toInt() == GLFW_PRESS

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

    private companion object {
        const val MACOS_XBOX_360_MAPPING =
            "030000005e0400008e02000014010000,Xbox 360 Controller," +
                "a:b0,b:b1,back:b9,dpdown:b12,dpleft:b13,dpright:b14,dpup:b11," +
                "guide:b10,leftshoulder:b4,leftstick:b6,lefttrigger:a2,leftx:a0,lefty:a1~," +
                "rightshoulder:b5,rightstick:b7,righttrigger:a5,rightx:a3,righty:a4~," +
                "start:b8,x:b2,y:b3,platform:Mac OS X,"
        const val RAW_BUTTON_A = 0
        const val RAW_BUTTON_B = 1
        const val RAW_BUTTON_LEFT_BUMPER = 6
        const val RAW_BUTTON_RIGHT_BUMPER = 7
        const val RAW_BUTTON_BACK = 10
        const val RAW_BUTTON_START = 11
        const val RAW_BUTTON_GUIDE = 12
    }
}
