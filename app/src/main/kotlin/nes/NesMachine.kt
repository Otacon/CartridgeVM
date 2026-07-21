package nes

import nes.cartridge.Cartridge
import nes.cartridge.Mapper0
import nes.cpu.Cpu6502
import nes.cpu.CpuBus
import nes.input.NesController
import nes.ppu.Ppu

class NesMachine(private val cartridge: Cartridge) {
    val controller = NesController()
    private val mapper = Mapper0(cartridge)
    val ppu = Ppu(mapper, cartridge.mirroring)
    val bus = CpuBus(mapper, ppu, controller)
    val cpu = Cpu6502(bus)

    init { reset() }

    fun reset() {
        ppu.reset()
        cpu.reset()
    }

    fun runUntilFrame() {
        ppu.clearFrameComplete()
        while (!ppu.frameComplete) {
            if (ppu.pollNmi()) cpu.requestNmi()
            val cycles = cpu.step()
            var i = 0
            val ppuCycles = cycles * Timing.PPU_PER_CPU
            while (i < ppuCycles) {
                ppu.step()
                if (ppu.pollNmi()) cpu.requestNmi()
                i++
            }
        }
    }
}
