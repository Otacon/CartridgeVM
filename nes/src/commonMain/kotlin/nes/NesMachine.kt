package nes

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    private val _isPoweredOn = MutableStateFlow(false)
    val isPoweredOn: StateFlow<Boolean> = _isPoweredOn.asStateFlow()
    val timing: Timing
        get() = cartridgeSocket.region.timing

    private var ppuCycleRemainder = 0

    fun powerOn() {
        reset()
        _isPoweredOn.value = true
    }

    fun powerOff() {
        _isPoweredOn.value = false
    }

    fun insert(cartridge: Cartridge) {
        cartridgeSocket.insert(cartridge)
        applyCartridgeTiming()
    }

    fun reset() {
        cartridgeSocket.reset()
        applyCartridgeTiming()
        controller.reset()
        ppu.reset()
        apu.reset()
        cpu.reset()
    }

    fun runUntilFrame(onInputPoll: (() -> Unit)? = null) {
        ppu.clearFrameComplete()
        apu.beginFrame()
        val cpuCyclesPerInputPoll = timing.cpuHz / INPUT_POLLS_PER_SECOND
        var cyclesUntilInputPoll = cpuCyclesPerInputPoll
        while (!ppu.frameComplete) {
            val cycles = cpu.step()
            apu.step(cycles)
            latchInterrupts()
            cyclesUntilInputPoll -= cycles
            if (onInputPoll != null && cyclesUntilInputPoll <= 0) {
                onInputPoll()
                cyclesUntilInputPoll += cpuCyclesPerInputPoll
            }
            var i = 0
            val ppuCyclesNumerator = cycles * timing.ppuCyclesPerCpuNumerator + ppuCycleRemainder
            val ppuCycles = ppuCyclesNumerator / timing.ppuCyclesPerCpuDenominator
            ppuCycleRemainder = ppuCyclesNumerator % timing.ppuCyclesPerCpuDenominator
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

    private fun applyCartridgeTiming() {
        val cartridgeTiming = timing
        ppu.timing = cartridgeTiming
        apu.timing = cartridgeTiming
        ppuCycleRemainder = 0
    }

    companion object {
        private const val INPUT_POLLS_PER_SECOND = 500
    }
}
