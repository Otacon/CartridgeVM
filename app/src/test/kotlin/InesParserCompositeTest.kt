import nes.cartridge.*
import kotlin.io.path.createTempFile
import kotlin.io.path.deleteIfExists
import kotlin.io.path.writeBytes
import kotlin.test.*

class InesParserCompositeTest {

    private val utils = InesParserUtils()
    private val parser = InesParserComposite(
        inesParserV1 = InesParserV1(utils),
        inesParserV2 = InesParserV2(utils),
        utils = InesParserUtils()
    )


    @Test
    fun `routes iNES 1 ROMs to V1 parser`() {
        val cartridge = parser.parse(ines(prgBanks = 4, chrBanks = 0, flags6 = 0x20))

        assertEquals(64 * 1024, cartridge.prgRom.size)
        assertTrue(cartridge.mapper is Mapper2)
    }

    @Test
    fun `routes NES 2 ROMs to V2 parser`() {
        val cartridge = parser.parse(nes2(prgLsb = 1, chrLsb = 1))

        assertEquals(16 * 1024, cartridge.prgRom.size)
        assertFalse(cartridge.isChrRam)
    }

    @Test
    fun `parses Path and File entry points`() {
        val rom = createTempFile(prefix = "cartridgevm", suffix = ".nes")
        try {
            rom.writeBytes(ines())

            assertEquals(16 * 1024, parser.parse(rom).prgRom.size)
            assertEquals(16 * 1024, parser.parse(rom.toFile()).prgRom.size)
        } finally {
            rom.deleteIfExists()
        }
    }

    @Test
    fun `invalid magic throws before routing`() {
        assertFailsWith<RomFormatException> {
            parser.parse(ByteArray(16))
        }
    }

    private fun nes2(
        prgLsb: Int = 1,
        chrLsb: Int = 1,
        flags6: Int = 0,
        mapperUpper: Int = 0,
        submapper: Int = 0,
        sizeMsb: Int = 0,
        chrRamShift: Int = 0,
        timingMode: Int = 0,
        consoleType: Int = 0,
        miscRomCount: Int = 0,
        prgSize: Int = prgLsb * 16 * 1024,
        chrSize: Int = chrLsb * 8 * 1024,
    ): ByteArray {
        val header = ByteArray(16)
        header[0] = 'N'.code.toByte()
        header[1] = 'E'.code.toByte()
        header[2] = 'S'.code.toByte()
        header[3] = 0x1A
        header[4] = prgLsb.toByte()
        header[5] = chrLsb.toByte()
        header[6] = flags6.toByte()
        header[7] = (0x08 or consoleType).toByte()
        header[8] = ((submapper shl 4) or mapperUpper).toByte()
        header[9] = sizeMsb.toByte()
        header[11] = chrRamShift.toByte()
        header[12] = timingMode.toByte()
        header[14] = miscRomCount.toByte()
        return header + ByteArray(prgSize) + ByteArray(chrSize)
    }
}
