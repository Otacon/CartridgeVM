package nes.cartridge

import nes.util.toUnsignedInt

class Mapper0(
    private val prgRom: ByteArray,
    private val chr: ByteArray,
    private val isChrRam: Boolean,
) : Mapper {
    override fun cpuRead(address: Int): Int {
        if (address < 0x8000) return 0
        val index = if (prgRom.size == 0x4000) address and 0x3FFF else address and 0x7FFF
        return prgRom[index].toUnsignedInt()
    }

    override fun cpuWrite(address: Int, value: Int) = Unit

    override fun ppuRead(address: Int): Int = chr[address and 0x1FFF].toUnsignedInt()

    override fun ppuWrite(address: Int, value: Int) {
        if (isChrRam) chr[address and 0x1FFF] = value.toByte()
    }

    override fun clockScanline() = Unit

    override fun irqPending(): Boolean = false

    override fun mirroring(): Mirroring? = null
}
