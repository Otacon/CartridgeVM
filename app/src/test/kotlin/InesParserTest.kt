import kotlin.test.*
import nes.cartridge.InesParser
import nes.cartridge.Mapper2
import nes.cartridge.Mapper3
import nes.cartridge.Mirroring
import nes.cartridge.RomFormatException

class InesParserTest {
    private val parser = InesParser()

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
    fun `parser skips trainer bytes before PRG ROM`() {
        val cartridge = parser.parse(ines(1, 1, trainer = true, prgFill = 0x42))

        assertTrue(cartridge.trainerPresent)
        assertEquals(0x42, cartridge.prgRom[0].toInt() and 0xFF)
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
    fun `truncated data throws ROM format exception`() {
        assertFailsWith<RomFormatException> {
            parser.parse(ines().copyOf(20))
        }
    }

    @Test
    fun `unsupported mapper throws ROM format exception`() {
        assertFailsWith<RomFormatException> {
            parser.parse(ines(flags6 = 0x10))
        }
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
}
