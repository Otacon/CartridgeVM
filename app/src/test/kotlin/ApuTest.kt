import kotlin.test.*
import nes.apu.DmcDma
import nes.apu.NesApu
import nes.cartridge.Cartridge
import nes.cartridge.CartridgeSocket
import nes.cartridge.Mapper0
import nes.cartridge.Mirroring

class ApuTest {
    private fun apu(prg: ByteArray = ByteArray(16 * 1024)): NesApu {
        val chr = ByteArray(8192)
        val socket = CartridgeSocket()
        socket.insert(
            Cartridge(
                mirroring = Mirroring.HORIZONTAL,
                prgRom = prg,
                chr = chr,
                isChrRam = true,
                trainerPresent = false,
                mapper = Mapper0(prgRom = prg, chr = chr, isChrRam = true)
            )
        )
        return NesApu(DmcDma(socket))
    }

    @Test
    fun `pulse registers produce samples`() {
        val apu = apu()
        apu.cpuWrite(0x4015, 0x01)
        apu.cpuWrite(0x4000, 0x9F)
        apu.cpuWrite(0x4002, 0x20)
        apu.cpuWrite(0x4003, 0x08)
        apu.step(4000)
        assertTrue(apu.sampleCount > 0)
        assertTrue(apu.samples.any { it.toInt() != 0 })
    }

    @Test
    fun `status reflects enabled length counters`() {
        val apu = apu()
        apu.cpuWrite(0x4015, 0x0F)
        apu.cpuWrite(0x4003, 0x08)
        apu.cpuWrite(0x4007, 0x08)
        apu.cpuWrite(0x400B, 0x08)
        apu.cpuWrite(0x400F, 0x08)
        assertEquals(0x0F, apu.cpuRead(0x4015) and 0x0F)
        apu.cpuWrite(0x4015, 0x00)
        assertEquals(0, apu.cpuRead(0x4015) and 0x0F)
    }

    @Test
    fun `pulse still produces samples with NES timer divider`() {
        val apu = apu()
        apu.cpuWrite(0x4015, 0x01)
        apu.cpuWrite(0x4000, 0x9F)
        apu.cpuWrite(0x4002, 0x00)
        apu.cpuWrite(0x4003, 0x08)

        apu.step(4000)

        assertTrue(apu.sampleCount > 0)
        assertTrue(apu.samples.take(apu.sampleCount).any { it.toInt() != 0 })
    }

    @Test
    fun `DMC direct load contributes to output`() {
        val apu = apu()

        apu.cpuWrite(0x4011, 0x40)
        apu.step(100)

        assertTrue(apu.sampleCount > 0)
        assertTrue(apu.samples.take(apu.sampleCount).any { it.toInt() != 0 })
    }

    @Test
    fun `DMC fetches sample bytes from CPU memory reader`() {
        val prg = ByteArray(16 * 1024) { 0xFF.toByte() }
        val apu = apu(prg)
        apu.cpuWrite(0x4010, 0x0F)
        apu.cpuWrite(0x4011, 0x00)
        apu.cpuWrite(0x4012, 0x00)
        apu.cpuWrite(0x4013, 0x01)

        apu.cpuWrite(0x4015, 0x10)
        apu.step(2000)

        assertTrue((apu.cpuRead(0x4015) and 0x10) != 0)
        assertTrue(apu.samples.take(apu.sampleCount).any { it.toInt() != 0 })
    }
}
