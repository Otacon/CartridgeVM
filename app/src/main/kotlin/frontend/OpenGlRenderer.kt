package frontend

import di.AppScope
import me.tatarka.inject.annotations.Inject
import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL11.*
import java.nio.ByteBuffer

@Inject
@AppScope
class OpenGlRenderer : AutoCloseable {
    private var texture = 0
    private val upload: ByteBuffer = BufferUtils.createByteBuffer(256 * 240 * 4)

    fun init() {
        texture = glGenTextures()
        if (texture == 0) throw IllegalStateException("OpenGL initialization failure")
        glBindTexture(GL_TEXTURE_2D, texture)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST)
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, 256, 240, 0, GL_RGBA, GL_UNSIGNED_BYTE, upload)
    }

    fun present(framebuffer: IntArray, windowWidth: Int, windowHeight: Int) {
        upload.clear()
        var i = 0
        while (i < framebuffer.size) {
            val c = framebuffer[i]
            upload.put(((c shr 16) and 0xFF).toByte())
            upload.put(((c shr 8) and 0xFF).toByte())
            upload.put((c and 0xFF).toByte())
            upload.put(0xFF.toByte())
            i++
        }
        upload.flip()
        glBindTexture(GL_TEXTURE_2D, texture)
        glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, 256, 240, GL_RGBA, GL_UNSIGNED_BYTE, upload)
        glViewport(0, 0, windowWidth, windowHeight)
        glClearColor(0f, 0f, 0f, 1f)
        glClear(GL_COLOR_BUFFER_BIT)
        val scale = maxOf(1, minOf(windowWidth / 256, windowHeight / 240))
        val w = 256f * scale / windowWidth.toFloat()
        val h = 240f * scale / windowHeight.toFloat()
        glEnable(GL_TEXTURE_2D)
        glBegin(GL_QUADS)
        glTexCoord2f(0f, 1f); glVertex2f(-w, -h)
        glTexCoord2f(1f, 1f); glVertex2f(w, -h)
        glTexCoord2f(1f, 0f); glVertex2f(w, h)
        glTexCoord2f(0f, 0f); glVertex2f(-w, h)
        glEnd()
    }

    override fun close() { if (texture != 0) glDeleteTextures(texture) }
}
