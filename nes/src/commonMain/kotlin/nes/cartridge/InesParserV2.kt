package nes.cartridge

import co.touchlab.kermit.Logger
import nes.util.toUnsignedInt

class InesParserV2(
    private val utils: InesParserUtils,
) : InesParser {
    private val log = Logger.withTag("InesParserV2")

    override fun parse(bytes: ByteArray): Cartridge {
        utils.validateHeader(bytes)
        if (!utils.isNes2(bytes)) {
            throw RomFormatException("Expected NES 2.0 ROM, found iNES 1.0 header")
        }
        val prgLsb = bytes[4].toUnsignedInt()
        val chrLsb = bytes[5].toUnsignedInt()
        val flags6 = bytes[6].toUnsignedInt()
        val flags7 = bytes[7].toUnsignedInt()
        if ((flags6 and 0x08) != 0) {
            log.e { "Unsupported mirroring mode: four-screen mirroring" }
            throw RomFormatException("Unsupported mirroring mode: four-screen mirroring")
        }
        validateNes2Header(bytes, flags7)

        val mapper = (flags6 shr 4) or (flags7 and 0xF0) or ((bytes[8].toUnsignedInt() and 0x0F) shl 8)
        val submapper = bytes[8].toUnsignedInt() shr 4
        val sizeMsb = bytes[9].toUnsignedInt()
        val prgRomSize = decodeRomSize(prgLsb, sizeMsb and 0x0F, InesParserUtils.PRG_BANK_SIZE, "PRG ROM")
        val chrRomSize = decodeRomSize(chrLsb, sizeMsb shr 4, InesParserUtils.CHR_BANK_SIZE, "CHR ROM")
        val ramSizes = bytes[11].toUnsignedInt()
        val chrRamSize = decodeRamSize(ramSizes and 0x0F) + decodeRamSize(ramSizes shr 4)
        if (chrRomSize != 0L && chrRamSize != 0) {
            throw RomFormatException("Unsupported NES 2.0 ROM with both CHR ROM and CHR RAM")
        }
        log.d { "Mapper: $mapper, submapper $submapper" }

        utils.validateMapperSizes(
            mapper = mapper,
            submapper = submapper,
            prgSize = prgRomSize,
            chrRomSize = chrRomSize,
            chrRamSize = chrRamSize,
        )
        return utils.createCartridge(
            bytes = bytes,
            flags6 = flags6,
            mapper = mapper,
            prgRomSize = prgRomSize,
            chrRomSize = chrRomSize,
            chrRamSize = chrRamSize,
        )
    }

    private fun validateNes2Header(bytes: ByteArray, flags7: Int) {
        val consoleType = flags7 and 0x03
        if (consoleType != 0) {
            throw RomFormatException("Unsupported NES 2.0 console type: $consoleType; only standard NES/Famicom ROMs are supported")
        }
        val timingMode = bytes[12].toUnsignedInt() and 0x03
        if (timingMode == 1 || timingMode == 3) {
            val name = if (timingMode == 1) "PAL" else "Dendy"
            throw RomFormatException("Unsupported NES 2.0 timing mode: $name; only NTSC-compatible ROMs are supported")
        }
        val miscRomCount = bytes[14].toUnsignedInt() and 0x03
        if (miscRomCount != 0) {
            throw RomFormatException("Unsupported NES 2.0 ROM with $miscRomCount miscellaneous ROM area(s)")
        }
    }

    private fun decodeRomSize(lsb: Int, msb: Int, unit: Int, name: String): Long {
        if (msb != 0x0F) return (((msb shl 8) or lsb).toLong()) * unit

        val exponent = lsb shr 2
        val multiplier = (lsb and 0x03) * 2 + 1
        if (exponent >= Long.SIZE_BITS - 1) {
            throw RomFormatException("Unsupported $name size: exponent $exponent is too large")
        }
        val base = 1L shl exponent
        if (base > Long.MAX_VALUE / multiplier) {
            throw RomFormatException("Unsupported $name size: encoded size is too large")
        }
        return base * multiplier
    }

    private fun decodeRamSize(shift: Int): Int = if (shift == 0) 0 else 64 shl shift
}
