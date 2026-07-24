package nes.cartridge

import me.tatarka.inject.annotations.Inject
import nes.util.toUnsignedInt
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Path
import kotlin.io.path.readBytes

@Inject
class InesParserV1(
    private val utils: InesParserUtils,
) : InesParser {
    private val log = LoggerFactory.getLogger("InesParserV1")

    override fun parse(file: File): Cartridge = parse(file.toPath())

    override fun parse(path: Path): Cartridge {
        log.debug("Opening {}", path)
        return parse(path.readBytes())
    }

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
            log.error("Unsupported mirroring mode: four-screen mirroring")
            throw RomFormatException("Unsupported mirroring mode: four-screen mirroring")
        }

        val mapper = (flags6 shr 4) or (flags7 and 0xF0)
        val prgRomSize = prgBanks.toLong() * InesParserUtils.PRG_BANK_SIZE
        val chrRomSize = chrBanks.toLong() * InesParserUtils.CHR_BANK_SIZE
        val chrRamSize = if (chrRomSize == 0L) InesParserUtils.CHR_BANK_SIZE else 0
        log.debug("Mapper: {}", mapper)

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
