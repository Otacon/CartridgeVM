package frontend

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class SharedFrameBuffer : VideoOutput {
    private val buffers = Array(2) { MutableVideoFrame() }
    private var bufferIndex = 0

    val initialFrame: VideoFrame = buffers[bufferIndex].frame
    private val _frames = MutableSharedFlow<VideoFrame>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val frames = _frames.asSharedFlow()

    override fun submit(frame: VideoFrame) {
        bufferIndex = (bufferIndex + 1) % buffers.size
        val target = buffers[bufferIndex]
        frame.background.copyInto(target.background)
        frame.sprites.copyInto(target.sprites)
        _frames.tryEmit(target.frame)
    }

    private class MutableVideoFrame {
        val background = IntArray(256 * 240)
        val sprites = IntArray(256 * 240)
        val frame = VideoFrame(background, sprites)
    }
}
