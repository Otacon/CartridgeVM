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
        val normalizedSha1 = sha1.trim().lowercase()
        if(normalizedSha1.length != 40 || !normalizedSha1.all { it.isHexDigit() }) {
            log.e { "Invalid SHA1 provided: $sha1" }
            return null
        }

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

            if (columns.size < 5) {
                log.w { "Line $lineNumber contains ${columns.size} but rather than 5!" }
                continue
            }

            val entrySha1 = columns[0].trim().lowercase()
            val validEntrySha1 = entrySha1.length == 40 && entrySha1.all { it.isHexDigit() }
            if (!validEntrySha1) {
                log.w { "Line $lineNumber contains an invalid SHA-1: $entrySha1" }
                continue
            }
            if (normalizedSha1 == entrySha1) {
                return Nes20DbEntry(
                    sha1 = entrySha1,
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
