package nes.cartridge

class Mapper2(
    private val prgRom: ByteArray,
    private val chrRam: ByteArray,
) : Mapper {
    private val bankCount = prgRom.size / PRG_BANK_SIZE
    private var selectedBank = 0

    override fun cpuRead(address: Int): Int {
        val a = address and 0xFFFF
        if (a < 0x8000) return 0
        val bank = if (a < 0xC000) selectedBank else bankCount - 1
        val index = bank * PRG_BANK_SIZE + (a and 0x3FFF)
        return prgRom[index].toInt() and 0xFF
    }

    override fun cpuWrite(address: Int, value: Int) {
        if ((address and 0xFFFF) >= 0x8000) {
            selectedBank = (value and 0x0F) % bankCount
        }
    }

    override fun ppuRead(address: Int): Int {
        return chrRam[address and 0x1FFF].toInt() and 0xFF
    }

    override fun ppuWrite(address: Int, value: Int) {
        chrRam[address and 0x1FFF] = value.toByte()
    }

    companion object {
        private const val PRG_BANK_SIZE = 16 * 1024
    }
}
