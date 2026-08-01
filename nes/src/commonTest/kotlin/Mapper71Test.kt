import kotlin.test.Test
import kotlin.test.assertEquals
import nes.cartridge.Mapper71
import nes.cartridge.Mirroring

class Mapper71Test {
    @Test
    fun `BF909x selects switchable lower PRG bank and fixed upper bank`() {
        val mapper = Mapper71(prgRom = prgBanks(4), chrRam = ByteArray(8192))

        mapper.cpuWrite(0x8000, 2)

        assertEquals(0x12, mapper.cpuRead(0x8000))
        assertEquals(0x13, mapper.cpuRead(0xC000))
    }

    @Test
    fun `BF9097 mode updates one-screen mirroring below C000`() {
        val mapper = Mapper71(prgRom = prgBanks(4), chrRam = ByteArray(8192), bf9097Mode = true)

        mapper.cpuWrite(0x9000, 0x10)

        assertEquals(Mirroring.SINGLE_SCREEN_LOWER, mapper.mirroring())
    }

    private fun prgBanks(count: Int): ByteArray {
        val prg = ByteArray(count * 16 * 1024)
        repeat(count) { bank -> prg[bank * 16 * 1024] = (0x10 + bank).toByte() }
        return prg
    }
}
