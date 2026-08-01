import kotlin.test.Test
import kotlin.test.assertEquals
import nes.cartridge.Cartridge
import nes.cartridge.CartridgeSocket
import nes.cartridge.Mapper7
import nes.cartridge.Mirroring
import nes.ppu.PpuBus

class Mapper7Test {
    @Test
    fun `CPU reads use selected 32 KiB PRG bank`() {
        val mapper = Mapper7(prgRom = prgBanks(4), chrRam = ByteArray(8192))

        assertEquals(0x10, mapper.cpuRead(0x8000))
        assertEquals(0x10, mapper.cpuRead(0xFFFF))

        mapper.cpuWrite(0x8000, 2)

        assertEquals(0x12, mapper.cpuRead(0x8000))
        assertEquals(0x12, mapper.cpuRead(0xFFFF))
    }

    @Test
    fun `bank select mirrors values larger than available banks`() {
        val mapper = Mapper7(prgRom = prgBanks(4), chrRam = ByteArray(8192))

        mapper.cpuWrite(0xFFFF, 5)

        assertEquals(0x11, mapper.cpuRead(0x8000))
    }

    @Test
    fun `bank select uses bit three`() {
        val mapper = Mapper7(prgRom = prgBanks(16), chrRam = ByteArray(8192))

        mapper.cpuWrite(0x8000, 8)

        assertEquals(0x18, mapper.cpuRead(0x8000))
    }

    @Test
    fun `PPU reads and writes use CHR RAM`() {
        val mapper = Mapper7(prgRom = prgBanks(1), chrRam = ByteArray(8192))

        mapper.ppuWrite(0x0002, 0x44)

        assertEquals(0x44, mapper.ppuRead(0x2002))
    }

    @Test
    fun `write bit four controls single-screen mirroring`() {
        val mapper = Mapper7(prgRom = prgBanks(2), chrRam = ByteArray(8192))

        assertEquals(Mirroring.SINGLE_SCREEN_LOWER, mapper.mirroring())

        mapper.cpuWrite(0x8000, 0x10)

        assertEquals(Mirroring.SINGLE_SCREEN_UPPER, mapper.mirroring())

        mapper.cpuWrite(0x8000, 0x00)

        assertEquals(Mirroring.SINGLE_SCREEN_LOWER, mapper.mirroring())
    }

    @Test
    fun `reset restores first bank and lower one-screen mirroring`() {
        val mapper = Mapper7(prgRom = prgBanks(4), chrRam = ByteArray(8192))
        mapper.cpuWrite(0x8000, 0x13)

        mapper.reset()

        assertEquals(0x10, mapper.cpuRead(0x8000))
        assertEquals(Mirroring.SINGLE_SCREEN_LOWER, mapper.mirroring())
    }

    @Test
    fun `cartridge socket observes mapper-controlled one-screen mirroring`() {
        val prg = prgBanks(2)
        val chr = ByteArray(8192)
        val mapper = Mapper7(prg, chr)
        val socket = CartridgeSocket()
        socket.insert(Cartridge(Mirroring.HORIZONTAL, prg, chr, true, false, mapper))
        val ppuBus = PpuBus(socket)

        ppuBus.write(0x2000, 0x11)
        socket.cpuWrite(0x8000, 0x10)
        ppuBus.write(0x2000, 0x22)

        socket.cpuWrite(0x8000, 0x00)
        assertEquals(0x11, ppuBus.read(0x2C00))
        socket.cpuWrite(0x8000, 0x10)
        assertEquals(0x22, ppuBus.read(0x2800))
    }

    private fun prgBanks(count: Int): ByteArray {
        val prg = ByteArray(count * 32 * 1024)
        repeat(count) { bank ->
            prg[bank * 32 * 1024] = (0x10 + bank).toByte()
            prg[(bank + 1) * 32 * 1024 - 1] = (0x10 + bank).toByte()
        }
        return prg
    }
}
