package nes.apu

import di.AppScope
import me.tatarka.inject.annotations.Inject
import nes.cartridge.CartridgeSocket

@Inject
@AppScope
class DmcDma(
    private val cartridgeSocket: CartridgeSocket,
) {
    fun read(address: Int): Int {
        return cartridgeSocket.cpuRead(address)
    }
}
