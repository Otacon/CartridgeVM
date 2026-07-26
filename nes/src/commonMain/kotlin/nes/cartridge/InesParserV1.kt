package nes.cartridge

import co.touchlab.kermit.Logger
import nes.util.toUnsignedInt

class InesParserV1(
    private val utils: InesParserUtils,
) : InesParser {
    private val log = Logger.withTag("InesParserV1")

    override fun parse(bytes: ByteArray): Cartridge {
        utils.validateHeader(bytes)
        if (utils.isNes2(bytes)) {
            throw RomFormatException("Expected iNES 1.0 ROM, found NES 2.0 header")
        }
        val prgBanks = bytes[4].toUnsignedInt()
        val chrBanks = bytes[5].toUnsignedInt()
        val flags6 = bytes[6].toUnsignedInt()
        val flags7 = bytes[7].toUnsignedInt()
        if ((flags6 and 0x08) != 0) {
            log.e { "Unsupported mirroring mode: four-screen mirroring" }
            throw RomFormatException("Unsupported mirroring mode: four-screen mirroring")
        }

        val mapper = (flags6 shr 4) or (flags7 and 0xF0)
        val prgRomSize = prgBanks.toLong() * InesParserUtils.PRG_BANK_SIZE
        val chrRomSize = chrBanks.toLong() * InesParserUtils.CHR_BANK_SIZE
        val chrRamSize = if (chrRomSize == 0L) InesParserUtils.CHR_BANK_SIZE else 0
        log.d { "Mapper: $mapper" }

        utils.validateMapperSizes(
            mapper = mapper,
            submapper = 0,
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
}
