import kotlin.test.Test
import kotlin.test.assertEquals
import nes.cartridge.Mapper2

class Mapper2Test {
    @Test
    fun `CPU reads use selected lower bank and fixed upper bank`() {
        val prg = ByteArray(4 * 16 * 1024)
        prg[0 * 16 * 1024] = 0x10
        prg[1 * 16 * 1024] = 0x11
        prg[2 * 16 * 1024] = 0x12
        prg[3 * 16 * 1024] = 0x13
        val mapper = Mapper2(prgRom = prg, chrRam = ByteArray(8192))

        assertEquals(0x10, mapper.cpuRead(0x8000))
        assertEquals(0x13, mapper.cpuRead(0xC000))

        mapper.cpuWrite(0x8000, 2)

        assertEquals(0x12, mapper.cpuRead(0x8000))
        assertEquals(0x13, mapper.cpuRead(0xC000))
    }

    @Test
    fun `bank select mirrors values larger than available banks`() {
        val prg = ByteArray(4 * 16 * 1024)
        prg[1 * 16 * 1024] = 0x21
        val mapper = Mapper2(prgRom = prg, chrRam = ByteArray(8192))

        mapper.cpuWrite(0xFFFF, 5)

        assertEquals(0x21, mapper.cpuRead(0x8000))
    }

    @Test
    fun `submapper 2 applies bus conflicts`() {
        val prg = ByteArray(4 * 16 * 1024)
        prg[0] = 0x01
        prg[1 * 16 * 1024] = 0x21
        val mapper = Mapper2(prgRom = prg, chrRam = ByteArray(8192), hasBusConflicts = true)

        mapper.cpuWrite(0x8000, 0x03)

        assertEquals(0x21, mapper.cpuRead(0x8000))
    }

    @Test
    fun `PPU reads and writes use CHR RAM`() {
        val mapper = Mapper2(prgRom = ByteArray(2 * 16 * 1024), chrRam = ByteArray(8192))

        mapper.ppuWrite(0x0002, 0x44)

        assertEquals(0x44, mapper.ppuRead(0x2002))
    }
}
