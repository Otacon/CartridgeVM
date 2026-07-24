package nes.cartridge

import nes.util.low16Bits
import nes.util.toUnsignedInt

class Mapper2(
    private val prgRom: ByteArray,
    private val chrRam: ByteArray,
) : Mapper {
    private val bankCount = prgRom.size / PRG_BANK_SIZE
    private var selectedBank = 0

    override fun cpuRead(address: Int): Int {
        val a = address.low16Bits()
        if (a < 0x8000) return 0
        val bank = if (a < 0xC000) selectedBank else bankCount - 1
        val index = bank * PRG_BANK_SIZE + (a and 0x3FFF)
        return prgRom[index].toUnsignedInt()
    }

    override fun cpuWrite(address: Int, value: Int) {
        if (address.low16Bits() >= 0x8000) {
            selectedBank = (value and 0x0F) % bankCount
        }
    }

    override fun ppuRead(address: Int): Int {
        return chrRam[address and 0x1FFF].toUnsignedInt()
    }

    override fun ppuWrite(address: Int, value: Int) {
        chrRam[address and 0x1FFF] = value.toByte()
    }

    override fun clockScanline() = Unit

    override fun irqPending(): Boolean = false

    override fun mirroring(): Mirroring? = null

    companion object {
        private const val PRG_BANK_SIZE = 16 * 1024
    }
}
