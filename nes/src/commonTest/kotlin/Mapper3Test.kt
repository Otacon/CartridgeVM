import kotlin.test.Test
import kotlin.test.assertEquals
import nes.cartridge.Mapper3

class Mapper3Test {
    @Test
    fun `CPU reads fixed PRG ROM`() {
        val prg = ByteArray(2 * 16 * 1024)
        prg[0] = 0x31
        prg[16 * 1024] = 0x32
        val mapper = Mapper3(prgRom = prg, chrRom = ByteArray(2 * 8192))

        assertEquals(0x31, mapper.cpuRead(0x8000))
        assertEquals(0x32, mapper.cpuRead(0xC000))
    }

    @Test
    fun `CPU reads mirror 16 KiB PRG ROM`() {
        val prg = ByteArray(16 * 1024)
        prg[0] = 0x33
        val mapper = Mapper3(prgRom = prg, chrRom = ByteArray(2 * 8192))

        assertEquals(0x33, mapper.cpuRead(0x8000))
        assertEquals(0x33, mapper.cpuRead(0xC000))
    }

    @Test
    fun `CPU writes select CHR ROM bank`() {
        val chr = ByteArray(4 * 8192)
        chr[0 * 8192] = 0x40
        chr[1 * 8192] = 0x41
        chr[2 * 8192] = 0x42
        chr[3 * 8192] = 0x43
        val mapper = Mapper3(prgRom = ByteArray(32 * 1024), chrRom = chr)

        assertEquals(0x40, mapper.ppuRead(0x0000))

        mapper.cpuWrite(0x8000, 2)

        assertEquals(0x42, mapper.ppuRead(0x0000))
    }

    @Test
    fun `CHR bank select mirrors values larger than available banks`() {
        val chr = ByteArray(2 * 8192)
        chr[1 * 8192] = 0x51
        val mapper = Mapper3(prgRom = ByteArray(32 * 1024), chrRom = chr)

        mapper.cpuWrite(0xFFFF, 3)

        assertEquals(0x51, mapper.ppuRead(0x0000))
    }

    @Test
    fun `PPU writes do not modify CHR ROM`() {
        val chr = ByteArray(8192)
        chr[0] = 0x60
        val mapper = Mapper3(prgRom = ByteArray(32 * 1024), chrRom = chr)

        mapper.ppuWrite(0x0000, 0x61)

        assertEquals(0x60, mapper.ppuRead(0x0000))
    }
}
