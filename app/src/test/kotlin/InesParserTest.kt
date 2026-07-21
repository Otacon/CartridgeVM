import kotlin.test.*
import nes.cartridge.InesParser
import nes.cartridge.Mirroring
import nes.cartridge.RomFormatException

class InesParserTest {
    @Test fun validNrom128() { val c = InesParser.parse(ines(1, 1)); assertEquals(16 * 1024, c.prgRom.size); assertFalse(c.chrRam) }
    @Test fun validNrom256() { val c = InesParser.parse(ines(2, 1)); assertEquals(32 * 1024, c.prgRom.size) }
    @Test fun skipsTrainer() { val c = InesParser.parse(ines(1, 1, trainer = true, prgFill = 0x42)); assertTrue(c.trainerPresent); assertEquals(0x42, c.prgRom[0].toInt() and 0xFF) }
    @Test fun horizontalMirroring() { assertEquals(Mirroring.HORIZONTAL, InesParser.parse(ines(flags6 = 0)).mirroring) }
    @Test fun verticalMirroring() { assertEquals(Mirroring.VERTICAL, InesParser.parse(ines(flags6 = 1)).mirroring) }
    @Test fun chrRam() { val c = InesParser.parse(ines(1, 0)); assertTrue(c.chrRam); assertEquals(8192, c.chr.size) }
    @Test fun invalidMagic() { assertFailsWith<RomFormatException> { InesParser.parse(ByteArray(16)) } }
    @Test fun truncatedData() { assertFailsWith<RomFormatException> { InesParser.parse(ines().copyOf(20)) } }
    @Test fun unsupportedMapper() { assertFailsWith<RomFormatException> { InesParser.parse(ines(flags6 = 0x10)) } }
}
