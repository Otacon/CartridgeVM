package frontend

import com.sun.jna.Library
import com.sun.jna.Native
import java.nio.Buffer

interface NativeOpenGl : Library {
    fun glGenTextures(n: Int, textures: IntArray)
    fun glBindTexture(target: Int, texture: Int)
    fun glTexParameteri(target: Int, pname: Int, param: Int)
    fun glTexImage2D(target: Int, level: Int, internalFormat: Int, width: Int, height: Int, border: Int, format: Int, type: Int, pixels: Buffer?)
    fun glTexSubImage2D(target: Int, level: Int, xOffset: Int, yOffset: Int, width: Int, height: Int, format: Int, type: Int, pixels: Buffer)
    fun glViewport(x: Int, y: Int, width: Int, height: Int)
    fun glClearColor(red: Float, green: Float, blue: Float, alpha: Float)
    fun glClear(mask: Int)
    fun glEnable(cap: Int)
    fun glBegin(mode: Int)
    fun glTexCoord2f(s: Float, t: Float)
    fun glVertex2f(x: Float, y: Float)
    fun glEnd()
    fun glDeleteTextures(n: Int, textures: IntArray)
    fun glCreateShader(type: Int): Int
    fun glShaderSource(shader: Int, count: Int, string: Array<String>, length: IntArray)
    fun glCompileShader(shader: Int)
    fun glGetShaderiv(shader: Int, pname: Int, params: IntArray)
    fun glGetShaderInfoLog(shader: Int, maxLength: Int, length: IntArray?, infoLog: ByteArray)
    fun glDeleteShader(shader: Int)
    fun glCreateProgram(): Int
    fun glAttachShader(program: Int, shader: Int)
    fun glLinkProgram(program: Int)
    fun glGetProgramiv(program: Int, pname: Int, params: IntArray)
    fun glGetProgramInfoLog(program: Int, maxLength: Int, length: IntArray?, infoLog: ByteArray)
    fun glUseProgram(program: Int)
    fun glGetUniformLocation(program: Int, name: String): Int
    fun glUniform1i(location: Int, v0: Int)
    fun glUniform1f(location: Int, v0: Float)
    fun glUniform2f(location: Int, v0: Float, v1: Float)
    fun glDeleteProgram(program: Int)

    companion object {
        fun load(): NativeOpenGl {
            val os = System.getProperty("os.name").lowercase()
            val library = when {
                os.contains("mac") -> "/System/Library/Frameworks/OpenGL.framework/OpenGL"
                os.contains("win") -> "opengl32"
                else -> "GL"
            }
            return Native.load(library, NativeOpenGl::class.java)
        }
    }
}
