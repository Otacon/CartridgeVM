package nes

object Timing {
    const val CPU_HZ = 1_789_773.0
    const val PPU_PER_CPU = 3
    const val PPU_CYCLES_PER_SCANLINE = 341
    const val SCANLINES_PER_FRAME = 262
    const val FRAME_RATE = CPU_HZ / 29_780.5
    const val FRAME_NANOS = (1_000_000_000.0 / FRAME_RATE).toLong()
}
