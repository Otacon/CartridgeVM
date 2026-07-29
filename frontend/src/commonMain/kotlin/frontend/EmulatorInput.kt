package frontend

interface EmulatorInput {
    fun poll()

    fun consumeReset(): Boolean

    fun quitRequested(): Boolean

    fun close() = Unit
}

abstract class BaseEmulatorInput : EmulatorInput {
    private var prevReset = false
    private var resetEdge = false

    protected fun updateControlEdges(reset: Boolean) {
        resetEdge = resetEdge || (reset && !prevReset)
        prevReset = reset
    }

    override fun consumeReset(): Boolean {
        val value = resetEdge
        resetEdge = false
        return value
    }
}

class DelegatingEmulatorInput(initialInput: EmulatorInput? = null) : EmulatorInput {
    var current: EmulatorInput? = initialInput

    override fun poll() {
        current?.poll()
    }

    override fun consumeReset(): Boolean = current?.consumeReset() == true

    override fun quitRequested(): Boolean = current?.quitRequested() == true

    override fun close() {
        current?.close()
        current = null
    }
}
