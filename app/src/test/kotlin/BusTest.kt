import kotlin.test.*
import nes.apu.NesApu
import nes.cartridge.Cartridge
import nes.cartridge.CartridgeSocket
import nes.cartridge.Mapper0
import nes.cartridge.Mirroring
import nes.cpu.CpuBus
import nes.input.NesController
import nes.ppu.Ppu
import nes.ppu.PpuBus

class BusTest {
    private fun cartridge(
        mirroring: Mirroring = Mirroring.HORIZONTAL,
        prgRom: ByteArray = ByteArray(16 * 1024),
        chr: ByteArray = ByteArray(8192),
        isChrRam: Boolean = true,
        trainerPresent: Boolean = false,
    ): Cartridge {
        return Cartridge(
            mirroring = mirroring,
            prgRom = prgRom,
            chr = chr,
            isChrRam = isChrRam,
            trainerPresent = trainerPresent,
            mapper = Mapper0(prgRom = prgRom, chr = chr, isChrRam = isChrRam)
        )
    }

    @Test
    fun `internal RAM mirrors every 2 KiB`() {
        val (_, bus, _) = cpuWithProgram(byteArrayOf(0xEA.toByte()))

        bus.write(0x0002, 0x66)

        assertEquals(0x66, bus.read(0x0802))
    }

    @Test
    fun `PPU registers mirror through CPU bus`() {
        val (_, bus, ppu) = cpuWithProgram(byteArrayOf(0xEA.toByte()))

        bus.write(0x2008, 0x80)

        assertEquals(0x80, ppu.ctrl)
    }

    @Test
    fun `cartridge socket reads mirrored PRG ROM`() {
        val prg = ByteArray(16 * 1024)
        prg[0] = 0x12
        val socket = CartridgeSocket()
        socket.insert(cartridge(prgRom = prg))

        assertEquals(0x12, socket.cpuRead(0x8000))
        assertEquals(0x12, socket.cpuRead(0xC000))
    }

    @Test
    fun `controller reads shift button state`() {
        val controller = NesController()
        controller.setButton(NesController.A, true)

        controller.write(1)
        controller.write(0)

        assertEquals(1, controller.read() and 1)
        assertEquals(0, controller.read() and 1)
    }

    @Test
    fun `cartridge reads return zero after removal`() {
        val prg = ByteArray(16 * 1024)
        prg[0] = 0x12
        val socket = CartridgeSocket()
        socket.insert(cartridge(prgRom = prg))

        assertEquals(0x12, socket.cpuRead(0x8000))

        socket.remove()

        assertEquals(0, socket.cpuRead(0x8000))
    }

    @Test
    fun `OAM DMA copies CPU page into PPU OAM`() {
        val socket = CartridgeSocket()
        socket.insert(cartridge())
        val ppu = Ppu(PpuBus(socket))
        val bus = CpuBus(socket, ppu, NesController(), NesApu())

        bus.write(0x0000, 0x77)
        bus.write(0x4014, 0)

        assertEquals(0x77, ppu.oam[0].toInt() and 0xFF)
        assertTrue(bus.consumeDmaCycles() >= 513)
    }

    @Test
    fun `APU status routes through CPU bus`() {
        val (_, bus, _) = cpuWithProgram(byteArrayOf(0xEA.toByte()))

        bus.write(0x4015, 0x01)
        bus.write(0x4003, 0x08)

        assertTrue((bus.read(0x4015) and 0x01) != 0)
    }
}
