package frontend

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.onPreviewKeyEvent
import co.touchlab.kermit.Logger
import frontend.EmulatorRunState.Running
import nes.NesMachine
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.awt.AWTGLCanvas
import org.lwjgl.opengl.awt.GLData
import java.awt.BorderLayout
import java.awt.KeyboardFocusManager
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import javax.swing.JPanel
import javax.swing.SwingUtilities

@Composable
fun ComposeOpenGlScreen(
    machine: NesMachine,
    renderer: OpenGlRenderer,
    audio: OpenAlAudio,
    input: EmulatorInput,
    keyboardInput: ComposeKeyboardInput?,
    crt: Boolean,
    frameNanos: Long,
    unlimited: Boolean,
    running: Boolean,
    pollInputOnEmulatorThread: Boolean,
    modifier: Modifier = Modifier,
    onFps: (Int) -> Unit = {},
    onQuit: () -> Unit,
) {
    val frameBuffer = remember { SharedFrameBuffer() }
    val focusRequester = remember { FocusRequester() }
    val canvas = remember {
        NesOpenGlCanvas(renderer, frameBuffer, crt)
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    DisposableEffect(keyboardInput) {
        if (keyboardInput == null) return@DisposableEffect onDispose { }
        val dispatcher = java.awt.KeyEventDispatcher { event ->
            keyboardInput.onAwtKeyEvent(event)
        }
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(dispatcher)
        onDispose {
            KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(dispatcher)
        }
    }

    DisposableEffect(canvas, machine, audio, input, running, unlimited) {
        var closed = false
        val thread = Thread({
            val pacer = FramePacer(frameNanos)
            var runState = Running
            var frames = 0
            var fpsTime = System.nanoTime()

            while (!closed) {
                if (pollInputOnEmulatorThread) input.poll()
                if (input.consumePause()) {
                    runState = if (runState == Running) EmulatorRunState.Paused else Running
                }
                if (input.consumeReset()) {
                    machine.reset()
                    runState = Running
                }
                if (input.quitRequested()) {
                    SwingUtilities.invokeLater(onQuit)
                }

                if (running && runState == Running) {
                    machine.runUntilFrame { if (pollInputOnEmulatorThread) input.poll() }
                    audio.submit(machine.apu.samples, machine.apu.sampleCount)
                    frameBuffer.update(machine.ppu.framebuffer)
                } else {
                    Thread.sleep(8)
                }
                canvas.renderOnEdt()
                if (!unlimited) pacer.waitForNextFrame()

                frames++
                val now = System.nanoTime()
                if (now - fpsTime >= 1_000_000_000L) {
                    onFps(frames)
                    frames = 0
                    fpsTime = now
                }
            }
        }, "compose-opengl-emulator")
        thread.isDaemon = true
        thread.start()

        onDispose {
            closed = true
            thread.join(1_000)
            input.close()
        }
    }

    Box(
        modifier = modifier
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent { keyboardInput?.onKeyEvent(it) == true },
    ) {
        SwingPanel(
            modifier = Modifier.matchParentSize(),
            factory = {
                val keyListener = object : KeyAdapter() {
                    override fun keyPressed(e: KeyEvent) {
                        keyboardInput?.onAwtKeyEvent(e)
                    }

                    override fun keyReleased(e: KeyEvent) {
                        keyboardInput?.onAwtKeyEvent(e)
                    }
                }
                JPanel(BorderLayout()).apply {
                    isFocusable = true
                    addKeyListener(keyListener)
                    canvas.isFocusable = true
                    canvas.addKeyListener(keyListener)
                    add(canvas, BorderLayout.CENTER)
                    SwingUtilities.invokeLater { canvas.requestFocusInWindow() }
                }
            },
        )
    }
}

private enum class EmulatorRunState { Running, Paused }

private class NesOpenGlCanvas(
    private val renderer: OpenGlRenderer,
    private val frameBuffer: SharedFrameBuffer,
    private val crt: Boolean,
) : AWTGLCanvas(GLData().apply {
    majorVersion = 2
    minorVersion = 1
    samples = 0
    swapInterval = 0
}) {
    private val log = Logger.withTag("NesOpenGlCanvas")
    private var initialized = false

    init {
        background = java.awt.Color.BLACK
    }

    fun renderOnEdt() {
        if (!isDisplayable) return
        SwingUtilities.invokeLater {
            if (isDisplayable) render()
        }
    }

    override fun initGL() {
        try {
            GL.createCapabilities()
            renderer.init(crt)
            initialized = true
            log.d { "OpenGL canvas initialized" }
        } catch (e: Throwable) {
            log.e(e) { "Unable to initialize OpenGL canvas" }
            throw e
        }
    }

    override fun paintGL() {
        try {
            if (!initialized) return
            renderer.present(frameBuffer.snapshot(), width, height)
            swapBuffers()
        } catch (e: Throwable) {
            log.e(e) { "Unable to render OpenGL frame" }
            throw e
        }
    }

    override fun removeNotify() {
        if (initialized) {
            runInContext {
                renderer.close()
                initialized = false
            }
        }
        super.removeNotify()
    }
}

private class SharedFrameBuffer {
    private val frame = IntArray(256 * 240)

    @Synchronized
    fun update(source: IntArray) {
        source.copyInto(frame, endIndex = minOf(source.size, frame.size))
    }

    @Synchronized
    fun snapshot(): IntArray = frame.copyOf()
}
