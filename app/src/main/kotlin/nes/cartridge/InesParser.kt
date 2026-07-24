package nes.cartridge

import me.tatarka.inject.annotations.Inject
import org.slf4j.LoggerFactory
import java.nio.file.Path
import kotlin.io.path.readBytes

@Inject
class InesParser {

    private val log = LoggerFactory.getLogger("InesParser")

    fun parse(file: Path): Cartridge {
        log.debug("Opening {}", file)
        return parse(file.readBytes())
    }

    fun parse(bytes: ByteArray): Cartridge {
        if (bytes.size < HEADER_SIZE) {
            log.error("Truncated ROM: missing iNES header")
            throw RomFormatException("Truncated ROM: missing iNES header")
        }
        if (bytes[0] != 'N'.code.toByte() || bytes[1] != 'E'.code.toByte() || bytes[2] != 'S'.code.toByte() || bytes[3] != 0x1A.toByte()) {
            log.error("Invalid iNES header: expected NES<EOF> magic bytes")
            throw RomFormatException("Invalid iNES header: expected NES<EOF> magic bytes")
        }
        val prgBanks = bytes[4].toInt() and 0xFF
        val chrBanks = bytes[5].toInt() and 0xFF
        val flags6 = bytes[6].toInt() and 0xFF
        val flags7 = bytes[7].toInt() and 0xFF
        if ((flags7 and 0x0C) == 0x08) {
            log.error("Unsupported NES 2.0 ROM")
            throw RomFormatException("Unsupported NES 2.0 ROM")
        }
        if ((flags6 and 0x08) != 0) {
            log.error("Unsupported mirroring mode: four-screen mirroring")
            throw RomFormatException("Unsupported mirroring mode: four-screen mirroring")
        }
        val mapper = (flags6 shr 4) or (flags7 and 0xF0)
        log.debug("Mapper: {}", mapper)
        validateMapperSizes(mapper, prgBanks, chrBanks)

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
            log.error("Truncated ROM: expected at least $required bytes, found ${bytes.size}")
            throw RomFormatException("Truncated ROM: expected at least $required bytes, found ${bytes.size}")
        }
        val prg = bytes.copyOfRange(offset, offset + prgSize)
        log.debug("PRG ROM: {}KiB", prg.size / 1024)
        offset += prgSize
        val isChrRam = chrBanks == 0
        val chr = if (isChrRam) {
            log.debug("CHR: {} KiB RAM", chrSize / 1024)
            ByteArray(CHR_BANK_SIZE)
        } else {
            log.debug("CHR: {} KiB ROM", chrSize / 1024)
            bytes.copyOfRange(offset, offset + chrSize)
        }
        val mirroring = if ((flags6 and 0x01) != 0) {
            log.debug("Mirroring: Vertical")
            Mirroring.VERTICAL
        } else {
            log.debug("Mirroring: Horizontal")
            Mirroring.HORIZONTAL
        }
        return Cartridge(
            mirroring = mirroring,
            prgRom = prg,
            chr = chr,
            isChrRam = isChrRam,
            trainerPresent = trainer,
            mapper = createMapper(mapper, prg, chr, isChrRam)
        )
    }

    private fun validateMapperSizes(mapper: Int, prgBanks: Int, chrBanks: Int) {
        when (mapper) {
            0 -> {
                if (prgBanks != 1 && prgBanks != 2) {
                    log.error("Invalid PRG ROM size for Mapper 0: ${prgBanks * 16} KiB")
                    throw RomFormatException("Invalid PRG ROM size for Mapper 0: ${prgBanks * 16} KiB")
                }
                if (chrBanks > 1) {
                    log.error("Invalid CHR ROM size for Mapper 0: ${chrBanks * 8} KiB")
                    throw RomFormatException("Invalid CHR ROM size for Mapper 0: ${chrBanks * 8} KiB")
                }
            }
            2 -> {
                if (prgBanks < 2 || prgBanks > 16) {
                    log.error("Invalid PRG ROM size for Mapper 2: ${prgBanks * 16} KiB")
                    throw RomFormatException("Invalid PRG ROM size for Mapper 2: ${prgBanks * 16} KiB")
                }
                if (chrBanks != 0) {
                    log.error("Invalid CHR ROM size for Mapper 2: ${chrBanks * 8} KiB; UxROM uses CHR RAM")
                    throw RomFormatException("Invalid CHR ROM size for Mapper 2: ${chrBanks * 8} KiB; UxROM uses CHR RAM")
                }
            }
            3 -> {
                if (prgBanks != 1 && prgBanks != 2) {
                    log.error("Invalid PRG ROM size for Mapper 3: ${prgBanks * 16} KiB")
                    throw RomFormatException("Invalid PRG ROM size for Mapper 3: ${prgBanks * 16} KiB")
                }
                if (chrBanks < 1 || chrBanks > 4) {
                    log.error("Invalid CHR ROM size for Mapper 3: ${chrBanks * 8} KiB")
                    throw RomFormatException("Invalid CHR ROM size for Mapper 3: ${chrBanks * 8} KiB")
                }
            }
            4 -> {
                if (prgBanks < 2 || prgBanks > 32) {
                    log.error("Invalid PRG ROM size for Mapper 4: ${prgBanks * 16} KiB")
                    throw RomFormatException("Invalid PRG ROM size for Mapper 4: ${prgBanks * 16} KiB")
                }
                if (chrBanks > 32) {
                    log.error("Invalid CHR ROM size for Mapper 4: ${chrBanks * 8} KiB")
                    throw RomFormatException("Invalid CHR ROM size for Mapper 4: ${chrBanks * 8} KiB")
                }
            }
            else -> {
                log.error("Unsupported mapper $mapper; only Mapper 0 / NROM, Mapper 2 / UxROM, Mapper 3 / CNROM, and Mapper 4 / MMC3 are supported")
                throw RomFormatException("Unsupported mapper $mapper; only Mapper 0 / NROM, Mapper 2 / UxROM, Mapper 3 / CNROM, and Mapper 4 / MMC3 are supported")
            }
        }
    }

    private fun createMapper(mapper: Int, prg: ByteArray, chr: ByteArray, isChrRam: Boolean): Mapper {
        return when (mapper) {
            0 -> Mapper0(prgRom = prg, chr = chr, isChrRam = isChrRam)
            2 -> Mapper2(prgRom = prg, chrRam = chr)
            3 -> Mapper3(prgRom = prg, chrRom = chr)
            4 -> Mapper4(prgRom = prg, chr = chr, isChrRam = isChrRam)
            else -> error("Unsupported mapper $mapper")
        }
    }

    companion object {
        private const val HEADER_SIZE = 16
        private const val TRAINER_SIZE = 512
        private const val PRG_BANK_SIZE = 16 * 1024
        private const val CHR_BANK_SIZE = 8 * 1024
    }
}
