package nes.cartridge

import di.AppScope
import me.tatarka.inject.annotations.Inject

@Inject
@AppScope
class CartridgeSocket {
    private var mapper: Mapper? = null
    private var cartridgeMirroring: Mirroring? = null
    private var effectiveMirroring: Mirroring? = null

    val mirroring: Mirroring?
        get() = effectiveMirroring

    fun insert(cartridge: Cartridge) {
        mapper = cartridge.mapper
        cartridgeMirroring = cartridge.mirroring
        updateMirroring()
    }

    fun remove() {
        mapper = null
        cartridgeMirroring = null
        effectiveMirroring = null
    }

    fun reset() {
        mapper?.reset()
        updateMirroring()
    }

    fun cpuRead(address: Int): Int {
        return mapper?.cpuRead(address) ?: 0
    }

    fun cpuWrite(address: Int, value: Int) {
        mapper?.cpuWrite(address, value)
        updateMirroring()
    }

    fun ppuRead(address: Int): Int {
        return mapper?.ppuRead(address) ?: 0
    }

    fun ppuWrite(address: Int, value: Int) {
        mapper?.ppuWrite(address, value)
    }

    fun clockScanline() {
        mapper?.clockScanline()
    }

    fun irqPending(): Boolean {
        return mapper?.irqPending() ?: false
    }

    private fun updateMirroring() {
        effectiveMirroring = mapper?.mirroring() ?: cartridgeMirroring
    }
}
