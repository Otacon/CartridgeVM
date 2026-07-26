package frontend

import nes.input.NesController
import org.eclipse.swt.SWT

class KeyboardInput(
    private val window: SwtWindow,
    private val controller: NesController,
) : BaseEmulatorInput() {

    override fun poll() {
        var buttons = 0
        if ('z'.code.isKeyPressed() || 'Z'.code.isKeyPressed()) buttons = buttons or (1 shl NesController.A)
        if ('x'.code.isKeyPressed() || 'X'.code.isKeyPressed()) buttons = buttons or (1 shl NesController.B)
        if (SWT.SHIFT.isKeyPressed()) buttons = buttons or (1 shl NesController.SELECT)
        if (SWT.CR.code.isKeyPressed() || SWT.KEYPAD_CR.isKeyPressed()) {
            buttons = buttons or (1 shl NesController.START)
        }
        if (SWT.ARROW_UP.isKeyPressed()) buttons = buttons or (1 shl NesController.UP)
        if (SWT.ARROW_DOWN.isKeyPressed()) buttons = buttons or (1 shl NesController.DOWN)
        if (SWT.ARROW_LEFT.isKeyPressed()) buttons = buttons or (1 shl NesController.LEFT)
        if (SWT.ARROW_RIGHT.isKeyPressed()) buttons = buttons or (1 shl NesController.RIGHT)
        controller.setButtons(buttons)
        updateControlEdges('p'.code.isKeyPressed() || 'P'.code.isKeyPressed(), 'r'.code.isKeyPressed() || 'R'.code.isKeyPressed())
    }

    override fun quitRequested() = SWT.ESC.code.isKeyPressed()

    private fun Int.isKeyPressed(): Boolean = window.isKeyPressed(this)
}
