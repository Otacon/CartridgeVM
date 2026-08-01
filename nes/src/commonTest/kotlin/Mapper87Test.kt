import kotlin.test.Test
import kotlin.test.assertEquals
import nes.cartridge.Mapper87

class Mapper87Test {
    @Test
    fun `Jaleco JF-xx swaps low CHR select bits`() {
        val mapper = Mapper87(prgRom = ByteArray(32 * 1024), chrRom = chrBanks(4))

        mapper.cpuWrite(0x6000, 0x01)

        assertEquals(0x22, mapper.ppuRead(0x0000))
    }

    private fun chrBanks(count: Int): ByteArray {
        val chr = ByteArray(count * 8 * 1024)
        repeat(count) { bank -> chr[bank * 8 * 1024] = (0x20 + bank).toByte() }
        return chr
    }
}
