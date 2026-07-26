package nes

import nes.apu.NesApu
import nes.cartridge.Cartridge
import nes.cartridge.CartridgeSocket
import nes.cpu.Cpu6502
import nes.input.NesController
import nes.ppu.Ppu

class NesMachine(
    val controller: NesController,
    val cartridgeSocket: CartridgeSocket,
    val ppu: Ppu,
    val apu: NesApu,
    val cpu: Cpu6502,
) {

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

    fun runUntilFrame(onInputPoll: (() -> Unit)? = null) {
        ppu.clearFrameComplete()
        apu.beginFrame()
        var cyclesUntilInputPoll = CPU_CYCLES_PER_INPUT_POLL
        while (!ppu.frameComplete) {
            latchInterrupts()
            val cycles = cpu.step()
            apu.step(cycles)
            cyclesUntilInputPoll -= cycles
            if (onInputPoll != null && cyclesUntilInputPoll <= 0) {
                onInputPoll()
                cyclesUntilInputPoll += CPU_CYCLES_PER_INPUT_POLL
            }
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

    companion object {
        private const val INPUT_POLLS_PER_SECOND = 500
        private const val CPU_CYCLES_PER_INPUT_POLL = Timing.CPU_HZ / INPUT_POLLS_PER_SECOND
    }
}
