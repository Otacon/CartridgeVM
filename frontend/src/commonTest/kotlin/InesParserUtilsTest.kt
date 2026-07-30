import nes.cartridge.InesParserUtils
import kotlin.test.Test
import kotlin.test.assertContentEquals

class InesParserUtilsTest {
    private val utils = InesParserUtils()

    @Test
    fun `ROM hash bytes exclude iNES header`() {
        val prgAndChr = byteArrayOf(1, 2, 3, 4)

        assertContentEquals(
            prgAndChr,
            utils.romBytesForHash(ines(prgBanks = 0, chrBanks = 0) + prgAndChr),
        )
    }

    @Test
    fun `ROM hash bytes exclude trainer`() {
        val rom = ines(prgBanks = 0, chrBanks = 0, trainer = true) + byteArrayOf(1, 2, 3, 4)

        assertContentEquals(
            byteArrayOf(1, 2, 3, 4),
            utils.romBytesForHash(rom),
        )
    }
}
