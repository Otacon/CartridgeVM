package nes.cartridge

interface Mapper {
    fun cpuRead(address: Int): Int
    fun cpuWrite(address: Int, value: Int)
    fun ppuRead(address: Int): Int
    fun ppuWrite(address: Int, value: Int)
}
