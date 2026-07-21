package nes.cartridge

class Mapper0(private val cartridge: Cartridge) : Mapper {
    override fun cpuRead(address: Int): Int {
        if (address < 0x8000) return 0
        val index = if (cartridge.prgRom.size == 0x4000) address and 0x3FFF else address and 0x7FFF
        return cartridge.prgRom[index].toInt() and 0xFF
    }

    override fun cpuWrite(address: Int, value: Int) = Unit

    override fun ppuRead(address: Int): Int = cartridge.chr[address and 0x1FFF].toInt() and 0xFF

    override fun ppuWrite(address: Int, value: Int) {
        if (cartridge.chrRam) cartridge.chr[address and 0x1FFF] = value.toByte()
    }
}
