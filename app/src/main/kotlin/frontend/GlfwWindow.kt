package frontend

import org.lwjgl.glfw.GLFW.*
import org.lwjgl.glfw.GLFWErrorCallback
import org.lwjgl.opengl.GL

class GlfwWindow : AutoCloseable {

    private var handle: Long = 0

    var title: String
        get() = glfwGetWindowTitle(handle).orEmpty()
        set(value) = glfwSetWindowTitle(handle, value)

    val width: Int
        get() {
            val a = IntArray(1)
            val b = IntArray(1)
            glfwGetFramebufferSize(handle, a, b)
            return a[0]
        }

    val height: Int
        get() {
            val a = IntArray(1)
            val b = IntArray(1)
            glfwGetFramebufferSize(handle, a, b)
            return b[0]
        }

    fun create(width: Int, height: Int): Long {
        GLFWErrorCallback.createPrint(System.err).set()
        if (!glfwInit()) {
            throw IllegalStateException("GLFW initialization failure")
        }
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 2)
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 1)
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE)
        handle = glfwCreateWindow(width, height, "", 0, 0)
        if (handle == 0L) {
            throw IllegalStateException("GLFW window creation failure")
        }
        glfwMakeContextCurrent(handle)
        glfwSwapInterval(0)
        GL.createCapabilities()
        return handle
    }

    fun pollEvents() {
        glfwPollEvents()
    }

    fun swapBuffers() {
        glfwSwapBuffers(handle)
    }

    fun shouldClose(): Boolean {
        return glfwWindowShouldClose(handle)
    }

    fun requestClose() {
        glfwSetWindowShouldClose(handle, true)
    }

    override fun close() {
        glfwDestroyWindow(handle)
        glfwTerminate()
        glfwSetErrorCallback(null)?.free()
    }
}
