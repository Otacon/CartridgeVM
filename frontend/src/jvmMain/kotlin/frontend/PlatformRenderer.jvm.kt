package frontend

import me.tatarka.inject.annotations.Inject
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Color
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorSpace
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.FilterTileMode
import org.jetbrains.skia.Image
import org.jetbrains.skia.ImageInfo
import org.jetbrains.skia.Paint
import org.jetbrains.skia.Rect
import org.jetbrains.skia.RuntimeEffect
import org.jetbrains.skia.RuntimeShaderBuilder
import org.jetbrains.skia.SamplingMode

@Inject
actual class PlatformRenderer actual constructor() : Renderer, AutoCloseable {
    private val upload = ByteArray(FRAME_WIDTH * FRAME_HEIGHT * BYTES_PER_PIXEL)
    private val imageInfo = ImageInfo(
        FRAME_WIDTH,
        FRAME_HEIGHT,
        ColorType.RGBA_8888,
        ColorAlphaType.OPAQUE,
        ColorSpace.sRGB,
    )
    private var frameImage: Image? = null
    private var backgroundPaint: Paint? = null
    private var framePaint: Paint? = null
    private var crtEffect: RuntimeEffect? = null
    private var crtBuilder: RuntimeShaderBuilder? = null
    private var crtEnabled = false
    private var initialized = false
    private var outputWidth = 0
    private var outputHeight = 0
    private var presentedFrames = 0L

    actual override fun init(crt: Boolean) {
        release()
        try {
            crtEnabled = crt
            backgroundPaint = Paint().apply { color = Color.BLACK }
            framePaint = Paint().apply { isAntiAlias = false }
            if (crt) {
                val source = checkNotNull(javaClass.getResourceAsStream(CRT_SHADER_RESOURCE)) {
                    "Missing shader resource: $CRT_SHADER_RESOURCE"
                }.bufferedReader().use { it.readText() }
                crtEffect = RuntimeEffect.makeForShader(source)
                crtBuilder = RuntimeShaderBuilder(requireNotNull(crtEffect))
            }
            presentedFrames = 0L
            initialized = true
        } catch (error: Throwable) {
            release()
            throw error
        }
    }

    actual override fun present(framebuffer: IntArray, windowWidth: Int, windowHeight: Int) {
        check(initialized) { "Skiko renderer is not initialized" }
        require(framebuffer.size >= FRAME_WIDTH * FRAME_HEIGHT) { "Incomplete NES framebuffer" }

        var src = 0
        var dst = 0
        while (src < FRAME_WIDTH * FRAME_HEIGHT) {
            val color = framebuffer[src++]
            upload[dst++] = (color shr 16).toByte()
            upload[dst++] = (color shr 8).toByte()
            upload[dst++] = color.toByte()
            upload[dst++] = 0xFF.toByte()
        }

        frameImage?.close()
        frameImage = Image.makeRaster(imageInfo, upload, FRAME_WIDTH * BYTES_PER_PIXEL)
        outputWidth = windowWidth
        outputHeight = windowHeight
    }

    fun draw(canvas: Canvas) {
        val image = frameImage ?: return
        if (outputWidth <= 0 || outputHeight <= 0) return

        val output = Rect.makeWH(outputWidth.toFloat(), outputHeight.toFloat())
        canvas.drawRect(output, requireNotNull(backgroundPaint))
        val destination = destinationRect()
        if (crtEnabled) {
            drawCrt(canvas, image, destination)
        } else {
            canvas.drawImageRect(
                image,
                SOURCE_RECT,
                destination,
                SamplingMode.DEFAULT,
                null,
                true,
            )
        }
        presentedFrames++
    }

    actual override fun close() {
        release()
    }

    private fun drawCrt(canvas: Canvas, image: Image, destination: Rect) {
        val frameShader = image.makeShader(
            FilterTileMode.CLAMP,
            FilterTileMode.CLAMP,
            SamplingMode.LINEAR,
            null,
        )
        val builder = requireNotNull(crtBuilder)
        try {
            builder.child("frameTexture", frameShader)
            builder.uniform("outputSize", outputWidth.toFloat(), outputHeight.toFloat())
            builder.uniform("destinationOrigin", destination.left, destination.top)
            builder.uniform("destinationSize", destination.width, destination.height)
            builder.uniform("time", presentedFrames / 60f)

            val shader = builder.makeShader()
            val paint = requireNotNull(framePaint)
            try {
                paint.shader = shader
                canvas.drawRect(destination, paint)
            } finally {
                paint.shader = null
                shader.close()
            }
        } finally {
            frameShader.close()
        }
    }

    private fun destinationRect(): Rect {
        val width = outputWidth.toFloat()
        val height = outputHeight.toFloat()
        val (destinationWidth, destinationHeight) = if (crtEnabled) {
            val scale = minOf(width / FRAME_WIDTH, height / FRAME_HEIGHT)
            FRAME_WIDTH * scale to FRAME_HEIGHT * scale
        } else {
            val scale = maxOf(1, minOf(outputWidth / FRAME_WIDTH, outputHeight / FRAME_HEIGHT))
            FRAME_WIDTH * scale.toFloat() to FRAME_HEIGHT * scale.toFloat()
        }
        val left = (width - destinationWidth) * 0.5f
        val top = (height - destinationHeight) * 0.5f
        return Rect.makeLTRB(left, top, left + destinationWidth, top + destinationHeight)
    }

    private fun release() {
        framePaint?.shader = null
        frameImage?.close()
        crtBuilder?.close()
        crtEffect?.close()
        framePaint?.close()
        backgroundPaint?.close()
        frameImage = null
        crtBuilder = null
        crtEffect = null
        framePaint = null
        backgroundPaint = null
        initialized = false
    }

    private companion object {
        const val FRAME_WIDTH = 256
        const val FRAME_HEIGHT = 240
        const val BYTES_PER_PIXEL = 4
        const val CRT_SHADER_RESOURCE = "/shaders/crt.sksl"
        val SOURCE_RECT = Rect.makeWH(FRAME_WIDTH.toFloat(), FRAME_HEIGHT.toFloat())
    }
}
