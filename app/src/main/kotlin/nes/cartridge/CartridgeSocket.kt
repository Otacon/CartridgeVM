package nes.cartridge

import di.AppScope
import me.tatarka.inject.annotations.Inject

@Inject
@AppScope
class CartridgeSocket {
    private var cartridge: Cartridge? = null

    var mirroring: Mirroring? = null
        private set

    fun insert(cartridge: Cartridge) {
        this.cartridge = cartridge
        mirroring = cartridge.mapper.mirroring() ?: cartridge.mirroring
    }

    fun remove() {
        cartridge = null
        mirroring = null
    }

    fun reset() {
        val inserted = cartridge ?: return
        inserted.mapper.reset()
        mirroring = inserted.mapper.mirroring() ?: inserted.mirroring
    }

    fun cpuRead(address: Int): Int {
        return cartridge?.mapper?.cpuRead(address) ?: 0
    }

    fun cpuWrite(address: Int, value: Int) {
        val inserted = cartridge ?: return
        inserted.mapper.cpuWrite(address, value)
        mirroring = inserted.mapper.mirroring() ?: inserted.mirroring
    }

    fun ppuRead(address: Int): Int {
        return cartridge?.mapper?.ppuRead(address) ?: 0
    }

    fun ppuWrite(address: Int, value: Int) {
        cartridge?.mapper?.ppuWrite(address, value)
    }

    fun clockScanline() {
        cartridge?.mapper?.clockScanline()
    }

    fun irqPending(): Boolean {
        return cartridge?.mapper?.irqPending() ?: false
    }
}
