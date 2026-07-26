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
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.key.onPreviewKeyEvent
import co.touchlab.kermit.Logger
import kotlinx.coroutines.launch
import nes.NesMachine
import java.util.concurrent.atomic.AtomicBoolean
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
    unlimited: Boolean,
    running: Boolean,
    focusRequestKey: Int,
    modifier: Modifier = Modifier,
    onFps: (Int) -> Unit = {},
    onQuit: () -> Unit,
) {
    val frameBuffer = remember { SharedFrameBuffer() }
    val focusRequester = remember { FocusRequester() }
    val runtime = remember(machine, audio, input, frameBuffer) {
        EmulatorRuntime(machine, audio, input, frameBuffer)
    }
    val runningFlag = remember { AtomicBoolean(running) }
    val fpsHandler by rememberUpdatedState(onFps)
    val quitHandler by rememberUpdatedState(onQuit)
    val uiScope = rememberCoroutineScope()
    var drawTick by remember { mutableLongStateOf(0L) }

    SideEffect {
        runningFlag.set(running)
    }

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

    DisposableEffect(runtime, machineLock, unlimited) {
        val keepRunning = AtomicBoolean(true)
        val thread = Thread({
            val pacer = FramePacer(frameNanos)
            var frames = 0
            var fpsTime = System.nanoTime()

            try {
                while (keepRunning.get()) {
                    val result = synchronized(machineLock) {
                        runtime.step(runningFlag.get())
                    }
                    if (result.quitRequested) {
                        keepRunning.set(false)
                        uiScope.launch { quitHandler() }
                        break
                    }
                    if (!result.frameRendered) Thread.sleep(8)
                    if (!unlimited) pacer.waitForNextFrame()

                    frames++
                    val now = System.nanoTime()
                    if (now - fpsTime >= 1_000_000_000L) {
                        val measuredFps = frames
                        uiScope.launch { fpsHandler(measuredFps) }
                        frames = 0
                        fpsTime = now
                    }
                }
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
            } catch (error: Throwable) {
                log.e(error) { "Emulator thread failed" }
                uiScope.launch { quitHandler() }
            }
        }, "compose-skiko-emulator")
        thread.isDaemon = true
        thread.start()

        onDispose {
            keepRunning.set(false)
            thread.interrupt()
            thread.join()
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
            drawIntoCanvas { canvas -> renderer.draw(canvas.nativeCanvas) }
        }
    }
}

private class SharedFrameBuffer : VideoOutput {
    private val frame = IntArray(256 * 240)

    @Synchronized
    override fun submit(framebuffer: IntArray) {
        framebuffer.copyInto(frame, endIndex = minOf(framebuffer.size, frame.size))
    }

    @Synchronized
    fun snapshot(): IntArray = frame.copyOf()
}

private val log = Logger.withTag("ComposeSkiaScreen")
