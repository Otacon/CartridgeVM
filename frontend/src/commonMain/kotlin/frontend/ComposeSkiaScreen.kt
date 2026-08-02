package frontend

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.focusable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.skiaCanvas
import androidx.compose.ui.input.key.onPreviewKeyEvent
import kotlin.math.roundToInt

@Composable
fun ComposeSkiaScreen(
    frameBuffer: SharedFrameBuffer,
    renderer: PlatformRenderer,
    keyboardInput: PlatformKeyboardInput?,
    crt: Boolean,
    castShadow: Boolean,
    focusRequestKey: Any,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }
    val frame by frameBuffer.frames.collectAsState(frameBuffer.initialFrame)

    LaunchedEffect(focusRequestKey) {
        focusRequester.requestFocus()
    }

    DisposableEffect(renderer, crt, castShadow) {
        renderer.init(crt, castShadow)
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
        val width = size.width.roundToInt()
        val height = size.height.roundToInt()
        if (width > 0 && height > 0) {
            renderer.present(frame, width, height)
            renderer.draw(drawContext.canvas.skiaCanvas)
        }
    }
}
