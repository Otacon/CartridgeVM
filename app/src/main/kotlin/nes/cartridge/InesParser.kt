package nes.cartridge

object InesParser {
    private const val HEADER_SIZE = 16
    private const val TRAINER_SIZE = 512
    private const val PRG_BANK_SIZE = 16 * 1024
    private const val CHR_BANK_SIZE = 8 * 1024

    fun parse(bytes: ByteArray): Cartridge {
        if (bytes.size < HEADER_SIZE) {
            throw RomFormatException("Truncated ROM: missing iNES header")
        }
        if (bytes[0] != 'N'.code.toByte() || bytes[1] != 'E'.code.toByte() || bytes[2] != 'S'.code.toByte() || bytes[3] != 0x1A.toByte()) {
            throw RomFormatException("Invalid iNES header: expected NES<EOF> magic bytes")
        }
        val prgBanks = bytes[4].toInt() and 0xFF
        val chrBanks = bytes[5].toInt() and 0xFF
        val flags6 = bytes[6].toInt() and 0xFF
        val flags7 = bytes[7].toInt() and 0xFF
        if ((flags7 and 0x0C) == 0x08) {
            throw RomFormatException("Unsupported NES 2.0 ROM")
        }
        if ((flags6 and 0x08) != 0) {
            throw RomFormatException("Unsupported mirroring mode: four-screen mirroring")
        }
        val mapper = (flags6 shr 4) or (flags7 and 0xF0)
        if (mapper != 0) {
            throw RomFormatException("Unsupported mapper $mapper; only Mapper 0 / NROM is supported")
        }
        if (prgBanks != 1 && prgBanks != 2) {
            throw RomFormatException("Invalid PRG ROM size for Mapper 0: ${prgBanks * 16} KiB")
        }
        if (chrBanks > 1) {
            throw RomFormatException("Invalid CHR ROM size for Mapper 0: ${chrBanks * 8} KiB")
        }

        val trainer = (flags6 and 0x04) != 0
        var offset = HEADER_SIZE + if (trainer) {
            TRAINER_SIZE
        } else {
            0
        }
        val prgSize = prgBanks * PRG_BANK_SIZE
        val chrSize = chrBanks * CHR_BANK_SIZE
        val required = offset + prgSize + chrSize
        if (bytes.size < required) {
            throw RomFormatException("Truncated ROM: expected at least $required bytes, found ${bytes.size}")
        }
        val prg = bytes.copyOfRange(offset, offset + prgSize)
        offset += prgSize
        val chrRam = chrBanks == 0
        val chr = if (chrRam) {
            ByteArray(CHR_BANK_SIZE)
        } else {
            bytes.copyOfRange(offset, offset + chrSize)
        }
        val mirroring = if ((flags6 and 0x01) != 0) {
            Mirroring.VERTICAL
        } else {
            Mirroring.HORIZONTAL
        }
        return Cartridge(mapper, mirroring, prg, chr, chrRam, trainer)
    }
}
