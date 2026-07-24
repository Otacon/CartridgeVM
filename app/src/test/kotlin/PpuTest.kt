import kotlin.test.*
import nes.cartridge.Cartridge
import nes.cartridge.Mirroring
import nes.ppu.Ppu
import nes.ppu.PpuBus

class PpuTest {
    private fun ppu(mirroring: Mirroring = Mirroring.HORIZONTAL): Ppu {
        val chr = ByteArray(8192)
        val cart = Cartridge(0, mirroring, ByteArray(16 * 1024), chr, true, false)
        return Ppu(PpuBus(cart.mapper, cart.mirroring))
    }

    @Test fun nametableMirroring() { val p = ppu(Mirroring.VERTICAL); p.ppuWrite(0x2000, 0x22); assertEquals(0x22, p.ppuRead(0x2800)) }
    @Test fun paletteMirroring() { val p = ppu(); p.ppuWrite(0x3F00, 0x09); assertEquals(0x09, p.ppuRead(0x3F10)) }
    @Test fun ppuAddressWrites() { val p = ppu(); p.cpuWrite(6, 0x21); p.cpuWrite(6, 0x05); assertEquals(0x2105, p.v) }
    @Test fun scrollWriteLatch() { val p = ppu(); p.cpuWrite(5, 0x13); assertTrue(p.writeLatch); p.cpuWrite(5, 0x24); assertFalse(p.writeLatch) }
    @Test fun bufferedReads() { val p = ppu(); p.ppuWrite(0x2000, 0x55); p.cpuWrite(6, 0x20); p.cpuWrite(6, 0x00); assertEquals(0, p.cpuRead(7)); assertEquals(0x55, p.cpuRead(7)) }
    @Test fun vblankFlagAndStatusSideEffects() { val p = ppu(); repeat(241 * 341 + 2) { p.step() }; assertTrue((p.status and 0x80) != 0); p.cpuRead(2); assertFalse((p.status and 0x80) != 0); assertFalse(p.writeLatch) }
    @Test fun nmiTriggering() { val p = ppu(); p.cpuWrite(0, 0x80); repeat(241 * 341 + 2) { p.step() }; assertTrue(p.pollNmi()) }
    @Test fun basicBackgroundTileOutput() { val p = ppu(); p.ppuWrite(0x2000, 1); p.ppuWrite(0x0000 + 16, 0x80); p.ppuWrite(0x3F01, 0x22); p.cpuWrite(1, 0x08); repeat(2) { p.step() }; assertNotEquals(0, p.framebuffer[0]) }
    @Test fun backgroundUsesHorizontalNametableBit() { val p = ppu(Mirroring.VERTICAL); p.ppuWrite(0x2400, 1); p.ppuWrite(16, 0x80); p.ppuWrite(0x3F01, 0x22); p.cpuWrite(6, 0x24); p.cpuWrite(6, 0x00); p.cpuWrite(1, 0x08); repeat(2) { p.step() }; assertNotEquals(0, p.framebuffer[0]) }
    @Test fun basicSpriteOutput() { val p = ppu(); p.oam[0] = 0; p.oam[1] = 1; p.oam[3] = 0; p.ppuWrite(16, 0x80); p.ppuWrite(0x3F11, 0x22); p.cpuWrite(1, 0x10); repeat(344) { p.step() }; assertNotEquals(0, p.framebuffer[256]) }
    @Test fun spriteEightBySixteenOutput() { val p = ppu(); p.oam[0] = 0; p.oam[1] = 2; p.oam[3] = 0; p.ppuWrite(3 * 16, 0x80); p.ppuWrite(0x3F11, 0x22); p.cpuWrite(0, 0x20); p.cpuWrite(1, 0x10); repeat(341 * 9 + 2) { p.step() }; assertNotEquals(0, p.framebuffer[8 * 256]) }
    @Test fun spriteZeroHit() { val p = ppu(); p.ppuWrite(0x2000, 1); p.ppuWrite(16, 0x80); p.ppuWrite(17, 0x80); p.oam[0] = 0; p.oam[1] = 1; p.oam[3] = 0; p.cpuWrite(1, 0x18); repeat(344) { p.step() }; assertTrue((p.status and 0x40) != 0) }
}
