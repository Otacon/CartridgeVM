package nes.di

import me.tatarka.inject.annotations.Component
import me.tatarka.inject.annotations.Provides
import me.tatarka.inject.annotations.Scope
import nes.NesMachine
import nes.apu.DmcDma
import nes.apu.NesApu
import nes.cartridge.CartridgeSocket
import nes.cpu.Cpu6502
import nes.cpu.CpuBus
import nes.cpu.CpuStall
import nes.input.NesController
import nes.ppu.Ppu
import nes.ppu.PpuBus

@NesScope
@Component
abstract class NesComponent {
    abstract val nesMachine: NesMachine

    @NesScope
    @Provides
    fun cartridgeSocket(): CartridgeSocket = CartridgeSocket()

    @NesScope
    @Provides
    fun nesController(): NesController = NesController()

    @NesScope
    @Provides
    fun cpuStall(): CpuStall = CpuStall()

    @NesScope
    @Provides
    fun dmcDma(cartridgeSocket: CartridgeSocket, cpuStall: CpuStall): DmcDma = DmcDma(cartridgeSocket, cpuStall)

    @NesScope
    @Provides
    fun nesApu(dmcDma: DmcDma): NesApu = NesApu(dmcDma)

    @NesScope
    @Provides
    fun ppuBus(cartridgeSocket: CartridgeSocket): PpuBus = PpuBus(cartridgeSocket)

    @NesScope
    @Provides
    fun ppu(ppuBus: PpuBus): Ppu = Ppu(ppuBus)

    @NesScope
    @Provides
    fun cpuBus(
        cartridgeSocket: CartridgeSocket,
        ppu: Ppu,
        controller: NesController,
        apu: NesApu,
        cpuStall: CpuStall,
    ): CpuBus = CpuBus(cartridgeSocket, ppu, controller, apu, cpuStall)

    @NesScope
    @Provides
    fun cpu6502(cpuBus: CpuBus): Cpu6502 = Cpu6502(cpuBus)

    @NesScope
    @Provides
    fun nesMachine(
        controller: NesController,
        cartridgeSocket: CartridgeSocket,
        ppu: Ppu,
        apu: NesApu,
        cpu: Cpu6502,
    ): NesMachine = NesMachine(controller, cartridgeSocket, ppu, apu, cpu)
}

@Scope
annotation class NesScope
