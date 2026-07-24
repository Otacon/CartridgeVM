import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import nes.cartridge.Mapper4
import nes.cartridge.Mirroring

class Mapper4Test {
    @Test
    fun `CPU reads use MMC3 PRG mode zero`() {
        val prg = prgBanks(4)
        val mapper = Mapper4(prgRom = prg, chr = ByteArray(8192), isChrRam = false)

        mapper.cpuWrite(0x8000, 6)
        mapper.cpuWrite(0x8001, 1)
        mapper.cpuWrite(0x8000, 7)
        mapper.cpuWrite(0x8001, 0)

        assertEquals(0x11, mapper.cpuRead(0x8000))
        assertEquals(0x10, mapper.cpuRead(0xA000))
        assertEquals(0x12, mapper.cpuRead(0xC000))
        assertEquals(0x13, mapper.cpuRead(0xE000))
    }

    @Test
    fun `CPU reads use MMC3 PRG mode one`() {
        val prg = prgBanks(4)
        val mapper = Mapper4(prgRom = prg, chr = ByteArray(8192), isChrRam = false)

        mapper.cpuWrite(0x8000, 0x46)
        mapper.cpuWrite(0x8001, 1)
        mapper.cpuWrite(0x8000, 0x47)
        mapper.cpuWrite(0x8001, 0)

        assertEquals(0x12, mapper.cpuRead(0x8000))
        assertEquals(0x10, mapper.cpuRead(0xA000))
        assertEquals(0x11, mapper.cpuRead(0xC000))
        assertEquals(0x13, mapper.cpuRead(0xE000))
    }

    @Test
    fun `PPU reads use MMC3 CHR mode zero`() {
        val chr = chrBanks(8)
        val mapper = Mapper4(prgRom = prgBanks(4), chr = chr, isChrRam = false)

        mapper.cpuWrite(0x8000, 0)
        mapper.cpuWrite(0x8001, 2)
        mapper.cpuWrite(0x8000, 2)
        mapper.cpuWrite(0x8001, 6)

        assertEquals(0x22, mapper.ppuRead(0x0000))
        assertEquals(0x23, mapper.ppuRead(0x0400))
        assertEquals(0x26, mapper.ppuRead(0x1000))
    }

    @Test
    fun `PPU reads use MMC3 CHR mode one`() {
        val chr = chrBanks(8)
        val mapper = Mapper4(prgRom = prgBanks(4), chr = chr, isChrRam = false)

        mapper.cpuWrite(0x8000, 0x80)
        mapper.cpuWrite(0x8001, 2)
        mapper.cpuWrite(0x8000, 0x82)
        mapper.cpuWrite(0x8001, 6)

        assertEquals(0x26, mapper.ppuRead(0x0000))
        assertEquals(0x22, mapper.ppuRead(0x1000))
        assertEquals(0x23, mapper.ppuRead(0x1400))
    }

    @Test
    fun `PPU writes modify CHR RAM when present`() {
        val mapper = Mapper4(prgRom = prgBanks(4), chr = ByteArray(8192), isChrRam = true)

        mapper.cpuWrite(0x8000, 2)
        mapper.cpuWrite(0x8001, 3)
        mapper.ppuWrite(0x1000, 0x55)

        assertEquals(0x55, mapper.ppuRead(0x1000))
    }

    @Test
    fun `CPU reads and writes PRG RAM`() {
        val mapper = Mapper4(prgRom = prgBanks(4), chr = ByteArray(8192), isChrRam = false)

        mapper.cpuWrite(0x6000, 0x66)
        mapper.cpuWrite(0x7FFF, 0x77)

        assertEquals(0x66, mapper.cpuRead(0x6000))
        assertEquals(0x77, mapper.cpuRead(0x7FFF))
    }

    @Test
    fun `mirroring register overrides cartridge mirroring`() {
        val mapper = Mapper4(prgRom = prgBanks(4), chr = ByteArray(8192), isChrRam = false)

        mapper.cpuWrite(0xA000, 0)

        assertEquals(Mirroring.VERTICAL, mapper.mirroring())

        mapper.cpuWrite(0xA000, 1)

        assertEquals(Mirroring.HORIZONTAL, mapper.mirroring())
    }

    @Test
    fun `IRQ counter requests interrupt when enabled and counter reaches zero`() {
        val mapper = Mapper4(prgRom = prgBanks(4), chr = ByteArray(8192), isChrRam = false)
        mapper.cpuWrite(0xC000, 2)
        mapper.cpuWrite(0xDFFF, 0)
        mapper.cpuWrite(0xE001, 0)

        mapper.clockScanline()
        mapper.clockScanline()

        assertFalse(mapper.irqPending())

        mapper.clockScanline()

        assertTrue(mapper.irqPending())
    }

    @Test
    fun `IRQ disable clears pending interrupt`() {
        val mapper = Mapper4(prgRom = prgBanks(4), chr = ByteArray(8192), isChrRam = false)
        mapper.cpuWrite(0xC000, 1)
        mapper.cpuWrite(0xC001, 0)
        mapper.cpuWrite(0xE001, 0)
        mapper.clockScanline()
        mapper.clockScanline()

        assertTrue(mapper.irqPending())

        mapper.cpuWrite(0xE000, 0)

        assertFalse(mapper.irqPending())
    }

    private fun prgBanks(count: Int): ByteArray {
        val prg = ByteArray(count * 8192)
        var bank = 0
        while (bank < count) {
            prg[bank * 8192] = (0x10 + bank).toByte()
            bank++
        }
        return prg
    }

    private fun chrBanks(count: Int): ByteArray {
        val chr = ByteArray(count * 1024)
        var bank = 0
        while (bank < count) {
            chr[bank * 1024] = (0x20 + bank).toByte()
            bank++
        }
        return chr
    }
}
