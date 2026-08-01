import kotlin.test.Test
import kotlin.test.assertEquals
import nes.cartridge.Mapper11

class Mapper11Test {
    @Test
    fun `CPU writes select 32 KiB PRG and 8 KiB CHR banks with bus conflict`() {
        val prg = prgBanks(4)
        val chr = chrBanks(4)
        val mapper = Mapper11(prgRom = prg, chrRom = chr)
        prg[1] = 0xFF.toByte()

        mapper.cpuWrite(0x8001, 0x31)

        assertEquals(0x11, mapper.cpuRead(0x8000))
        assertEquals(0x23, mapper.ppuRead(0x0000))
    }

    private fun prgBanks(count: Int): ByteArray {
        val prg = ByteArray(count * 32 * 1024)
        repeat(count) { bank -> prg[bank * 32 * 1024] = (0x10 + bank).toByte() }
        return prg
    }

    private fun chrBanks(count: Int): ByteArray {
        val chr = ByteArray(count * 8 * 1024)
        repeat(count) { bank -> chr[bank * 8 * 1024] = (0x20 + bank).toByte() }
        return chr
    }
}
