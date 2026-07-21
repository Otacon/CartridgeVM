import kotlin.test.*
import nes.apu.NesApu
import nes.cartridge.Cartridge
import nes.cartridge.Mapper0
import nes.cartridge.Mirroring
import nes.cpu.CpuBus
import nes.input.NesController
import nes.ppu.Ppu

class BusTest {
    @Test fun internalRamMirroring() { val (_, bus, _) = cpuWithProgram(byteArrayOf(0xEA.toByte())); bus.write(0x0002, 0x66); assertEquals(0x66, bus.read(0x0802)) }
    @Test fun ppuRegisterMirroring() { val (_, bus, ppu) = cpuWithProgram(byteArrayOf(0xEA.toByte())); bus.write(0x2008, 0x80); assertEquals(0x80, ppu.ctrl) }
    @Test fun cartridgePrgMirroring() { val prg = ByteArray(16 * 1024); prg[0] = 0x12; val c = Cartridge(0, Mirroring.HORIZONTAL, prg, ByteArray(8192), true, false); val m = Mapper0(c); assertEquals(0x12, m.cpuRead(0x8000)); assertEquals(0x12, m.cpuRead(0xC000)) }
    @Test fun controllerReads() { val c = NesController(); c.setButton(NesController.A, true); c.write(1); c.write(0); assertEquals(1, c.read() and 1); assertEquals(0, c.read() and 1) }
    @Test fun oamDmaRouting() { val cart = Cartridge(0, Mirroring.HORIZONTAL, ByteArray(16 * 1024), ByteArray(8192), true, false); val ppu = Ppu(Mapper0(cart), Mirroring.HORIZONTAL); val bus = CpuBus(Mapper0(cart), ppu, NesController(), NesApu()); bus.write(0x0000, 0x77); bus.write(0x4014, 0); assertEquals(0x77, ppu.oam[0].toInt() and 0xFF); assertTrue(bus.consumeDmaCycles() >= 513) }
    @Test fun apuStatusRouting() { val (_, bus, _) = cpuWithProgram(byteArrayOf(0xEA.toByte())); bus.write(0x4015, 0x01); bus.write(0x4003, 0x08); assertTrue((bus.read(0x4015) and 0x01) != 0) }
}
