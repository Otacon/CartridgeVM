package frontend

import nes.cartridge.RomData

data class Config(
    val debug: Boolean = false,
    val rom: RomData? = null,
)
