package frontend

import co.touchlab.kermit.Logger
import nes.NesMachine
import nes.cartridge.InesParserComposite
import nes.cartridge.RomFormatException

class RomLoader(
    private val parser: InesParserComposite,
    private val machine: NesMachine,
) {
    private val log = Logger.withTag("RomLoader")

    var currentRomName: String? = null
        private set

    fun load(rom: RomData): Boolean {
        return try {
            machine.insert(parser.parse(rom.bytes))
            machine.reset()
            currentRomName = rom.name
            log.i { "Loaded ROM: ${rom.name}" }
            true
        } catch (e: RomFormatException) {
            log.e(e) { "Unable to load ROM: ${rom.name}" }
            false
        }
    }
}
