package nes.cartridge

enum class Mirroring { HORIZONTAL, VERTICAL }

class RomFormatException(message: String) : IllegalArgumentException(message)

data class Cartridge(
    val mapperNumber: Int,
    val mirroring: Mirroring,
    val prgRom: ByteArray,
    val chr: ByteArray,
    val chrRam: Boolean,
    val trainerPresent: Boolean,
) {
    init {
        require(mapperNumber == 0) { "Only Mapper 0 cartridges can be constructed" }
    }
}
