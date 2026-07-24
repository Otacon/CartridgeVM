package nes.ppu

import nes.cartridge.Mapper
import nes.cartridge.Mirroring

class PpuBus(
    private val mapper: Mapper,
    private val mirroring: Mirroring,
) {
    private val nametables = ByteArray(2048)
    private val paletteRam = ByteArray(32)

    fun read(address: Int): Int {
        val a = address and 0x3FFF
        return when {
            a < 0x2000 -> mapper.ppuRead(a)
            a < 0x3F00 -> nametables[mirrorNametable(a)].toInt() and 0xFF
            else -> paletteRam[mirrorPalette(a)].toInt() and 0x3F
        }
    }

    fun write(address: Int, value: Int) {
        val a = address and 0x3FFF
        when {
            a < 0x2000 -> mapper.ppuWrite(a, value)
            a < 0x3F00 -> nametables[mirrorNametable(a)] = value.toByte()
            else -> paletteRam[mirrorPalette(a)] = (value and 0x3F).toByte()
        }
    }

    private fun mirrorNametable(address: Int): Int {
        val index = (address - 0x2000) and 0x0FFF
        val table = index / 0x400
        val offset = index and 0x3FF
        val physical = when (mirroring) {
            Mirroring.VERTICAL -> table and 1
            Mirroring.HORIZONTAL -> table shr 1
        }
        return physical * 0x400 + offset
    }

    private fun mirrorPalette(address: Int): Int {
        var index = (address - 0x3F00) and 0x1F
        if (index == 0x10) index = 0
        if (index == 0x14) index = 4
        if (index == 0x18) index = 8
        if (index == 0x1C) index = 12
        return index
    }
}
