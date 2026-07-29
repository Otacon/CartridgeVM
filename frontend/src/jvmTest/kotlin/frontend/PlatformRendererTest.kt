package frontend

import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Color
import org.jetbrains.skia.Surface
import kotlin.test.Test
import kotlin.test.assertNotEquals

class PlatformRendererTest {
    @Test
    fun pixelSharpRendererDrawsFramebuffer() {
        assertNotEquals(Color.BLACK, renderCenterPixel(crt = false))
    }

    @Test
    fun crtRendererCompilesAndDrawsFramebuffer() {
        assertNotEquals(Color.BLACK, renderCenterPixel(crt = true))
    }

    private fun renderCenterPixel(crt: Boolean): Int {
        val renderer = PlatformRenderer()
        val surface = Surface.makeRasterN32Premul(512, 480)
        return try {
            renderer.init(crt)
            renderer.present(IntArray(256 * 240) { Color.WHITE }, 512, 480)
            renderer.draw(surface.canvas)

            val image = surface.makeImageSnapshot()
            try {
                val bitmap = Bitmap.makeFromImage(image)
                try {
                    bitmap.getColor(256, 240)
                } finally {
                    bitmap.close()
                }
            } finally {
                image.close()
            }
        } finally {
            renderer.close()
            surface.close()
        }
    }
}
