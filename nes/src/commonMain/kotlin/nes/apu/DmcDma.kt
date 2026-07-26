package nes.apu

import nes.cartridge.CartridgeSocket
import nes.cpu.CpuStall

class DmcDma(
    private val cartridgeSocket: CartridgeSocket,
    private val cpuStall: CpuStall,
) {
    fun read(address: Int): Int {
        cpuStall.request(4)
        return cartridgeSocket.cpuRead(address)
    }
}
