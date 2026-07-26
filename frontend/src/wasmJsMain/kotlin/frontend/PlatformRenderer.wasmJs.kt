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
    private val upload = Uint8Array(256 * 240 * 4)

    fun attach(canvas: HTMLCanvasElement) {
        this.canvas = canvas
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
        val vertex = compileShader(context, WebGLRenderingContext.VERTEX_SHADER, VertexShader)
        val fragment = compileShader(context, WebGLRenderingContext.FRAGMENT_SHADER, if (crt) CrtFragmentShader else FragmentShader)
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

    private companion object {
        const val VertexShader = """
            attribute vec2 position;
            attribute vec2 texCoord;
            varying vec2 vTexCoord;
            void main() {
                vTexCoord = texCoord;
                gl_Position = vec4(position, 0.0, 1.0);
            }
        """

        const val FragmentShader = """
            precision mediump float;
            uniform sampler2D frameTexture;
            varying vec2 vTexCoord;
            void main() {
                gl_FragColor = texture2D(frameTexture, vTexCoord);
            }
        """

        const val CrtFragmentShader = """
            precision mediump float;
            uniform sampler2D frameTexture;
            uniform vec2 outputSize;
            uniform float time;
            varying vec2 vTexCoord;

            const vec2 sourceSize = vec2(256.0, 240.0);

            float random(vec2 point) {
                return fract(sin(dot(point, vec2(12.9898, 78.233))) * 43758.5453);
            }

            vec3 linearSample(vec2 coordinate) {
                return pow(texture2D(frameTexture, coordinate).rgb, vec3(2.2));
            }

            vec3 analogSignal(vec2 coordinate) {
                vec2 pixel = 1.0 / sourceSize;
                vec3 farLeft = linearSample(coordinate - vec2(1.65 * pixel.x, 0.0));
                vec3 left = linearSample(coordinate - vec2(0.70 * pixel.x, 0.0));
                vec3 center = linearSample(coordinate);
                vec3 right = linearSample(coordinate + vec2(0.70 * pixel.x, 0.0));
                vec3 farRight = linearSample(coordinate + vec2(1.65 * pixel.x, 0.0));

                vec3 signal;
                signal.r = farLeft.r * 0.07 +
                    left.r * 0.24 +
                    center.r * 0.45 +
                    right.r * 0.20 +
                    farRight.r * 0.04;
                signal.g = farLeft.g * 0.04 +
                    left.g * 0.20 +
                    center.g * 0.52 +
                    right.g * 0.20 +
                    farRight.g * 0.04;
                signal.b = farLeft.b * 0.04 +
                    left.b * 0.20 +
                    center.b * 0.45 +
                    right.b * 0.24 +
                    farRight.b * 0.07;
                return signal;
            }

            vec3 phosphorMask() {
                float row = mod(floor(gl_FragCoord.y / 2.0), 2.0);
                float column = mod(floor(gl_FragCoord.x / 2.0) + row, 3.0);
                vec3 mask = vec3(0.78);
                if (column < 1.0) {
                    mask.r = 1.14;
                } else if (column < 2.0) {
                    mask.g = 1.14;
                } else {
                    mask.b = 1.14;
                }
                return mask * mix(1.0, 0.94, row);
            }

            void main() {
                vec2 glassPoint = vTexCoord * 2.0 - 1.0;
                vec2 sampleCoordinate = glassPoint * 0.965 * 0.5 + 0.5;

                vec3 color = analogSignal(sampleCoordinate);
                float luminance = dot(color, vec3(0.2126, 0.7152, 0.0722));

                float linePosition = abs(fract(sampleCoordinate.y * sourceSize.y) - 0.5) * 2.0;
                float beamWidth = mix(0.34, 0.72, smoothstep(0.04, 0.82, luminance));
                float beam = exp(-(linePosition * linePosition) / (2.0 * beamWidth * beamWidth));
                color *= 0.54 + 0.59 * beam;

                vec2 bloomPixel = 1.0 / sourceSize;
                vec3 bloom = linearSample(sampleCoordinate + vec2(0.0, bloomPixel.y)) +
                    linearSample(sampleCoordinate - vec2(0.0, bloomPixel.y)) +
                    linearSample(sampleCoordinate + vec2(2.0 * bloomPixel.x, 0.0)) +
                    linearSample(sampleCoordinate - vec2(2.0 * bloomPixel.x, 0.0));
                bloom *= 0.25;
                color += max(bloom - vec3(0.24), 0.0) * 0.075;

                float maskStrength = smoothstep(640.0, 1050.0, outputSize.x);
                color *= mix(vec3(1.0), phosphorMask(), 0.72 * maskStrength);

                float vignette = 16.0 * sampleCoordinate.x * sampleCoordinate.y *
                    (1.0 - sampleCoordinate.x) * (1.0 - sampleCoordinate.y);
                vignette = pow(max(vignette, 0.0), 0.17);
                color *= 0.68 + 0.32 * vignette;

                float noise = random(gl_FragCoord.xy + vec2(time * 31.0, time * 17.0)) - 0.5;
                color *= 0.995 + noise * 0.018;
                color *= vec3(1.035, 1.0, 0.945);
                color = vec3(1.0) - exp(-color * 1.30);
                color = pow(max(color, 0.0), vec3(1.0 / 2.2));

                float reflection = pow(
                    max(0.0, 1.0 - length(glassPoint - vec2(-0.72, 0.78)) / 1.30),
                    4.0
                );
                color += vec3(0.055, 0.070, 0.080) * reflection;
                color += vec3(0.004, 0.006, 0.009);

                gl_FragColor = vec4(color, 1.0);
            }
        """
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
