import kotlin.test.Test
import kotlin.test.assertEquals
import nes.cartridge.Mapper79
import nes.cartridge.Mirroring

class Mapper79Test {
    @Test
    fun `NINA-03 selects PRG bit and CHR bank from 4100 writes`() {
        val mapper = Mapper79(prgRom = prgBanks(2), chrRom = chrBanks(8))

        mapper.cpuWrite(0x4100, 0x0D)

        assertEquals(0x11, mapper.cpuRead(0x8000))
        assertEquals(0x25, mapper.ppuRead(0x0000))
    }

    @Test
    fun `Mapper 113 multicart mode selects larger PRG CHR and mirroring`() {
        val mapper = Mapper79(prgRom = prgBanks(8), chrRom = chrBanks(16), multicartMode = true)

        mapper.cpuWrite(0x4100, 0xC9)

        assertEquals(0x29, mapper.ppuRead(0x0000))
        assertEquals(Mirroring.VERTICAL, mapper.mirroring())
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
