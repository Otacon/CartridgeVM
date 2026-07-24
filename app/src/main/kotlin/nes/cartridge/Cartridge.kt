package nes.cartridge

enum class Mirroring { HORIZONTAL, VERTICAL }

class RomFormatException(message: String) : IllegalArgumentException(message)

data class Cartridge(
    val mirroring: Mirroring,
    val prgRom: ByteArray,
    val chr: ByteArray,
    val isChrRam: Boolean,
    val trainerPresent: Boolean,
    val mapper: Mapper,
)
