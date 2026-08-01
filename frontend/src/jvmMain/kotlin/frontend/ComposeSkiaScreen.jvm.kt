package frontend

import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas

actual fun DrawScope.drawPlatformRenderer(renderer: PlatformRenderer) {
    renderer.draw(drawContext.canvas.nativeCanvas)
}
