package io

import co.touchlab.kermit.Logger
import nes.ConsoleRegion
import nes.cartridge.Mirroring

interface Nes20Db {
    fun findBySha1(sha1: String): Nes20DbEntry?
}

class Nes20DbCsv(
    private val csvResource: String,
) : Nes20Db {

    private val log = Logger.withTag("Nes20Db")

    override fun findBySha1(sha1: String): Nes20DbEntry? {
        val lines = readTextResource(csvResource)
            .lineSequence()
            .map(String::trim)
            .filter(String::isNotEmpty)
            .iterator()

        if (!lines.hasNext()) {
            log.e { "The Nes20Db file is empty!" }
            return null
        }

        val expectedHeader = "rom_sha1,console_region,pcb_mapper,pcb_submapper,pcb_mirroring"

        val actualHeader = lines.next().removePrefix("\uFEFF")

        if (actualHeader != expectedHeader) {
            log.e { "The Nes20Db file format is incorrect!" }
            return null
        }


        var lineNumber = 1

        while (lines.hasNext()) {
            lineNumber++

            val columns = lines.next().split(',')

            require(columns.size == 5) {
                "Invalid column count at line $lineNumber"
            }

            val sha1 = columns[0].trim().lowercase()

            require(sha1.length == 40 && sha1.all { it.isHexDigit() }) {
                "Invalid SHA-1 at line $lineNumber: $sha1"
            }
            if (sha1 == sha1) {
                return Nes20DbEntry(
                    sha1 = sha1,
                    region = parseRegion(columns[1], lineNumber),
                    mapper = columns[2].toIntOrNull() ?: error("Invalid mapper at line $lineNumber"),
                    submapper = columns[3].toIntOrNull() ?: error("Invalid submapper at line $lineNumber"),
                    mirroring = parseMirroring(columns[4], lineNumber),
                )
            }
        }
        return null
    }

    private fun parseRegion(value: String, lineNumber: Int): ConsoleRegion = when (value.trim().toIntOrNull()) {
        0 -> ConsoleRegion.NTSC
        1 -> ConsoleRegion.PAL
        2 -> ConsoleRegion.MULTI_REGION
        3 -> ConsoleRegion.DENDY
        else -> error(
            "Invalid console region at line $lineNumber: $value"
        )
    }

    private fun parseMirroring(value: String, lineNumber: Int): Mirroring = when (value.trim().uppercase()) {
        "H" -> Mirroring.HORIZONTAL
        "V" -> Mirroring.VERTICAL
        "4" -> Mirroring.SINGLE_SCREEN_LOWER
        "M" -> Mirroring.SINGLE_SCREEN_UPPER
        else -> error(
            "Invalid mirroring at line $lineNumber: $value"
        )
    }

    private fun Char.isHexDigit(): Boolean =
        this in '0'..'9' ||
                this in 'a'..'f' ||
                this in 'A'..'F'

}

data class Nes20DbEntry(
    val sha1: String,
    val region: ConsoleRegion,
    val mapper: Int,
    val submapper: Int,
    val mirroring: Mirroring,
)