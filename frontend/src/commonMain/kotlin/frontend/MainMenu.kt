package frontend

enum class MenuAction {
    OpenRom,
    Exit,
}

sealed interface MenuEntry {
    data class Item(
        val label: String,
        val action: MenuAction,
    ) : MenuEntry

    data object Separator : MenuEntry
}

data class MenuDefinition(
    val label: String,
    val entries: List<MenuEntry>,
)

val emulatorMainMenu = MenuDefinition(
    label = "File",
    entries = listOf(
        MenuEntry.Item("Open ROM...", MenuAction.OpenRom),
        MenuEntry.Separator,
        MenuEntry.Item("Exit", MenuAction.Exit),
    ),
)

class EmulatorApplicationState(
    private val romLoader: RomLoader,
) {
    val currentRomName: String?
        get() = romLoader.currentRomName

    fun loadRom(rom: RomData): Boolean = romLoader.load(rom)

    suspend fun performMenuAction(
        action: MenuAction,
        romPicker: RomPicker,
        onExit: () -> Unit,
    ): Boolean = when (action) {
        MenuAction.OpenRom -> romPicker.pickRom()?.let(::loadRom) ?: false
        MenuAction.Exit -> {
            onExit()
            false
        }
    }
}
