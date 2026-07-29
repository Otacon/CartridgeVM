package frontend

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.focusable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.key.onPreviewKeyEvent
import kotlin.math.roundToInt

@Composable
fun ComposeSkiaScreen(
    frameBuffer: SharedFrameBuffer,
    renderer: PlatformRenderer,
    keyboardInput: PlatformKeyboardInput?,
    crt: Boolean,
    focusRequestKey: Any,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }
    var drawTick by remember { mutableLongStateOf(0L) }

    LaunchedEffect(focusRequestKey) {
        focusRequester.requestFocus()
    }

    LaunchedEffect(Unit) {
        while (true) {
            withFrameNanos { drawTick = it }
        }
    }

    DisposableEffect(renderer, crt) {
        renderer.init(crt)
        onDispose(renderer::close)
    }

    Canvas(
        modifier = modifier
            .focusRequester(focusRequester)
            .onPreviewKeyEvent { keyboardInput?.onKeyEvent(it) == true }
            .onFocusChanged { state ->
                if (!state.isFocused) keyboardInput?.releaseAll()
            }
            .focusable(),
    ) {
        drawTick
        val width = size.width.roundToInt()
        val height = size.height.roundToInt()
        if (width > 0 && height > 0) {
            renderer.present(frameBuffer.snapshot(), width, height)
            drawPlatformRenderer(renderer)
        }
    }
}

expect fun DrawScope.drawPlatformRenderer(renderer: PlatformRenderer)
