package frontend

import org.lwjgl.glfw.GLFW.*
import org.lwjgl.glfw.GLFWErrorCallback
import org.lwjgl.opengl.GL

class GlfwWindow(title: String, width: Int, height: Int) : AutoCloseable {
    val handle: Long
    var title: String = title
        get() = glfwGetWindowTitle(handle).orEmpty()
        set(value) {
            glfwSetWindowTitle(handle, value)
            field = value
        }

    init {
        GLFWErrorCallback.createPrint(System.err).set()
        if (!glfwInit()) {
            throw IllegalStateException("GLFW initialization failure")
        }
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 2)
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 1)
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE)
        handle = glfwCreateWindow(width, height, title, 0, 0)
        if (handle == 0L) {
            throw IllegalStateException("GLFW window creation failure")
        }
        glfwMakeContextCurrent(handle)
        glfwSwapInterval(0)
        GL.createCapabilities()
    }

    fun pollEvents() = glfwPollEvents()
    fun swapBuffers() = glfwSwapBuffers(handle)
    fun shouldClose() = glfwWindowShouldClose(handle)
    fun requestClose() = glfwSetWindowShouldClose(handle, true)
    fun width(): Int {
        val a = IntArray(1)
        val b = IntArray(1)
        glfwGetFramebufferSize(handle, a, b)
        return a[0]
    }

    fun height(): Int {
        val a = IntArray(1)
        val b = IntArray(1)
        glfwGetFramebufferSize(handle, a, b)
        return b[0]
    }

    override fun close() {
        glfwDestroyWindow(handle)
        glfwTerminate()
        glfwSetErrorCallback(null)?.free()
    }
}
