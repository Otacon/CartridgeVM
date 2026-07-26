package frontend

import co.touchlab.kermit.Logger
import me.tatarka.inject.annotations.Inject
import org.eclipse.swt.SWT
import org.eclipse.swt.events.KeyAdapter
import org.eclipse.swt.events.KeyEvent
import org.eclipse.swt.layout.GridData
import org.eclipse.swt.layout.GridLayout
import org.eclipse.swt.opengl.GLCanvas
import org.eclipse.swt.opengl.GLData
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Display
import org.eclipse.swt.widgets.FileDialog
import org.eclipse.swt.widgets.Menu
import org.eclipse.swt.widgets.MenuItem
import org.eclipse.swt.widgets.Shell
import java.nio.file.Path

@Inject
class SwtWindow : AutoCloseable {
    private lateinit var display: Display
    private lateinit var shell: Shell
    private lateinit var canvas: GLCanvas
    private val pressedKeys = mutableSetOf<Int>()
    private val log = Logger.withTag("SwtWindow")
    private var ignoredCocoaPaintFailure = false

    var onRomSelected: ((Path) -> Unit)? = null

    var title: String
        get() = shell.text
        set(value) {
            shell.text = value
        }

    val width: Int
        get() = canvas.clientArea.width

    val height: Int
        get() = canvas.clientArea.height

    fun create(width: Int, height: Int, aspectWidth: Int = width, aspectHeight: Int = height): GLCanvas {
        Display.setAppName("CartridgeVM")
        display = Display()
        shell = Shell(display)
        shell.layout = GridLayout(1, false).apply {
            marginWidth = 0
            marginHeight = 0
            verticalSpacing = 0
        }
        shell.menuBar = createMenuBar()

        val root = Composite(shell, SWT.NONE)
        root.layoutData = GridData(SWT.FILL, SWT.FILL, true, true)
        root.layout = GridLayout(1, false).apply {
            marginWidth = 0
            marginHeight = 0
            verticalSpacing = 0
        }

        val data = GLData().apply {
            doubleBuffer = true
            redSize = 8
            greenSize = 8
            blueSize = 8
            alphaSize = 8
            depthSize = 0
        }
        canvas = GLCanvas(root, SWT.NONE, data)
        canvas.layoutData = GridData(SWT.FILL, SWT.FILL, true, true)
        canvas.addKeyListener(object : KeyAdapter() {
            override fun keyPressed(e: KeyEvent) {
                pressedKeys += e.keyCode
            }

            override fun keyReleased(e: KeyEvent) {
                pressedKeys -= e.keyCode
            }
        })
        shell.minimumSize = shell.computeSize(aspectWidth, aspectHeight)
        shell.setSize(width, height)
        shell.open()
        display.asyncExec {
            if (!shell.isDisposed && !canvas.isDisposed) {
                shell.forceActive()
                canvas.forceFocus()
            }
        }
        canvas.setCurrent()
        return canvas
    }

    fun isKeyPressed(keyCode: Int): Boolean = keyCode in pressedKeys

    fun pollEvents() {
        while (true) {
            val dispatched = try {
                display.readAndDispatch()
            } catch (e: NullPointerException) {
                if (isCocoaNullGraphicsContextPaintFailure(e)) {
                    if (!ignoredCocoaPaintFailure) {
                        log.w(e) { "Ignoring SWT Cocoa GLCanvas paint event with null NSGraphicsContext" }
                        ignoredCocoaPaintFailure = true
                    }
                    true
                } else {
                    throw e
                }
            }
            if (!dispatched) break
        }
    }

    fun swapBuffers() {
        canvas.swapBuffers()
    }

    fun makeCurrent() {
        canvas.setCurrent()
    }

    fun shouldClose(): Boolean = shell.isDisposed

    fun requestClose() {
        shell.close()
    }

    override fun close() {
        if (::canvas.isInitialized && !canvas.isDisposed) canvas.dispose()
        if (::shell.isInitialized && !shell.isDisposed) shell.dispose()
        if (::display.isInitialized && !display.isDisposed) display.dispose()
    }

    private fun isCocoaNullGraphicsContextPaintFailure(e: NullPointerException): Boolean {
        if (!e.message.orEmpty().contains("NSGraphicsContext.saveGraphicsState")) return false
        return e.stackTrace.any { it.className == "org.eclipse.swt.widgets.Widget" && it.methodName == "drawRect" }
    }

    private fun createMenuBar(): Menu {
        val menuBar = Menu(shell, SWT.BAR)
        val fileMenuHeader = MenuItem(menuBar, SWT.CASCADE)
        fileMenuHeader.text = "File"

        val fileMenu = Menu(shell, SWT.DROP_DOWN)
        fileMenuHeader.menu = fileMenu

        val openItem = MenuItem(fileMenu, SWT.PUSH)
        openItem.text = "Open ROM..."
        openItem.addListener(SWT.Selection) { openRomDialog()?.let { onRomSelected?.invoke(it) } }

        MenuItem(fileMenu, SWT.SEPARATOR)

        val exitItem = MenuItem(fileMenu, SWT.PUSH)
        exitItem.text = "Exit"
        exitItem.addListener(SWT.Selection) { requestClose() }
        return menuBar
    }

    private fun openRomDialog(): Path? {
        val dialog = FileDialog(shell, SWT.OPEN)
        dialog.text = "Open NES ROM"
        dialog.setFilterNames("NES ROMs (*.nes)")
        dialog.setFilterExtensions("*.nes")
        return dialog.open()?.let { Path.of(it) }
    }
}
