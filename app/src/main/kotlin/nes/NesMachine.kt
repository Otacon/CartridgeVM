package nes

import nes.cartridge.Cartridge
import nes.cartridge.Mapper0
import nes.apu.NesApu
import nes.cpu.Cpu6502
import nes.cpu.CpuBus
import nes.input.NesController
import nes.ppu.Ppu

class NesMachine(private val cartridge: Cartridge) {
    val controller = NesController()
    private val mapper = Mapper0(cartridge)
    val ppu = Ppu(mapper, cartridge.mirroring)
    val apu = NesApu()
    val bus = CpuBus(mapper, ppu, controller, apu)
    val cpu = Cpu6502(bus)

    init { reset() }

    fun reset() {
        ppu.reset()
        apu.reset()
        cpu.reset()
    }

    fun runUntilFrame() {
        ppu.clearFrameComplete()
        apu.beginFrame()
        while (!ppu.frameComplete) {
            if (ppu.pollNmi()) cpu.requestNmi()
            val cycles = cpu.step()
            apu.step(cycles)
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
