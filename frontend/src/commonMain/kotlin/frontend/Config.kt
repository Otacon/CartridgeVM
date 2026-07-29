package frontend

data class Config(
    val debug: Boolean = false,
    val unlimited: Boolean = false,
    val controller: Boolean = false,
    val crt: Boolean = false,
    val rom: RomData? = null,
)