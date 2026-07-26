import kotlin.test.Test
import kotlin.test.assertEquals
import nes.cartridge.Cartridge
import nes.cartridge.CartridgeSocket
import nes.cartridge.Mapper1
import nes.cartridge.Mirroring
import nes.ppu.PpuBus

class Mapper1Test {
    @Test
    fun `default PRG mode switches lower bank and fixes last bank`() {
        val mapper = mapper(prgBanks = 4)

        writeRegister(mapper, 0xE000, 1)

        assertEquals(0x11, mapper.cpuRead(0x8000))
        assertEquals(0x13, mapper.cpuRead(0xC000))
    }

    @Test
    fun `PRG mode two fixes first bank and switches upper bank`() {
        val mapper = mapper(prgBanks = 4)
        writeRegister(mapper, 0x8000, 0x08)
        writeRegister(mapper, 0xE000, 2)

        assertEquals(0x10, mapper.cpuRead(0x8000))
        assertEquals(0x12, mapper.cpuRead(0xC000))
    }

    @Test
    fun `PRG modes zero and one switch a 32 KiB bank`() {
        val mapper = mapper(prgBanks = 4)
        writeRegister(mapper, 0x8000, 0)
        writeRegister(mapper, 0xE000, 2)

        assertEquals(0x12, mapper.cpuRead(0x8000))
        assertEquals(0x13, mapper.cpuRead(0xC000))
    }

    @Test
    fun `CHR mode zero switches one 8 KiB bank`() {
        val mapper = mapper(chrBanks = 4)
        writeRegister(mapper, 0xA000, 2)

        assertEquals(0x22, mapper.ppuRead(0x0000))
        assertEquals(0x23, mapper.ppuRead(0x1000))
    }

    @Test
    fun `CHR mode one switches independent 4 KiB banks`() {
        val mapper = mapper(chrBanks = 4)
        writeRegister(mapper, 0x8000, 0x10)
        writeRegister(mapper, 0xA000, 1)
        writeRegister(mapper, 0xC000, 3)

        assertEquals(0x21, mapper.ppuRead(0x0000))
        assertEquals(0x23, mapper.ppuRead(0x1000))
    }

    @Test
    fun `CHR RAM writes follow selected bank`() {
        val mapper = Mapper1(ByteArray(32 * 1024), ByteArray(8 * 1024), true)
        writeRegister(mapper, 0x8000, 0x10)
        mapper.ppuWrite(0x0000, 0x45)
        writeRegister(mapper, 0xA000, 1)

        assertEquals(0, mapper.ppuRead(0x0000))

        writeRegister(mapper, 0xA000, 0)
        assertEquals(0x45, mapper.ppuRead(0x0000))
    }

    @Test
    fun `PRG RAM can be disabled with PRG register`() {
        val mapper = mapper()
        mapper.cpuWrite(0x6000, 0x66)

        assertEquals(0x66, mapper.cpuRead(0x6000))

        writeRegister(mapper, 0xE000, 0x10)
        mapper.cpuWrite(0x6000, 0x77)

        assertEquals(0, mapper.cpuRead(0x6000))

        writeRegister(mapper, 0xE000, 0)
        assertEquals(0x66, mapper.cpuRead(0x6000))
    }

    @Test
    fun `reset write clears partial serial load and restores fixed last PRG mode`() {
        val mapper = mapper(prgBanks = 4)
        mapper.cpuWrite(0x8000, 1)
        mapper.cpuWrite(0x8000, 1)
        mapper.cpuWrite(0x8000, 0x80)
        writeRegister(mapper, 0xE000, 1)

        assertEquals(0x11, mapper.cpuRead(0x8000))
        assertEquals(0x13, mapper.cpuRead(0xC000))
    }

    @Test
    fun `control register selects all mirroring modes`() {
        val mapper = mapper()

        writeRegister(mapper, 0x8000, 0)
        assertEquals(Mirroring.SINGLE_SCREEN_LOWER, mapper.mirroring())
        writeRegister(mapper, 0x8000, 1)
        assertEquals(Mirroring.SINGLE_SCREEN_UPPER, mapper.mirroring())
        writeRegister(mapper, 0x8000, 2)
        assertEquals(Mirroring.VERTICAL, mapper.mirroring())
        writeRegister(mapper, 0x8000, 3)
        assertEquals(Mirroring.HORIZONTAL, mapper.mirroring())
    }

    @Test
    fun `one-screen mirroring preserves separate lower and upper nametables`() {
        val prg = ByteArray(32 * 1024)
        val chr = ByteArray(8 * 1024)
        val mapper = Mapper1(prg, chr, true)
        val socket = CartridgeSocket()
        socket.insert(Cartridge(Mirroring.HORIZONTAL, prg, chr, true, false, mapper))
        val ppuBus = PpuBus(socket)

        ppuBus.write(0x2000, 0x11)
        writeRegister(socket, 0x8000, 1)
        ppuBus.write(0x2000, 0x22)

        writeRegister(socket, 0x8000, 0)
        assertEquals(0x11, ppuBus.read(0x2C00))
        writeRegister(socket, 0x8000, 1)
        assertEquals(0x22, ppuBus.read(0x2800))
    }

    private fun mapper(prgBanks: Int = 2, chrBanks: Int = 2): Mapper1 {
        val prg = ByteArray(prgBanks * 16 * 1024)
        repeat(prgBanks) { prg[it * 16 * 1024] = (0x10 + it).toByte() }
        val chr = ByteArray(chrBanks * 4 * 1024)
        repeat(chrBanks) { chr[it * 4 * 1024] = (0x20 + it).toByte() }
        return Mapper1(prg, chr, false)
    }

    private fun writeRegister(mapper: Mapper1, address: Int, value: Int) {
        repeat(5) { mapper.cpuWrite(address, (value shr it) and 1) }
    }

    private fun writeRegister(socket: CartridgeSocket, address: Int, value: Int) {
        repeat(5) { socket.cpuWrite(address, (value shr it) and 1) }
    }
}
