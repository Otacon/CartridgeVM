package nes.cartridge

class CartridgeSocket {
    private var cartridge: Cartridge? = null

    val mirroring: Mirroring?
        get() = cartridge?.mirroring

    fun insert(cartridge: Cartridge) {
        this.cartridge = cartridge
    }

    fun remove() {
        cartridge = null
    }

    fun cpuRead(address: Int): Int {
        return cartridge?.mapper?.cpuRead(address) ?: 0
    }

    fun cpuWrite(address: Int, value: Int) {
        cartridge?.mapper?.cpuWrite(address, value)
    }

    fun ppuRead(address: Int): Int {
        return cartridge?.mapper?.ppuRead(address) ?: 0
    }

    fun ppuWrite(address: Int, value: Int) {
        cartridge?.mapper?.ppuWrite(address, value)
    }
}
