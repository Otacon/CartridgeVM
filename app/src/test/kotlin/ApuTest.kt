import kotlin.test.*
import nes.apu.NesApu

class ApuTest {
    @Test fun pulseRegisterProducesSamples() {
        val apu = NesApu()
        apu.cpuWrite(0x4015, 0x01)
        apu.cpuWrite(0x4000, 0x9F)
        apu.cpuWrite(0x4002, 0x20)
        apu.cpuWrite(0x4003, 0x08)
        apu.step(4000)
        assertTrue(apu.sampleCount > 0)
        assertTrue(apu.samples.any { it.toInt() != 0 })
    }

    @Test fun statusReflectsEnabledLengthCounters() {
        val apu = NesApu()
        apu.cpuWrite(0x4015, 0x0F)
        apu.cpuWrite(0x4003, 0x08)
        apu.cpuWrite(0x4007, 0x08)
        apu.cpuWrite(0x400B, 0x08)
        apu.cpuWrite(0x400F, 0x08)
        assertEquals(0x0F, apu.cpuRead(0x4015) and 0x0F)
        apu.cpuWrite(0x4015, 0x00)
        assertEquals(0, apu.cpuRead(0x4015) and 0x0F)
    }

    @Test fun pulseStillProducesSamplesWithNesTimerDivider() {
        val apu = NesApu(1_789_773)
        apu.cpuWrite(0x4015, 0x01)
        apu.cpuWrite(0x4000, 0x9F)
        apu.cpuWrite(0x4002, 0x00)
        apu.cpuWrite(0x4003, 0x08)
        apu.step(16)
        assertEquals(16, apu.sampleCount)
        assertTrue(apu.samples.take(apu.sampleCount).any { it.toInt() != 0 })
    }
}
