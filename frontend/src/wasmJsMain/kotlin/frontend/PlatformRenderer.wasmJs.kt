@file:OptIn(ExperimentalWasmJsInterop::class)

package frontend

import nes.util.low8Bits
import org.khronos.webgl.Uint8Array
import org.khronos.webgl.WebGLBuffer
import org.khronos.webgl.WebGLProgram
import org.khronos.webgl.WebGLRenderingContext
import org.khronos.webgl.WebGLShader
import org.khronos.webgl.WebGLTexture
import org.w3c.dom.HTMLCanvasElement

actual class PlatformRenderer actual constructor() : Renderer {
    private var canvas: HTMLCanvasElement? = null
    private var gl: WebGLRenderingContext? = null
    private var texture: WebGLTexture? = null
    private var program: WebGLProgram? = null
    private var vertexBuffer: WebGLBuffer? = null
    private var crtEnabled = false
    private var presentedFrames = 0L
    private var vertexShaderSource: String? = null
    private var fragmentShaderSource: String? = null
    private var crtFragmentShaderSource: String? = null
    private val upload = Uint8Array(256 * 240 * 4)

    fun attach(canvas: HTMLCanvasElement) {
        this.canvas = canvas
    }

    fun setShaderSources(vertex: String, fragment: String) {
        vertexShaderSource = vertex
        fragmentShaderSource = fragment
    }

    fun setCrtShaderSource(fragment: String) {
        crtFragmentShaderSource = fragment
    }

    actual override fun init(crt: Boolean) {
        close()
        crtEnabled = crt
        val target = requireNotNull(canvas) { "Web renderer requires a canvas" }
        val context = target.getContext("webgl") as? WebGLRenderingContext
            ?: target.getContext("experimental-webgl") as? WebGLRenderingContext
            ?: throw IllegalStateException("WebGL is not available")
        gl = context
        program = createProgram(context, crt)
        vertexBuffer = context.createBuffer()
        texture = context.createTexture()

        context.bindTexture(WebGLRenderingContext.TEXTURE_2D, texture)
        context.texParameteri(WebGLRenderingContext.TEXTURE_2D, WebGLRenderingContext.TEXTURE_MIN_FILTER, WebGLRenderingContext.NEAREST)
        context.texParameteri(WebGLRenderingContext.TEXTURE_2D, WebGLRenderingContext.TEXTURE_MAG_FILTER, WebGLRenderingContext.NEAREST)
        context.texParameteri(WebGLRenderingContext.TEXTURE_2D, WebGLRenderingContext.TEXTURE_WRAP_S, WebGLRenderingContext.CLAMP_TO_EDGE)
        context.texParameteri(WebGLRenderingContext.TEXTURE_2D, WebGLRenderingContext.TEXTURE_WRAP_T, WebGLRenderingContext.CLAMP_TO_EDGE)
        context.texImage2D(
            WebGLRenderingContext.TEXTURE_2D,
            0,
            WebGLRenderingContext.RGBA,
            256,
            240,
            0,
            WebGLRenderingContext.RGBA,
            WebGLRenderingContext.UNSIGNED_BYTE,
            upload,
        )

        context.bindBuffer(WebGLRenderingContext.ARRAY_BUFFER, vertexBuffer)
        context.bufferData(
            WebGLRenderingContext.ARRAY_BUFFER,
            jsFloatArrayOf(
                -1f, -1f, 0f, 1f,
                1f, -1f, 1f, 1f,
                -1f, 1f, 0f, 0f,
                1f, 1f, 1f, 0f,
            ),
            WebGLRenderingContext.STATIC_DRAW,
        )
    }

    actual override fun present(framebuffer: IntArray, windowWidth: Int, windowHeight: Int) {
        val context = requireNotNull(gl)
        val target = requireNotNull(canvas)
        val devicePixelRatio = jsDevicePixelRatio()
        val width = maxOf(1, (target.clientWidth * devicePixelRatio).toInt())
        val height = maxOf(1, (target.clientHeight * devicePixelRatio).toInt())
        if (target.width != width || target.height != height) {
            target.width = width
            target.height = height
        }

        var src = 0
        var dst = 0
        while (src < framebuffer.size && dst + 3 < upload.length) {
            val c = framebuffer[src]
            setUint8(upload, dst++, (c shr 16).low8Bits())
            setUint8(upload, dst++, (c shr 8).low8Bits())
            setUint8(upload, dst++, c.low8Bits())
            setUint8(upload, dst++, 255)
            src++
        }

        context.viewport(0, 0, width, height)
        context.clearColor(0f, 0f, 0f, 1f)
        context.clear(WebGLRenderingContext.COLOR_BUFFER_BIT)
        context.useProgram(program)
        if (crtEnabled) {
            context.uniform2f(context.getUniformLocation(program, "outputSize"), width.toFloat(), height.toFloat())
            context.uniform1f(context.getUniformLocation(program, "time"), presentedFrames / 60f)
        }
        context.bindTexture(WebGLRenderingContext.TEXTURE_2D, texture)
        context.texSubImage2D(
            WebGLRenderingContext.TEXTURE_2D,
            0,
            0,
            0,
            256,
            240,
            WebGLRenderingContext.RGBA,
            WebGLRenderingContext.UNSIGNED_BYTE,
            upload,
        )
        context.bindBuffer(WebGLRenderingContext.ARRAY_BUFFER, vertexBuffer)
        val position = context.getAttribLocation(program, "position")
        val texCoord = context.getAttribLocation(program, "texCoord")
        context.enableVertexAttribArray(position)
        context.vertexAttribPointer(position, 2, WebGLRenderingContext.FLOAT, false, 16, 0)
        context.enableVertexAttribArray(texCoord)
        context.vertexAttribPointer(texCoord, 2, WebGLRenderingContext.FLOAT, false, 16, 8)
        context.drawArrays(WebGLRenderingContext.TRIANGLE_STRIP, 0, 4)
        presentedFrames++
    }

    actual override fun close() {
        val context = gl ?: return
        texture?.let(context::deleteTexture)
        vertexBuffer?.let(context::deleteBuffer)
        program?.let(context::deleteProgram)
        texture = null
        vertexBuffer = null
        program = null
        gl = null
    }

    private fun createProgram(context: WebGLRenderingContext, crt: Boolean): WebGLProgram {
        val vertexSource = requireNotNull(vertexShaderSource) { "Missing WebGL vertex shader" }
        val fragmentSource = if (crt) {
            requireNotNull(crtFragmentShaderSource) { "Missing WebGL CRT fragment shader" }
        } else {
            requireNotNull(fragmentShaderSource) { "Missing WebGL fragment shader" }
        }
        val vertex = compileShader(context, WebGLRenderingContext.VERTEX_SHADER, vertexSource)
        val fragment = compileShader(context, WebGLRenderingContext.FRAGMENT_SHADER, fragmentSource)
        val linked = context.createProgram() ?: throw IllegalStateException("Unable to create WebGL program")
        context.attachShader(linked, vertex)
        context.attachShader(linked, fragment)
        context.linkProgram(linked)
        if (!webGlProgramParameter(context, linked, WebGLRenderingContext.LINK_STATUS)) {
            throw IllegalStateException("WebGL link failure: ${context.getProgramInfoLog(linked)}")
        }
        context.deleteShader(vertex)
        context.deleteShader(fragment)
        context.useProgram(linked)
        context.uniform1i(context.getUniformLocation(linked, "frameTexture"), 0)
        return linked
    }

    private fun compileShader(context: WebGLRenderingContext, type: Int, source: String): WebGLShader {
        val shader = context.createShader(type) ?: throw IllegalStateException("Unable to create WebGL shader")
        context.shaderSource(shader, source)
        context.compileShader(shader)
        if (!webGlShaderParameter(context, shader, WebGLRenderingContext.COMPILE_STATUS)) {
            throw IllegalStateException("WebGL shader failure: ${context.getShaderInfoLog(shader)}")
        }
        return shader
    }

}

@JsFun("(values) => new Float32Array(values)")
private external fun jsFloatArrayOf(values: JsArray<JsNumber>): org.khronos.webgl.Float32Array

private fun jsFloatArrayOf(vararg values: Float): org.khronos.webgl.Float32Array {
    val array = JsArray<JsNumber>()
    values.forEachIndexed { index, value ->
        array[index] = value.toDouble().toJsNumber()
    }
    return jsFloatArrayOf(array)
}

@JsFun("() => window.devicePixelRatio || 1")
private external fun jsDevicePixelRatio(): Double

@JsFun("(array, index, value) => { array[index] = value; }")
private external fun setUint8(array: Uint8Array, index: Int, value: Int)

@JsFun("(gl, target, parameter) => !!gl.getProgramParameter(target, parameter)")
private external fun webGlProgramParameter(gl: WebGLRenderingContext, program: WebGLProgram, parameter: Int): Boolean

@JsFun("(gl, target, parameter) => !!gl.getShaderParameter(target, parameter)")
private external fun webGlShaderParameter(gl: WebGLRenderingContext, shader: WebGLShader, parameter: Int): Boolean
