package nes.cpu

import di.AppScope
import me.tatarka.inject.annotations.Inject

@Inject
@AppScope
class CpuStall {
    private var pendingCycles = 0

    fun request(cycles: Int) {
        pendingCycles += cycles
    }

    fun drain(): Int {
        val cycles = pendingCycles
        pendingCycles = 0
        return cycles
    }

    fun reset() {
        pendingCycles = 0
    }
}
