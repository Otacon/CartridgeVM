package nes

import di.AppScope
import me.tatarka.inject.annotations.Inject
import nes.apu.NesApu
import nes.cartridge.Cartridge
import nes.cartridge.CartridgeSocket
import nes.cpu.Cpu6502
import nes.input.NesController
import nes.ppu.Ppu

@Inject
@AppScope
class NesMachine(
    val controller: NesController,
    val cartridgeSocket: CartridgeSocket,
    val ppu: Ppu,
    val apu: NesApu,
    val cpu: Cpu6502,
) {

    init {
        reset()
    }

    fun insert(cartridge: Cartridge) {
        cartridgeSocket.insert(cartridge)
    }

    fun reset() {
        cartridgeSocket.reset()
        controller.reset()
        ppu.reset()
        apu.reset()
        cpu.reset()
    }

    fun runUntilFrame() {
        ppu.clearFrameComplete()
        apu.beginFrame()
        while (!ppu.frameComplete) {
            latchInterrupts()
            val cycles = cpu.step()
            apu.step(cycles)
            var i = 0
            val ppuCycles = cycles * Timing.PPU_PER_CPU
            while (i < ppuCycles) {
                ppu.step()
                i++
            }
        }
    }

    private fun latchInterrupts() {
        if (ppu.pollNmi()) cpu.requestNmi()
        cpu.setIrqLine(cartridgeSocket.irqPending() || apu.irqPending())
    }
}
