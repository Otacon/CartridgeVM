package nes.cpu

import nes.apu.NesApu
import nes.cartridge.CartridgeSocket
import nes.input.NesController
import nes.ppu.Ppu
import nes.util.low16Bits
import nes.util.low8Bits
import nes.util.toUnsignedInt

class CpuBus(
    private val cartridgeSocket: CartridgeSocket,
    private val ppu: Ppu,
    private val controller: NesController,
    private val apu: NesApu,
    private val cpuStall: CpuStall,
) {
    val ram = ByteArray(2048)
    private val oamDmaBuffer = ByteArray(256)
    private var openBus = 0

    fun read(address: Int): Int {
        val value = when (val a = address.low16Bits()) {
            in 0x0000..0x1FFF -> ram[a and 0x07FF].toUnsignedInt()
            in 0x2000..0x3FFF -> ppu.cpuRead(0x2000 + (a and 7))
            in 0x4000..0x4013 -> apu.cpuRead(a)
            0x4014 -> 0
            0x4015 -> apu.cpuRead(a)
            0x4016 -> controller.read()
            0x4017 -> 0
            in 0x4020..0xFFFF -> cartridgeSocket.cpuRead(a, openBus)
            else -> 0
        }
        openBus = value
        return value
    }

    fun write(address: Int, value: Int) {
        val a = address.low16Bits()
        val v = value.low8Bits()
        openBus = v
        when (a) {
            in 0x0000..0x1FFF -> ram[a and 0x07FF] = v.toByte()
            in 0x2000..0x3FFF -> ppu.cpuWrite(0x2000 + (a and 7), v)
            in 0x4000..0x4013 -> apu.cpuWrite(a, v)
            0x4014 -> doOamDma(v)
            0x4015 -> apu.cpuWrite(a, v)
            0x4016 -> controller.write(v)
            0x4017 -> apu.cpuWrite(a, v)
            in 0x4020..0xFFFF -> cartridgeSocket.cpuWrite(a, v)
        }
    }

    fun consumeDmaCycles(): Int {
        return cpuStall.drain()
    }

    fun reset() {
        cpuStall.reset()
        openBus = 0
    }

    private fun doOamDma(page: Int) {
        val base = page shl 8
        var i = 0
        while (i < 256) {
            oamDmaBuffer[i] = read(base + i).toByte()
            i++
        }
        ppu.writeOamDma(oamDmaBuffer)
        cpuStall.request(513)
    }
}
