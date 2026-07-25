package frontend

import nes.input.NesController
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.glfw.GLFWGamepadState
import org.lwjgl.system.MemoryStack
import org.slf4j.LoggerFactory

class ControllerInput(
    private val controller: NesController,
) : BaseEmulatorInput() {
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
    private var stateAvailable = false
    private var quitPressed = false

    private val log = LoggerFactory.getLogger("ControllerInput")

    init {
        if (!mappedGamepad) {
            throw IllegalStateException(
                "--controller was requested, but GLFW did not detect a mapped gamepad for '${glfwGetJoystickName(joystick)}'",
            )
        }
        log.info("Using mapped gamepad: {}", glfwGetGamepadName(joystick))
    }

    override fun poll() {
        if (!glfwJoystickPresent(joystick)) {
            stateAvailable = false
            controller.setButtons(0)
            quitPressed = false
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
        controller.setButtons(buttons)
        updateControlEdges(pause, reset)
    }

    override fun quitRequested() = stateAvailable && quitPressed

    override fun close() {
        state.free()
    }

    private fun Int.isPressed(): Boolean = state.buttons(this).toInt() == GLFW_PRESS

    private companion object {
        const val MACOS_XBOX_360_MAPPING =
            "030000005e0400008e02000014010000,Xbox 360 Controller," +
                "a:b0,b:b1,back:b9,dpdown:b12,dpleft:b13,dpright:b14,dpup:b11," +
                "guide:b10,leftshoulder:b4,leftstick:b6,lefttrigger:a2,leftx:a0,lefty:a1~," +
                "rightshoulder:b5,rightstick:b7,righttrigger:a5,rightx:a3,righty:a4~," +
                "start:b8,x:b2,y:b3,platform:Mac OS X,"
    }
}
