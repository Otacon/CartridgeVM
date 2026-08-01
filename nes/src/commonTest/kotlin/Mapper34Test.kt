import kotlin.test.Test
import kotlin.test.assertEquals
import nes.cartridge.Mapper34

class Mapper34Test {
    @Test
    fun `BNROM variant selects 32 KiB PRG bank with bus conflict and CHR RAM`() {
        val prg = prgBanks(4)
        val chr = ByteArray(8192)
        val mapper = Mapper34(prgRom = prg, chr = chr, isChrRam = true, prgRamSize = 8192)
        prg[1] = 0xFF.toByte()

        mapper.cpuWrite(0x8001, 3)
        mapper.ppuWrite(0x0000, 0x44)

        assertEquals(0x13, mapper.cpuRead(0x8000))
        assertEquals(0x44, mapper.ppuRead(0x2000))
    }

    @Test
    fun `NINA-001 variant uses PRG RAM registers for PRG and CHR banks`() {
        val prg = prgBanks(2)
        val chr = chrBanks4k(4)
        val mapper = Mapper34(prgRom = prg, chr = chr, isChrRam = false, prgRamSize = 8192)

        mapper.cpuWrite(0x7FFD, 1)
        mapper.cpuWrite(0x7FFE, 2)
        mapper.cpuWrite(0x7FFF, 3)

        assertEquals(0x11, mapper.cpuRead(0x8000))
        assertEquals(0x22, mapper.ppuRead(0x0000))
        assertEquals(0x23, mapper.ppuRead(0x1000))
    }

    private fun prgBanks(count: Int): ByteArray {
        val prg = ByteArray(count * 32 * 1024)
        repeat(count) { bank -> prg[bank * 32 * 1024] = (0x10 + bank).toByte() }
        return prg
    }

    private fun chrBanks4k(count: Int): ByteArray {
        val chr = ByteArray(count * 4 * 1024)
        repeat(count) { bank -> chr[bank * 4 * 1024] = (0x20 + bank).toByte() }
        return chr
    }
}
