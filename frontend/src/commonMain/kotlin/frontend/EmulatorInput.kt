package frontend

interface EmulatorInput {
    fun poll()

    fun consumePause(): Boolean

    fun consumeReset(): Boolean

    fun quitRequested(): Boolean

    fun close() = Unit
}

abstract class BaseEmulatorInput : EmulatorInput {
    private var prevPause = false
    private var prevReset = false
    private var pauseEdge = false
    private var resetEdge = false

    protected fun updateControlEdges(pause: Boolean, reset: Boolean) {
        pauseEdge = pauseEdge || (pause && !prevPause)
        resetEdge = resetEdge || (reset && !prevReset)
        prevPause = pause
        prevReset = reset
    }

    override fun consumePause(): Boolean {
        val value = pauseEdge
        pauseEdge = false
        return value
    }

    override fun consumeReset(): Boolean {
        val value = resetEdge
        resetEdge = false
        return value
    }
}
