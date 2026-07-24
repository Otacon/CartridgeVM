package frontend

import di.AppScope
import me.tatarka.inject.annotations.Inject
import nes.util.low8Bits
import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL11.*
import org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE
import org.lwjgl.opengl.GL20.*
import java.nio.ByteBuffer

@Inject
@AppScope
class OpenGlRenderer : AutoCloseable {
    private var texture = 0
    private var crtProgram = 0
    private var crtEnabled = false
    private var outputSizeUniform = -1
    private var timeUniform = -1
    private var presentedFrames = 0L
    private val upload: ByteBuffer = BufferUtils.createByteBuffer(256 * 240 * 4)

    fun init(crt: Boolean) {
        crtEnabled = crt
        texture = glGenTextures()
        if (texture == 0) throw IllegalStateException("OpenGL initialization failure")
        glBindTexture(GL_TEXTURE_2D, texture)
        val filtering = if (crt) GL_LINEAR else GL_NEAREST
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, filtering)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, filtering)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE)
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, 256, 240, 0, GL_RGBA, GL_UNSIGNED_BYTE, upload)
        if (crt) initCrtProgram()
    }

    fun present(framebuffer: IntArray, windowWidth: Int, windowHeight: Int) {
        upload.clear()
        var i = 0
        while (i < framebuffer.size) {
            val c = framebuffer[i]
            upload.put((c shr 16).low8Bits().toByte())
            upload.put((c shr 8).low8Bits().toByte())
            upload.put(c.low8Bits().toByte())
            upload.put(0xFF.toByte())
            i++
        }
        upload.flip()
        glBindTexture(GL_TEXTURE_2D, texture)
        glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, 256, 240, GL_RGBA, GL_UNSIGNED_BYTE, upload)
        glViewport(0, 0, windowWidth, windowHeight)
        glClearColor(0f, 0f, 0f, 1f)
        glClear(GL_COLOR_BUFFER_BIT)
        val (w, h) = if (crtEnabled) {
            fitAspect(windowWidth, windowHeight, 4f / 3f, 1f)
        } else {
            val scale = maxOf(1, minOf(windowWidth / 256, windowHeight / 240))
            256f * scale / windowWidth.toFloat() to 240f * scale / windowHeight.toFloat()
        }
        if (crtEnabled) {
            glUseProgram(crtProgram)
            glUniform2f(outputSizeUniform, windowWidth.toFloat(), windowHeight.toFloat())
            glUniform1f(timeUniform, presentedFrames / 60f)
        }
        glEnable(GL_TEXTURE_2D)
        glBegin(GL_QUADS)
        glTexCoord2f(0f, 1f); glVertex2f(-w, -h)
        glTexCoord2f(1f, 1f); glVertex2f(w, -h)
        glTexCoord2f(1f, 0f); glVertex2f(w, h)
        glTexCoord2f(0f, 0f); glVertex2f(-w, h)
        glEnd()
        if (crtEnabled) glUseProgram(0)
        presentedFrames++
    }

    override fun close() {
        if (crtProgram != 0) glDeleteProgram(crtProgram)
        if (texture != 0) glDeleteTextures(texture)
        crtProgram = 0
        texture = 0
    }

    private fun initCrtProgram() {
        val vertexShader = compileShader(GL_VERTEX_SHADER, "/shaders/crt.vert")
        val fragmentShader = compileShader(GL_FRAGMENT_SHADER, "/shaders/crt.frag")
        crtProgram = glCreateProgram()
        glAttachShader(crtProgram, vertexShader)
        glAttachShader(crtProgram, fragmentShader)
        glLinkProgram(crtProgram)
        glDeleteShader(vertexShader)
        glDeleteShader(fragmentShader)
        if (glGetProgrami(crtProgram, GL_LINK_STATUS) == GL_FALSE) {
            throw IllegalStateException("CRT shader link failure: ${glGetProgramInfoLog(crtProgram)}")
        }
        glUseProgram(crtProgram)
        glUniform1i(glGetUniformLocation(crtProgram, "frameTexture"), 0)
        outputSizeUniform = glGetUniformLocation(crtProgram, "outputSize")
        timeUniform = glGetUniformLocation(crtProgram, "time")
        glUseProgram(0)
    }

    private fun compileShader(type: Int, resource: String): Int {
        val source = checkNotNull(javaClass.getResource(resource)) { "Missing shader resource: $resource" }.readText()
        val shader = glCreateShader(type)
        glShaderSource(shader, source)
        glCompileShader(shader)
        if (glGetShaderi(shader, GL_COMPILE_STATUS) == GL_FALSE) {
            val error = glGetShaderInfoLog(shader)
            glDeleteShader(shader)
            throw IllegalStateException("CRT shader compile failure ($resource): $error")
        }
        return shader
    }

    private fun fitAspect(windowWidth: Int, windowHeight: Int, aspect: Float, fill: Float): Pair<Float, Float> {
        val windowAspect = windowWidth.toFloat() / windowHeight
        return if (windowAspect > aspect) {
            fill * aspect / windowAspect to fill
        } else {
            fill to fill * windowAspect / aspect
        }
    }
}
