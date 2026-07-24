package nes.cartridge

import java.io.File
import java.nio.file.Path

interface InesParser {
    fun parse(file: File): Cartridge

    fun parse(path: Path): Cartridge

    fun parse(bytes: ByteArray): Cartridge
}
