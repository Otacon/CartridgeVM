package frontend

interface EmulatorInput {

    fun init()

    fun poll()

    fun pause() = Unit

    fun close() = Unit
}

class CombinedEmulatorInput(
    private vararg val inputs: EmulatorInput,
) : EmulatorInput {

    override fun init() {
        inputs.forEach { it.init() }
    }

    override fun poll() {
        inputs.forEach { it.poll() }
    }

    override fun pause() {
        inputs.forEach { it.pause() }
    }

    override fun close() {
        inputs.forEach { it.close() }
    }
}

class DelegatingEmulatorInput(initialInput: EmulatorInput? = null) : EmulatorInput {
    var current: EmulatorInput? = initialInput

    override fun init() {
        current?.init()
    }

    override fun poll() {
        current?.poll()
    }

    override fun pause() {
        current?.pause()
    }

    override fun close() {
        current?.close()
        current = null
    }
}
