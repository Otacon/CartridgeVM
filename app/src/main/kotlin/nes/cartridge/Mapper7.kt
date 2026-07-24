package nes.cartridge

import nes.util.low16Bits
import nes.util.low8Bits
import nes.util.toUnsignedInt

class Mapper7(
    private val prgRom: ByteArray,
    private val chrRam: ByteArray,
) : Mapper {
    private val bankCount = prgRom.size / PRG_BANK_SIZE
    private var selectedBankBase = 0
    private var mirroring = Mirroring.SINGLE_SCREEN_LOWER

    override fun cpuRead(address: Int): Int {
        val a = address.low16Bits()
        if (a < 0x8000) return 0
        return prgRom[selectedBankBase + (a and 0x7FFF)].toUnsignedInt()
    }

    override fun cpuWrite(address: Int, value: Int) {
        if (address.low16Bits() < 0x8000) return
        val v = value.low8Bits()
        selectedBankBase = ((v and 0x07) % bankCount) * PRG_BANK_SIZE
        mirroring = if ((v and 0x10) == 0) Mirroring.SINGLE_SCREEN_LOWER else Mirroring.SINGLE_SCREEN_UPPER
    }

    override fun ppuRead(address: Int): Int {
        return chrRam[address and 0x1FFF].toUnsignedInt()
    }

    override fun ppuWrite(address: Int, value: Int) {
        chrRam[address and 0x1FFF] = value.toByte()
    }

    override fun reset() {
        selectedBankBase = 0
        mirroring = Mirroring.SINGLE_SCREEN_LOWER
    }

    override fun mirroring(): Mirroring = mirroring

    private companion object {
        const val PRG_BANK_SIZE = 32 * 1024
    }
}
