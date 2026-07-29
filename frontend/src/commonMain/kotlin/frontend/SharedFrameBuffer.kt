package frontend

class SharedFrameBuffer : VideoOutput {
    private val frame = IntArray(256 * 240)
    private val lock = Any()

    override fun submit(framebuffer: IntArray) {
        platformSynchronized(lock) {
            framebuffer.copyInto(frame, endIndex = minOf(framebuffer.size, frame.size))
        }
    }

    fun snapshot(): IntArray = platformSynchronized(lock) { frame.copyOf() }
}
