package nes.cartridge

interface InesParser {
    fun parse(bytes: ByteArray): Cartridge
}
