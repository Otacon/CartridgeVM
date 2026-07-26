package frontend

interface AudioPipeline {
    fun submit(samples: ShortArray, count: Int)

    fun close() = Unit
}

interface VideoOutput {
    fun submit(framebuffer: IntArray)
}

interface RomPicker {
    suspend fun pickRom(): RomData?
}

data class RomData(
    val name: String,
    val bytes: ByteArray,
)
