package nes.cpu

import nes.apu.NesApu
import nes.cartridge.Cartridge
import nes.input.NesController
import nes.ppu.Ppu

class CpuBus(
    private val cartridge: Cartridge,
    private val ppu: Ppu,
    private val controller: NesController,
    private val apu: NesApu
) {
    val ram = ByteArray(2048)
    var dmaCycles = 0
        private set

    fun read(address: Int): Int {
        return when (val a = address and 0xFFFF) {
            in 0x0000..0x1FFF -> ram[a and 0x07FF].toInt() and 0xFF
            in 0x2000..0x3FFF -> ppu.cpuRead(0x2000 + (a and 7))
            in 0x4000..0x4013 -> apu.cpuRead(a)
            0x4014 -> 0
            0x4015 -> apu.cpuRead(a)
            0x4016 -> controller.read()
            0x4017 -> 0
            in 0x4020..0xFFFF -> cartridge.mapper.cpuRead(a)
            else -> 0
        }
    }

    fun write(address: Int, value: Int) {
        val a = address and 0xFFFF
        val v = value and 0xFF
        when (a) {
            in 0x0000..0x1FFF -> ram[a and 0x07FF] = v.toByte()
            in 0x2000..0x3FFF -> ppu.cpuWrite(0x2000 + (a and 7), v)
            in 0x4000..0x4013 -> apu.cpuWrite(a, v)
            0x4014 -> doOamDma(v)
            0x4015 -> apu.cpuWrite(a, v)
            0x4016 -> controller.write(v)
            0x4017 -> apu.cpuWrite(a, v)
            in 0x4020..0xFFFF -> cartridge.mapper.cpuWrite(a, v)
        }
    }

    fun consumeDmaCycles(): Int {
        val value = dmaCycles
        dmaCycles = 0
        return value
    }

    private fun doOamDma(page: Int) {
        val bytes = ByteArray(256)
        val base = page shl 8
        var i = 0
        while (i < 256) {
            bytes[i] = read(base + i).toByte()
            i++
        }
        ppu.writeOamDma(bytes)
        dmaCycles += 513
    }
}
