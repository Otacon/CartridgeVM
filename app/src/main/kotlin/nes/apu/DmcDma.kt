package nes.apu

import di.AppScope
import me.tatarka.inject.annotations.Inject
import nes.cartridge.CartridgeSocket
import nes.cpu.CpuStall

@Inject
@AppScope
class DmcDma(
    private val cartridgeSocket: CartridgeSocket,
    private val cpuStall: CpuStall,
) {
    fun read(address: Int): Int {
        cpuStall.request(4)
        return cartridgeSocket.cpuRead(address)
    }
}
