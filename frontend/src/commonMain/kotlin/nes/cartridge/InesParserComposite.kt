package nes.cartridge

import co.touchlab.kermit.Logger
import io.Nes20Db

class InesParserComposite(
    private val inesParserV1: InesParserV1,
    private val inesParserV2: InesParserV2,
    private val nes20Db: Nes20Db,
    private val utils: InesParserUtils,
) : InesParser {
    private val log = Logger.withTag("InesParserComposite")

    override fun parse(romData: RomData): Cartridge {
        val bytes = romData.bytes
        utils.validateHeader(bytes)
        nes20Db.findBySha1("sha1")
        return if (utils.isNes2(bytes)) {
            log.d { "ROM format: NES 2.0" }
            inesParserV2.parse(romData)
        } else {
            log.d { "ROM format: iNES 1.0" }
            inesParserV1.parse(romData)
        }
    }
}
