import kotlin.test.*
import nes.cartridge.Cartridge
import nes.cartridge.CartridgeSocket
import nes.cartridge.Mapper0
import nes.cartridge.Mirroring
import nes.ppu.Ppu
import nes.ppu.PpuBus

class PpuTest {
    private fun ppu(mirroring: Mirroring = Mirroring.HORIZONTAL): Ppu {
        val chr = ByteArray(8192)
        val prgRom = ByteArray(16 * 1024)
        val cartridge = Cartridge(
            mirroring = mirroring,
            prgRom = prgRom,
            chr = chr,
            isChrRam = true,
            trainerPresent = false,
            mapper = Mapper0(prgRom = prgRom, chr = chr, isChrRam = true)
        )
        val socket = CartridgeSocket()
        socket.insert(cartridge)
        return Ppu(PpuBus(socket))
    }

    @Test
    fun `nametable addresses use cartridge mirroring`() {
        val ppu = ppu(Mirroring.VERTICAL)

        ppu.ppuWrite(0x2000, 0x22)

        assertEquals(0x22, ppu.ppuRead(0x2800))
    }

    @Test
    fun `palette addresses mirror universal background colors`() {
        val ppu = ppu()

        ppu.ppuWrite(0x3F00, 0x09)

        assertEquals(0x09, ppu.ppuRead(0x3F10))
    }

    @Test
    fun `PPU address register writes update VRAM address`() {
        val ppu = ppu()

        ppu.cpuWrite(6, 0x21)
        ppu.cpuWrite(6, 0x05)

        assertEquals(0x2105, ppu.v)
    }

    @Test
    fun `scroll register writes toggle write latch`() {
        val ppu = ppu()

        ppu.cpuWrite(5, 0x13)

        assertTrue(ppu.writeLatch)

        ppu.cpuWrite(5, 0x24)

        assertFalse(ppu.writeLatch)
    }

    @Test
    fun `PPUDATA reads are buffered outside palette range`() {
        val ppu = ppu()
        ppu.ppuWrite(0x2000, 0x55)
        ppu.cpuWrite(6, 0x20)
        ppu.cpuWrite(6, 0x00)

        assertEquals(0, ppu.cpuRead(7))
        assertEquals(0x55, ppu.cpuRead(7))
    }

    @Test
    fun `status read clears vblank flag and write latch`() {
        val ppu = ppu()

        repeat(241 * 341 + 2) { ppu.step() }

        assertTrue((ppu.status and 0x80) != 0)

        ppu.cpuRead(2)

        assertFalse((ppu.status and 0x80) != 0)
        assertFalse(ppu.writeLatch)
    }

    @Test
    fun `PPUCTRL enables NMI at vblank`() {
        val ppu = ppu()
        ppu.cpuWrite(0, 0x80)

        repeat(241 * 341 + 2) { ppu.step() }

        assertTrue(ppu.pollNmi())
    }

    @Test
    fun `background rendering draws non-zero pixels`() {
        val ppu = ppu()
        ppu.ppuWrite(0x2000, 1)
        ppu.ppuWrite(0x0000 + 16, 0x80)
        ppu.ppuWrite(0x3F01, 0x22)
        ppu.cpuWrite(1, 0x08)

        repeat(2) { ppu.step() }

        assertNotEquals(0, ppu.framebuffer[0])
    }

    @Test
    fun `background rendering uses horizontal nametable bit`() {
        val ppu = ppu(Mirroring.VERTICAL)
        ppu.ppuWrite(0x2400, 1)
        ppu.ppuWrite(16, 0x80)
        ppu.ppuWrite(0x3F01, 0x22)
        ppu.cpuWrite(6, 0x24)
        ppu.cpuWrite(6, 0x00)
        ppu.cpuWrite(1, 0x08)

        repeat(2) { ppu.step() }

        assertNotEquals(0, ppu.framebuffer[0])
    }

    @Test
    fun `sprite rendering draws non-zero pixels`() {
        val ppu = ppu()
        ppu.oam[0] = 0
        ppu.oam[1] = 1
        ppu.oam[3] = 0
        ppu.ppuWrite(16, 0x80)
        ppu.ppuWrite(0x3F11, 0x22)
        ppu.cpuWrite(1, 0x10)

        repeat(344) { ppu.step() }

        assertNotEquals(0, ppu.framebuffer[256])
    }

    @Test
    fun `eight by sixteen sprite rendering draws non-zero pixels`() {
        val ppu = ppu()
        ppu.oam[0] = 0
        ppu.oam[1] = 2
        ppu.oam[3] = 0
        ppu.ppuWrite(3 * 16, 0x80)
        ppu.ppuWrite(0x3F11, 0x22)
        ppu.cpuWrite(0, 0x20)
        ppu.cpuWrite(1, 0x10)

        repeat(341 * 9 + 2) { ppu.step() }

        assertNotEquals(0, ppu.framebuffer[8 * 256])
    }

    @Test
    fun `sprite zero hit is set when sprite overlaps background`() {
        val ppu = ppu()
        ppu.ppuWrite(0x2000, 1)
        ppu.ppuWrite(16, 0x80)
        ppu.ppuWrite(17, 0x80)
        ppu.oam[0] = 0
        ppu.oam[1] = 1
        ppu.oam[3] = 0
        ppu.cpuWrite(1, 0x18)

        repeat(344) { ppu.step() }

        assertTrue((ppu.status and 0x40) != 0)
    }
}
