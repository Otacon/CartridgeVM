package nes.input

import di.AppScope
import me.tatarka.inject.annotations.Inject
import org.slf4j.LoggerFactory

@Inject
@AppScope
class NesController {
    companion object {
        const val A = 0
        const val B = 1
        const val SELECT = 2
        const val START = 3
        const val UP = 4
        const val DOWN = 5
        const val LEFT = 6
        const val RIGHT = 7
    }

    private var live = 0
    private var latched = 0
    private var index = 0
    private var strobe = false

    private val log = LoggerFactory.getLogger("NesController")

    fun reset() {
        latched = live
        index = 0
        strobe = false
    }

    fun setButton(button: Int, pressed: Boolean) {
        require(button in A..RIGHT) { "Invalid controller button: $button" }
        val buttonMask = 1 shl button
        setButtons(if (pressed) live or buttonMask else live and buttonMask.inv())
    }

    fun setButtons(buttons: Int) {
        val previous = live
        live = buttons and 0xFF
        if ((live and (1 shl LEFT)) != 0 && (live and (1 shl RIGHT)) != 0) live = live and (1 shl RIGHT).inv()
        if ((live and (1 shl UP)) != 0 && (live and (1 shl DOWN)) != 0) live = live and (1 shl DOWN).inv()
        logButtonEdges(previous, live)
        if (strobe) latched = live
    }

    fun write(value: Int) {
        strobe = (value and 1) != 0
        if (strobe) {
            latched = live
            index = 0
        }
    }

    fun read(): Int {
        val bit = if (index < 8) (latched shr index) and 1 else 1
        if (!strobe && index < 8) index++
        return 0x40 or bit
    }

    fun snapshot(): Int = live

    private fun logButtonEdges(previous: Int, current: Int) {
        val pressed = current and previous.inv()
        val released = previous and current.inv()
        logEdges(pressed, "pressed")
        logEdges(released, "released")
    }

    private fun logEdges(buttons: Int, action: String) {
        if ((buttons and (1 shl START)) != 0) log.debug("START {}", action)
        if ((buttons and (1 shl A)) != 0) log.debug("A {}", action)
        if ((buttons and (1 shl B)) != 0) log.debug("B {}", action)
        if ((buttons and (1 shl SELECT)) != 0) log.debug("SELECT {}", action)
        if ((buttons and (1 shl UP)) != 0) log.debug("UP {}", action)
        if ((buttons and (1 shl DOWN)) != 0) log.debug("DOWN {}", action)
        if ((buttons and (1 shl LEFT)) != 0) log.debug("LEFT {}", action)
        if ((buttons and (1 shl RIGHT)) != 0) log.debug("RIGHT {}", action)
    }
}
