package nes.cartridge

enum class Mirroring { HORIZONTAL, VERTICAL }

class RomFormatException(message: String) : IllegalArgumentException(message)

class Cartridge(
    val mapperNumber: Int,
    val mirroring: Mirroring,
    val prgRom: ByteArray,
    val chr: ByteArray,
    val isChrRam: Boolean,
    val trainerPresent: Boolean,
) {
    val mapper: Mapper

    init {
        require(mapperNumber == 0) { "Only Mapper 0 cartridges can be constructed" }
        this.mapper = Mapper0(prgRom = prgRom, chr = chr, isChrRam = isChrRam)
    }
}
