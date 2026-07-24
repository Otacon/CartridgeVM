package nes.cartridge

import me.tatarka.inject.annotations.Inject
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Path
import kotlin.io.path.readBytes

@Inject
class InesParserComposite(
    private val inesParserV1: InesParserV1,
    private val inesParserV2: InesParserV2,
    private val utils: InesParserUtils,
) : InesParser {
    private val log = LoggerFactory.getLogger("InesParserComposite")

    override fun parse(file: File): Cartridge = parse(file.toPath())

    override fun parse(path: Path): Cartridge {
        log.debug("Opening {}", path)
        return parse(path.readBytes())
    }

    override fun parse(bytes: ByteArray): Cartridge {
        utils.validateHeader(bytes)
        return if (utils.isNes2(bytes)) {
            log.debug("ROM format: NES 2.0")
            inesParserV2.parse(bytes)
        } else {
            log.debug("ROM format: iNES 1.0")
            inesParserV1.parse(bytes)
        }
    }
}
