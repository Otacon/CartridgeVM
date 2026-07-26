package frontend

import kotlin.test.Test
import kotlin.test.assertEquals

class MainMenuTest {
    @Test
    fun exposesSharedFileMenu() {
        assertEquals("File", emulatorMainMenu.label)
        assertEquals(
            listOf(MenuAction.OpenRom, MenuAction.Exit),
            emulatorMainMenu.entries.filterIsInstance<MenuEntry.Item>().map { it.action },
        )
    }
}
