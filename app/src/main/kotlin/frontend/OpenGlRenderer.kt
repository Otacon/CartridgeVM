package frontend

import di.AppScope
import me.tatarka.inject.annotations.Inject
import nes.util.low8Bits
import org.eclipse.swt.opengl.GLCanvas
import java.nio.ByteBuffer
import java.nio.ByteOrder

@Inject
@AppScope
class OpenGlRenderer : AutoCloseable {
    private val gl = NativeOpenGl.load()
    private var texture = 0
    private var crtProgram = 0
    private var crtEnabled = false
    private var outputSizeUniform = -1
    private var timeUniform = -1
    private var presentedFrames = 0L
    private val upload: ByteBuffer = ByteBuffer.allocateDirect(256 * 240 * 4).order(ByteOrder.nativeOrder())

    fun init(canvas: GLCanvas, crt: Boolean) {
        canvas.setCurrent()
        crtEnabled = crt
        val textures = IntArray(1)
        gl.glGenTextures(1, textures)
        texture = textures[0]
        if (texture == 0) throw IllegalStateException("OpenGL initialization failure")
        gl.glBindTexture(GL_TEXTURE_2D, texture)
        val filtering = if (crt) GL_LINEAR else GL_NEAREST
        gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, filtering)
        gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, filtering)
        gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE)
        gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE)
        gl.glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, 256, 240, 0, GL_RGBA, GL_UNSIGNED_BYTE, upload)
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
        gl.glBindTexture(GL_TEXTURE_2D, texture)
        gl.glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, 256, 240, GL_RGBA, GL_UNSIGNED_BYTE, upload)
        gl.glViewport(0, 0, windowWidth, windowHeight)
        gl.glClearColor(0f, 0f, 0f, 1f)
        gl.glClear(GL_COLOR_BUFFER_BIT)
        val (w, h) = if (crtEnabled) {
            fitAspect(windowWidth, windowHeight, 256f / 240f, 1f)
        } else {
            val scale = maxOf(1, minOf(windowWidth / 256, windowHeight / 240))
            256f * scale / windowWidth.toFloat() to 240f * scale / windowHeight.toFloat()
        }
        if (crtEnabled) {
            gl.glUseProgram(crtProgram)
            gl.glUniform2f(outputSizeUniform, windowWidth.toFloat(), windowHeight.toFloat())
            gl.glUniform1f(timeUniform, presentedFrames / 60f)
        }
        gl.glEnable(GL_TEXTURE_2D)
        gl.glBegin(GL_QUADS)
        gl.glTexCoord2f(0f, 1f); gl.glVertex2f(-w, -h)
        gl.glTexCoord2f(1f, 1f); gl.glVertex2f(w, -h)
        gl.glTexCoord2f(1f, 0f); gl.glVertex2f(w, h)
        gl.glTexCoord2f(0f, 0f); gl.glVertex2f(-w, h)
        gl.glEnd()
        if (crtEnabled) gl.glUseProgram(0)
        presentedFrames++
    }

    override fun close() {
        if (crtProgram != 0) gl.glDeleteProgram(crtProgram)
        if (texture != 0) gl.glDeleteTextures(1, intArrayOf(texture))
        crtProgram = 0
        texture = 0
    }

    private fun initCrtProgram() {
        val vertexShader = compileShader(GL_VERTEX_SHADER, "/shaders/crt.vert")
        val fragmentShader = compileShader(GL_FRAGMENT_SHADER, "/shaders/crt.frag")
        crtProgram = gl.glCreateProgram()
        gl.glAttachShader(crtProgram, vertexShader)
        gl.glAttachShader(crtProgram, fragmentShader)
        gl.glLinkProgram(crtProgram)
        gl.glDeleteShader(vertexShader)
        gl.glDeleteShader(fragmentShader)
        val status = IntArray(1)
        gl.glGetProgramiv(crtProgram, GL_LINK_STATUS, status)
        if (status[0] == GL_FALSE) {
            throw IllegalStateException("CRT shader link failure: ${programInfoLog(crtProgram)}")
        }
        gl.glUseProgram(crtProgram)
        gl.glUniform1i(gl.glGetUniformLocation(crtProgram, "frameTexture"), 0)
        outputSizeUniform = gl.glGetUniformLocation(crtProgram, "outputSize")
        timeUniform = gl.glGetUniformLocation(crtProgram, "time")
        gl.glUseProgram(0)
    }

    private fun compileShader(type: Int, resource: String): Int {
        val source = checkNotNull(javaClass.getResource(resource)) { "Missing shader resource: $resource" }.readText()
        val shader = gl.glCreateShader(type)
        gl.glShaderSource(shader, 1, arrayOf(source), intArrayOf(source.length))
        gl.glCompileShader(shader)
        val status = IntArray(1)
        gl.glGetShaderiv(shader, GL_COMPILE_STATUS, status)
        if (status[0] == GL_FALSE) {
            val error = shaderInfoLog(shader)
            gl.glDeleteShader(shader)
            throw IllegalStateException("CRT shader compile failure ($resource): $error")
        }
        return shader
    }

    private fun shaderInfoLog(shader: Int): String {
        val length = IntArray(1)
        gl.glGetShaderiv(shader, GL_INFO_LOG_LENGTH, length)
        val log = ByteArray(maxOf(1, length[0]))
        gl.glGetShaderInfoLog(shader, log.size, null, log)
        return log.decodeToString().trimEnd('\u0000')
    }

    private fun programInfoLog(program: Int): String {
        val length = IntArray(1)
        gl.glGetProgramiv(program, GL_INFO_LOG_LENGTH, length)
        val log = ByteArray(maxOf(1, length[0]))
        gl.glGetProgramInfoLog(program, log.size, null, log)
        return log.decodeToString().trimEnd('\u0000')
    }

    private companion object {
        const val GL_FALSE = 0
        const val GL_TEXTURE_2D = 0x0DE1
        const val GL_TEXTURE_MIN_FILTER = 0x2801
        const val GL_TEXTURE_MAG_FILTER = 0x2800
        const val GL_TEXTURE_WRAP_S = 0x2802
        const val GL_TEXTURE_WRAP_T = 0x2803
        const val GL_NEAREST = 0x2600
        const val GL_LINEAR = 0x2601
        const val GL_CLAMP_TO_EDGE = 0x812F
        const val GL_RGBA = 0x1908
        const val GL_RGBA8 = 0x8058
        const val GL_UNSIGNED_BYTE = 0x1401
        const val GL_COLOR_BUFFER_BIT = 0x4000
        const val GL_QUADS = 0x0007
        const val GL_VERTEX_SHADER = 0x8B31
        const val GL_FRAGMENT_SHADER = 0x8B30
        const val GL_COMPILE_STATUS = 0x8B81
        const val GL_LINK_STATUS = 0x8B82
        const val GL_INFO_LOG_LENGTH = 0x8B84
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
