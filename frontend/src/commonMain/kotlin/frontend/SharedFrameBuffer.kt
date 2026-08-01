package frontend

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class SharedFrameBuffer : VideoOutput {
    private val buffers = Array(2) { IntArray(256 * 240) }
    private var bufferIndex = 0

    val initialFrame: IntArray = buffers[bufferIndex]
    private val _frames = MutableSharedFlow<IntArray>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val frames = _frames.asSharedFlow()

    override fun submit(framebuffer: IntArray) {
        bufferIndex = (bufferIndex + 1) % buffers.size
        val frame = buffers[bufferIndex]
        framebuffer.copyInto(frame)
        _frames.tryEmit(frame)
    }
}
