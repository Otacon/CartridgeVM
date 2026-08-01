package nes

enum class ConsoleRegion(val timing: Timing) {
    NTSC(ntscTiming()),
    PAL(
        Timing(
            cpuHz = 1_662_607,
            ppuCyclesPerCpuNumerator = 16,
            ppuCyclesPerCpuDenominator = 5,
            scanlinesPerFrame = 312,
            apuFourStepEvents = intArrayOf(8313, 16627, 24939, 33253),
            apuFiveStepEvents = intArrayOf(8313, 16627, 24939, 41565, 41566),
            noisePeriods = intArrayOf(4, 8, 14, 30, 60, 88, 118, 148, 188, 236, 354, 472, 708, 944, 1890, 3778),
            dmcPeriods = intArrayOf(398, 354, 316, 298, 276, 236, 210, 198, 176, 148, 132, 118, 98, 78, 66, 50),
            skipsOddFrameDot = false,
        ),
    ),
    DENDY(
        Timing(
            cpuHz = 1_773_448,
            ppuCyclesPerCpuNumerator = 3,
            ppuCyclesPerCpuDenominator = 1,
            scanlinesPerFrame = 312,
            apuFourStepEvents = intArrayOf(7457, 14913, 22371, 29829),
            apuFiveStepEvents = intArrayOf(7457, 14913, 22371, 37281, 37282),
            noisePeriods = intArrayOf(4, 8, 16, 32, 64, 96, 128, 160, 202, 254, 380, 508, 762, 1016, 2034, 4068),
            dmcPeriods = intArrayOf(428, 380, 340, 320, 286, 254, 226, 214, 190, 160, 142, 128, 106, 84, 72, 54),
            skipsOddFrameDot = false,
        ),
    ),
    MULTI_REGION(ntscTiming())
}

data class Timing(
    val cpuHz: Int,
    val ppuCyclesPerCpuNumerator: Int,
    val ppuCyclesPerCpuDenominator: Int,
    val scanlinesPerFrame: Int,
    val apuFourStepEvents: IntArray,
    val apuFiveStepEvents: IntArray,
    val noisePeriods: IntArray,
    val dmcPeriods: IntArray,
    val skipsOddFrameDot: Boolean,
) {
    val frameRate: Double = cpuHz.toDouble() * ppuCyclesPerCpuNumerator /
            (ppuCyclesPerCpuDenominator * PpuTiming.PPU_CYCLES_PER_SCANLINE * scanlinesPerFrame)
    val frameNanos: Long = (1_000_000_000.0 / frameRate).toLong()

    companion object {
        val DEFAULT = ntscTiming()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as Timing

        if (cpuHz != other.cpuHz) return false
        if (ppuCyclesPerCpuNumerator != other.ppuCyclesPerCpuNumerator) return false
        if (ppuCyclesPerCpuDenominator != other.ppuCyclesPerCpuDenominator) return false
        if (scanlinesPerFrame != other.scanlinesPerFrame) return false
        if (skipsOddFrameDot != other.skipsOddFrameDot) return false
        if (frameRate != other.frameRate) return false
        if (frameNanos != other.frameNanos) return false
        if (!apuFourStepEvents.contentEquals(other.apuFourStepEvents)) return false
        if (!apuFiveStepEvents.contentEquals(other.apuFiveStepEvents)) return false
        if (!noisePeriods.contentEquals(other.noisePeriods)) return false
        if (!dmcPeriods.contentEquals(other.dmcPeriods)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = cpuHz
        result = 31 * result + ppuCyclesPerCpuNumerator
        result = 31 * result + ppuCyclesPerCpuDenominator
        result = 31 * result + scanlinesPerFrame
        result = 31 * result + skipsOddFrameDot.hashCode()
        result = 31 * result + frameRate.hashCode()
        result = 31 * result + frameNanos.hashCode()
        result = 31 * result + apuFourStepEvents.contentHashCode()
        result = 31 * result + apuFiveStepEvents.contentHashCode()
        result = 31 * result + noisePeriods.contentHashCode()
        result = 31 * result + dmcPeriods.contentHashCode()
        return result
    }
}

private fun ntscTiming() = Timing(
    cpuHz = 1_789_773,
    ppuCyclesPerCpuNumerator = 3,
    ppuCyclesPerCpuDenominator = 1,
    scanlinesPerFrame = 262,
    apuFourStepEvents = intArrayOf(7457, 14913, 22371, 29829),
    apuFiveStepEvents = intArrayOf(7457, 14913, 22371, 37281, 37282),
    noisePeriods = intArrayOf(4, 8, 16, 32, 64, 96, 128, 160, 202, 254, 380, 508, 762, 1016, 2034, 4068),
    dmcPeriods = intArrayOf(428, 380, 340, 320, 286, 254, 226, 214, 190, 160, 142, 128, 106, 84, 72, 54),
    skipsOddFrameDot = true,
)

object PpuTiming {
    const val PPU_CYCLES_PER_SCANLINE = 341
}
