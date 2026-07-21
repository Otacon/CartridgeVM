import nes.cartridge.Cartridge
import nes.cartridge.Mirroring
import nes.cartridge.Mapper0
import nes.apu.NesApu
import nes.cpu.Cpu6502
import nes.cpu.CpuBus
import nes.input.NesController
import nes.ppu.Ppu

fun ines(prgBanks: Int = 1, chrBanks: Int = 1, flags6: Int = 0, trainer: Boolean = false, prgFill: Int = 0): ByteArray {
    val header = ByteArray(16)
    header[0] = 'N'.code.toByte(); header[1] = 'E'.code.toByte(); header[2] = 'S'.code.toByte(); header[3] = 0x1A
    header[4] = prgBanks.toByte(); header[5] = chrBanks.toByte(); header[6] = (flags6 or if (trainer) 4 else 0).toByte()
    val trainerBytes = if (trainer) ByteArray(512) { 0x55 } else ByteArray(0)
    val prg = ByteArray(prgBanks * 16 * 1024) { prgFill.toByte() }
    val chr = ByteArray(chrBanks * 8 * 1024)
    return header + trainerBytes + prg + chr
}

fun cpuWithProgram(program: ByteArray, start: Int = 0x8000): Triple<Cpu6502, CpuBus, Ppu> {
    val prg = ByteArray(16 * 1024)
    System.arraycopy(program, 0, prg, start - 0x8000, program.size)
    val vector = 0x3FFC
    prg[vector] = (start and 0xFF).toByte(); prg[vector + 1] = (start shr 8).toByte()
    prg[0x3FFA] = 0x00; prg[0x3FFB] = 0x90.toByte()
    prg[0x3FFE] = 0x00; prg[0x3FFF] = 0x91.toByte()
    prg[0x1000] = 0xEA.toByte()
    prg[0x1100] = 0x40.toByte()
    val cart = Cartridge(0, Mirroring.HORIZONTAL, prg, ByteArray(8192), true, false)
    val mapper = Mapper0(cart)
    val ppu = Ppu(mapper, cart.mirroring)
    val bus = CpuBus(mapper, ppu, NesController(), NesApu())
    val cpu = Cpu6502(bus)
    cpu.reset()
    return Triple(cpu, bus, ppu)
}
