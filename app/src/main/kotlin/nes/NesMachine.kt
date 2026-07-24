package nes

import nes.apu.NesApu
import nes.cartridge.Cartridge
import nes.cartridge.CartridgeSocket
import nes.cpu.Cpu6502
import nes.cpu.CpuBus
import nes.input.NesController
import nes.ppu.Ppu
import nes.ppu.PpuBus

class NesMachine {
    val controller = NesController()
    val cartridgeSocket = CartridgeSocket()
    val ppuBus = PpuBus(cartridgeSocket)
    val ppu = Ppu(ppuBus)
    val apu = NesApu()
    val bus = CpuBus(cartridgeSocket, ppu, controller, apu)
    val cpu = Cpu6502(bus)

    init {
        reset()
    }

    fun insert(cartridge: Cartridge) {
        cartridgeSocket.insert(cartridge)
    }

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
