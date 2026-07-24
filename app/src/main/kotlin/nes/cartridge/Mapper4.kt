package nes.cartridge

import nes.util.low16Bits
import nes.util.low8Bits
import nes.util.toUnsignedInt

class Mapper4(
    private val prgRom: ByteArray,
    private val chr: ByteArray,
    private val isChrRam: Boolean,
) : Mapper {
    private val prgBankCount = prgRom.size / PRG_BANK_SIZE
    private val chrBankCount = chr.size / CHR_BANK_SIZE
    private val prgRam = ByteArray(8 * 1024)
    private val registers = IntArray(8)

    private var selectedRegister = 0
    private var prgMode = false
    private var chrMode = false
    private var irqLatch = 0
    private var irqCounter = 0
    private var irqReload = false
    private var irqEnabled = false
    private var irqRequested = false
    private var mirroring: Mirroring? = null

    override fun cpuRead(address: Int): Int {
        val a = address.low16Bits()
        if (a in 0x6000..0x7FFF) return prgRam[a and 0x1FFF].toUnsignedInt()
        if (a < 0x8000) return 0
        val bank = when (a) {
            in 0x8000..0x9FFF -> if (prgMode) prgBankCount - 2 else registers[6]
            in 0xA000..0xBFFF -> registers[7]
            in 0xC000..0xDFFF -> if (prgMode) registers[6] else prgBankCount - 2
            else -> prgBankCount - 1
        }.floorMod(prgBankCount)
        val index = bank * PRG_BANK_SIZE + (a and 0x1FFF)
        return prgRom[index].toUnsignedInt()
    }

    override fun cpuWrite(address: Int, value: Int) {
        val a = address.low16Bits()
        val v = value.low8Bits()
        when (a) {
            in 0x6000..0x7FFF -> prgRam[a and 0x1FFF] = v.toByte()
            in 0x8000..0x9FFF -> if ((a and 1) == 0) {
                selectedRegister = v and 7
                prgMode = (v and 0x40) != 0
                chrMode = (v and 0x80) != 0
            } else {
                registers[selectedRegister] = v
            }
            in 0xA000..0xBFFF -> if ((a and 1) == 0) {
                mirroring = if ((v and 1) == 0) Mirroring.VERTICAL else Mirroring.HORIZONTAL
            }
            in 0xC000..0xDFFF -> if ((a and 1) == 0) {
                irqLatch = v
            } else {
                irqReload = true
            }
            in 0xE000..0xFFFF -> if ((a and 1) == 0) {
                irqEnabled = false
                irqRequested = false
            } else {
                irqEnabled = true
            }
        }
    }

    override fun ppuRead(address: Int): Int {
        return chr[mapChrAddress(address)].toUnsignedInt()
    }

    override fun ppuWrite(address: Int, value: Int) {
        if (isChrRam) {
            chr[mapChrAddress(address)] = value.toByte()
        }
    }

    override fun clockScanline() {
        if (irqCounter == 0 || irqReload) {
            irqCounter = irqLatch
            irqReload = false
        } else {
            irqCounter--
        }
        if (irqCounter == 0 && irqEnabled) {
            irqRequested = true
        }
    }

    override fun irqPending(): Boolean {
        return irqRequested
    }

    override fun mirroring(): Mirroring? {
        return mirroring
    }

    private fun mapChrAddress(address: Int): Int {
        val a = address and 0x1FFF
        val bank = when {
            !chrMode && a < 0x0800 -> evenChrBank(registers[0]) + ((a and 0x0400) shr 10)
            !chrMode && a < 0x1000 -> evenChrBank(registers[1]) + ((a and 0x0400) shr 10)
            !chrMode && a < 0x1400 -> registers[2]
            !chrMode && a < 0x1800 -> registers[3]
            !chrMode && a < 0x1C00 -> registers[4]
            !chrMode -> registers[5]
            chrMode && a < 0x0400 -> registers[2]
            chrMode && a < 0x0800 -> registers[3]
            chrMode && a < 0x0C00 -> registers[4]
            chrMode && a < 0x1000 -> registers[5]
            chrMode && a < 0x1800 -> evenChrBank(registers[0]) + ((a and 0x0400) shr 10)
            else -> evenChrBank(registers[1]) + ((a and 0x0400) shr 10)
        }.floorMod(chrBankCount)
        return bank * CHR_BANK_SIZE + (a and 0x03FF)
    }

    private fun evenChrBank(value: Int): Int {
        return value and 0xFE
    }

    private fun Int.floorMod(divisor: Int): Int {
        return ((this % divisor) + divisor) % divisor
    }

    companion object {
        private const val PRG_BANK_SIZE = 8 * 1024
        private const val CHR_BANK_SIZE = 1024
    }
}
