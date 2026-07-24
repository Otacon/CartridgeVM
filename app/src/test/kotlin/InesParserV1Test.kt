import nes.cartridge.*
import nes.util.toUnsignedInt
import kotlin.test.*

class InesParserV1Test {
    private val parser = InesParserV1(utils = InesParserUtils())

    @Test
    fun `valid NROM-128 parses PRG ROM and CHR ROM`() {
        val cartridge = parser.parse(ines(1, 1))

        assertEquals(16 * 1024, cartridge.prgRom.size)
        assertFalse(cartridge.isChrRam)
    }

    @Test
    fun `valid NROM-256 parses PRG ROM`() {
        val cartridge = parser.parse(ines(2, 1))

        assertEquals(32 * 1024, cartridge.prgRom.size)
    }

    @Test
    fun `valid MMC1 parses PRG ROM and CHR ROM`() {
        val cartridge = parser.parse(ines(prgBanks = 4, chrBanks = 2, flags6 = 0x10))

        assertEquals(64 * 1024, cartridge.prgRom.size)
        assertEquals(16 * 1024, cartridge.chr.size)
        assertFalse(cartridge.isChrRam)
        assertTrue(cartridge.mapper is Mapper1)
    }

    @Test
    fun `valid UxROM parses PRG ROM and CHR RAM`() {
        val cartridge = parser.parse(ines(prgBanks = 4, chrBanks = 0, flags6 = 0x20))

        assertEquals(64 * 1024, cartridge.prgRom.size)
        assertEquals(8192, cartridge.chr.size)
        assertTrue(cartridge.isChrRam)
        assertTrue(cartridge.mapper is Mapper2)
    }

    @Test
    fun `valid CNROM parses PRG ROM and CHR ROM`() {
        val cartridge = parser.parse(ines(prgBanks = 2, chrBanks = 4, flags6 = 0x30))

        assertEquals(32 * 1024, cartridge.prgRom.size)
        assertEquals(32 * 1024, cartridge.chr.size)
        assertFalse(cartridge.isChrRam)
        assertTrue(cartridge.mapper is Mapper3)
    }

    @Test
    fun `valid MMC3 parses PRG ROM and CHR ROM`() {
        val cartridge = parser.parse(ines(prgBanks = 4, chrBanks = 2, flags6 = 0x40))

        assertEquals(64 * 1024, cartridge.prgRom.size)
        assertEquals(16 * 1024, cartridge.chr.size)
        assertFalse(cartridge.isChrRam)
        assertTrue(cartridge.mapper is Mapper4)
    }

    @Test
    fun `valid MMC3 with CHR RAM parses PRG ROM and CHR RAM`() {
        val cartridge = parser.parse(ines(prgBanks = 4, chrBanks = 0, flags6 = 0x40))

        assertEquals(64 * 1024, cartridge.prgRom.size)
        assertEquals(8192, cartridge.chr.size)
        assertTrue(cartridge.isChrRam)
        assertTrue(cartridge.mapper is Mapper4)
    }

    @Test
    fun `valid AxROM parses PRG ROM and CHR RAM`() {
        val cartridge = parser.parse(ines(prgBanks = 4, chrBanks = 0, flags6 = 0x70))

        assertEquals(64 * 1024, cartridge.prgRom.size)
        assertEquals(8192, cartridge.chr.size)
        assertTrue(cartridge.isChrRam)
        assertTrue(cartridge.mapper is Mapper7)
    }

    @Test
    fun `parser skips trainer bytes before PRG ROM`() {
        val cartridge = parser.parse(ines(1, 1, trainer = true, prgFill = 0x42))

        assertTrue(cartridge.trainerPresent)
        assertEquals(0x42, cartridge.prgRom[0].toUnsignedInt())
    }

    @Test
    fun `horizontal mirroring flag parses as horizontal`() {
        assertEquals(Mirroring.HORIZONTAL, parser.parse(ines(flags6 = 0)).mirroring)
    }

    @Test
    fun `vertical mirroring flag parses as vertical`() {
        assertEquals(Mirroring.VERTICAL, parser.parse(ines(flags6 = 1)).mirroring)
    }

    @Test
    fun `zero CHR banks creates CHR RAM`() {
        val cartridge = parser.parse(ines(1, 0))

        assertTrue(cartridge.isChrRam)
        assertEquals(8192, cartridge.chr.size)
    }

    @Test
    fun `invalid magic throws ROM format exception`() {
        assertFailsWith<RomFormatException> {
            parser.parse(ByteArray(16))
        }
    }

    @Test
    fun `NES 2 header throws ROM format exception`() {
        val exception = assertFailsWith<RomFormatException> {
            parser.parse(nes2())
        }

        assertContains(exception.message.orEmpty(), "Expected iNES 1.0")
    }

    @Test
    fun `truncated data throws ROM format exception`() {
        assertFailsWith<RomFormatException> {
            parser.parse(ines().copyOf(20))
        }
    }

    @Test
    fun `unsupported mapper throws ROM format exception`() {
        val exception = assertFailsWith<RomFormatException> {
            parser.parse(ines(flags6 = 0x50))
        }

        assertContains(exception.message.orEmpty(), "mapper 5")
    }

    @Test
    fun `UxROM with CHR ROM throws ROM format exception`() {
        assertFailsWith<RomFormatException> {
            parser.parse(ines(prgBanks = 4, chrBanks = 1, flags6 = 0x20))
        }
    }

    @Test
    fun `UxROM with one PRG bank throws ROM format exception`() {
        assertFailsWith<RomFormatException> {
            parser.parse(ines(prgBanks = 1, chrBanks = 0, flags6 = 0x20))
        }
    }

    @Test
    fun `CNROM with CHR RAM throws ROM format exception`() {
        assertFailsWith<RomFormatException> {
            parser.parse(ines(prgBanks = 2, chrBanks = 0, flags6 = 0x30))
        }
    }

    @Test
    fun `CNROM with invalid PRG size throws ROM format exception`() {
        assertFailsWith<RomFormatException> {
            parser.parse(ines(prgBanks = 3, chrBanks = 1, flags6 = 0x30))
        }
    }

    @Test
    fun `MMC3 with one PRG bank throws ROM format exception`() {
        assertFailsWith<RomFormatException> {
            parser.parse(ines(prgBanks = 1, chrBanks = 1, flags6 = 0x40))
        }
    }

    @Test
    fun `AxROM with CHR ROM throws ROM format exception`() {
        assertFailsWith<RomFormatException> {
            parser.parse(ines(prgBanks = 4, chrBanks = 1, flags6 = 0x70))
        }
    }

    private fun nes2(): ByteArray {
        val header = ByteArray(16)
        header[0] = 'N'.code.toByte()
        header[1] = 'E'.code.toByte()
        header[2] = 'S'.code.toByte()
        header[3] = 0x1A
        header[4] = 1
        header[5] = 1
        header[7] = 0x08
        return header + ByteArray(16 * 1024) + ByteArray(8 * 1024)
    }
}
