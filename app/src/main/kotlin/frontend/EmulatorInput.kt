package frontend

interface EmulatorInput : AutoCloseable {
    fun poll()

    fun consumePause(): Boolean

    fun consumeReset(): Boolean

    fun quitRequested(): Boolean

    override fun close() = Unit
}
