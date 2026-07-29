package frontend

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.focusable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.key.onPreviewKeyEvent
import co.touchlab.kermit.Logger
import kotlinx.coroutines.launch
import nes.NesMachine
import kotlin.math.roundToInt

@Composable
fun ComposeSkiaScreen(
    machine: NesMachine,
    machineLock: Any,
    renderer: PlatformRenderer,
    audio: AudioPipeline,
    input: EmulatorInput,
    keyboardInput: PlatformKeyboardInput?,
    crt: Boolean,
    frameNanos: Long,
    enableFrameLimit: Boolean,
    isRunning: Boolean,
    modifier: Modifier = Modifier,
    onFps: (Int) -> Unit = {},
    onQuit: () -> Unit,
) {
    val frameBuffer = remember { SharedFrameBuffer() }
    val focusRequester = remember { FocusRequester() }
    val runtime = remember(machine, audio, input, frameBuffer) {
        EmulatorRuntime(machine, audio, input, frameBuffer)
    }
    val runningFlag = remember { PlatformAtomicBoolean(isRunning) }
    val fpsHandler by rememberUpdatedState(onFps)
    val quitHandler by rememberUpdatedState(onQuit)
    val uiScope = rememberCoroutineScope()
    var drawTick by remember { mutableLongStateOf(0L) }

    SideEffect {
        runningFlag.set(isRunning)
    }

    LaunchedEffect(Unit) {
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

    DisposableEffect(runtime, machineLock, enableFrameLimit) {
        val loop = startComposeEmulatorLoop(
            frameNanos = frameNanos,
            enableFrameLimit = !enableFrameLimit,
            step = {
                platformSynchronized(machineLock) {
                    runtime.step(runningFlag.get())
                }
            },
            onFps = { fps -> uiScope.launch { fpsHandler(fps) } },
            onQuit = { uiScope.launch { quitHandler() } },
            onError = { error ->
                log.e(error) { "Emulator thread failed" }
                uiScope.launch { quitHandler() }
            },
        )

        onDispose {
            loop.close()
            runtime.close()
        }
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

private class SharedFrameBuffer : VideoOutput {
    private val frame = IntArray(256 * 240)
    private val lock = Any()

    override fun submit(framebuffer: IntArray) {
        platformSynchronized(lock) {
            framebuffer.copyInto(frame, endIndex = minOf(framebuffer.size, frame.size))
        }
    }

    fun snapshot(): IntArray = platformSynchronized(lock) { frame.copyOf() }
}

expect fun <T> platformSynchronized(lock: Any, block: () -> T): T

expect class PlatformAtomicBoolean(initial: Boolean) {
    fun get(): Boolean

    fun set(value: Boolean)
}

interface ComposeEmulatorLoop : AutoCloseable

expect fun startComposeEmulatorLoop(
    frameNanos: Long,
    enableFrameLimit: Boolean,
    step: () -> EmulatorStepResult,
    onFps: (Int) -> Unit,
    onQuit: () -> Unit,
    onError: (Throwable) -> Unit,
): ComposeEmulatorLoop

expect fun DrawScope.drawPlatformRenderer(renderer: PlatformRenderer)

private val log = Logger.withTag("ComposeSkiaScreen")
