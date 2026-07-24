package nes.cartridge

import nes.util.low16Bits
import nes.util.toUnsignedInt

class Mapper3(
    private val prgRom: ByteArray,
    private val chrRom: ByteArray,
) : Mapper {
    private val chrBankCount = chrRom.size / CHR_BANK_SIZE
    private var selectedChrBank = 0

    override fun cpuRead(address: Int): Int {
        val a = address.low16Bits()
        if (a < 0x8000) return 0
        val index = if (prgRom.size == PRG_BANK_SIZE) a and 0x3FFF else a and 0x7FFF
        return prgRom[index].toUnsignedInt()
    }

    override fun cpuWrite(address: Int, value: Int) {
        if (address.low16Bits() >= 0x8000) {
            selectedChrBank = (value and 0x03) % chrBankCount
        }
    }

    override fun ppuRead(address: Int): Int {
        val index = selectedChrBank * CHR_BANK_SIZE + (address and 0x1FFF)
        return chrRom[index].toUnsignedInt()
    }

    override fun ppuWrite(address: Int, value: Int) = Unit

    override fun clockScanline() = Unit

    override fun irqPending(): Boolean = false

    override fun mirroring(): Mirroring? = null

    companion object {
        private const val PRG_BANK_SIZE = 16 * 1024
        private const val CHR_BANK_SIZE = 8 * 1024
    }
}
