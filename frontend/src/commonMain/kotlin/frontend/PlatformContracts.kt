package frontend

interface AudioPipeline {
    fun submit(samples: ShortArray, count: Int)

    fun close() = Unit
}

interface VideoOutput {
    fun submit(framebuffer: IntArray)
}
