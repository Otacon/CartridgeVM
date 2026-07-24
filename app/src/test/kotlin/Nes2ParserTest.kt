import nes.cartridge.*
import kotlin.test.*

class InesParserV2Test {
    private val parser = InesParserV2(utils = InesParserUtils())

    @Test
    fun `valid NES 2 NROM parses PRG ROM and CHR ROM`() {
        val cartridge = parser.parse(nes2(prgLsb = 1, chrLsb = 1))

        assertEquals(16 * 1024, cartridge.prgRom.size)
        assertEquals(8 * 1024, cartridge.chr.size)
        assertFalse(cartridge.isChrRam)
    }

    @Test
    fun `iNES 1 header throws ROM format exception`() {
        val exception = assertFailsWith<RomFormatException> {
            parser.parse(ines())
        }

        assertContains(exception.message.orEmpty(), "Expected NES 2.0")
    }

    @Test
    fun `NES 2 exponent multiplier sizes are decoded`() {
        val cartridge = parser.parse(
            nes2(
                prgLsb = 14 shl 2,
                chrLsb = 13 shl 2,
                sizeMsb = 0xFF,
                prgSize = 16 * 1024,
                chrSize = 8 * 1024,
            ),
        )

        assertEquals(16 * 1024, cartridge.prgRom.size)
        assertEquals(8 * 1024, cartridge.chr.size)
    }

    @Test
    fun `NES 2 MMC1 parses declared CHR RAM`() {
        val cartridge = parser.parse(
            nes2(prgLsb = 4, chrLsb = 0, flags6 = 0x10, chrRamShift = 7),
        )

        assertEquals(8 * 1024, cartridge.chr.size)
        assertTrue(cartridge.isChrRam)
        assertTrue(cartridge.mapper is Mapper1)
    }

    @Test
    fun `NES 2 UxROM uses declared CHR RAM size`() {
        val cartridge = parser.parse(
            nes2(prgLsb = 4, chrLsb = 0, flags6 = 0x20, chrRamShift = 7),
        )

        assertEquals(8 * 1024, cartridge.chr.size)
        assertTrue(cartridge.isChrRam)
        assertTrue(cartridge.mapper is Mapper2)
    }

    @Test
    fun `NES 2 AxROM parses declared CHR RAM`() {
        val cartridge = parser.parse(
            nes2(prgLsb = 4, chrLsb = 0, flags6 = 0x70, chrRamShift = 7),
        )

        assertEquals(8 * 1024, cartridge.chr.size)
        assertTrue(cartridge.isChrRam)
        assertTrue(cartridge.mapper is Mapper7)
    }

    @Test
    fun `NES 2 extended unsupported mapper throws ROM format exception`() {
        val exception = assertFailsWith<RomFormatException> {
            parser.parse(nes2(prgLsb = 2, chrLsb = 1, flags6 = 0x40, mapperUpper = 1))
        }

        assertContains(exception.message.orEmpty(), "mapper 260")
    }

    @Test
    fun `NES 2 unsupported submapper throws ROM format exception`() {
        val exception = assertFailsWith<RomFormatException> {
            parser.parse(nes2(submapper = 1))
        }

        assertContains(exception.message.orEmpty(), "submapper 1")
    }

    @Test
    fun `NES 2 missing CHR memory throws ROM format exception`() {
        assertFailsWith<RomFormatException> {
            parser.parse(nes2(chrLsb = 0, chrSize = 0))
        }
    }

    @Test
    fun `NES 2 mixed CHR ROM and RAM throws ROM format exception`() {
        val exception = assertFailsWith<RomFormatException> {
            parser.parse(nes2(chrRamShift = 7))
        }

        assertContains(exception.message.orEmpty(), "both CHR ROM and CHR RAM")
    }

    @Test
    fun `NES 2 PAL timing throws ROM format exception`() {
        val exception = assertFailsWith<RomFormatException> {
            parser.parse(nes2(timingMode = 1))
        }

        assertContains(exception.message.orEmpty(), "PAL")
    }

    @Test
    fun `NES 2 nonstandard console type throws ROM format exception`() {
        val exception = assertFailsWith<RomFormatException> {
            parser.parse(nes2(consoleType = 1))
        }

        assertContains(exception.message.orEmpty(), "console type")
    }

    @Test
    fun `NES 2 miscellaneous ROMs throw ROM format exception`() {
        val exception = assertFailsWith<RomFormatException> {
            parser.parse(nes2(miscRomCount = 1))
        }

        assertContains(exception.message.orEmpty(), "miscellaneous ROM")
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

    private fun ines(): ByteArray {
        val header = ByteArray(16)
        header[0] = 'N'.code.toByte()
        header[1] = 'E'.code.toByte()
        header[2] = 'S'.code.toByte()
        header[3] = 0x1A
        header[4] = 1
        header[5] = 1
        return header + ByteArray(16 * 1024) + ByteArray(8 * 1024)
    }
}
