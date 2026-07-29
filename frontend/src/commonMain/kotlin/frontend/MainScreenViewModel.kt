package frontend

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import me.tatarka.inject.annotations.Inject
import nes.NesMachine
import nes.cartridge.InesParserComposite

@Inject
class MainScreenViewModel(
    private val config: Config,
    private val machine: NesMachine,
    private val parser: InesParserComposite,
) : ViewModel() {

    private val _state = MutableStateFlow(MainWindowState())
    val state = _state.asStateFlow()

    private var rom: String? = null
    private var fps: Int? = null

    fun onCreate() {
        config.rom?.let { loadRom(it) }
        _state.update { it.copy(isCrtEnabled = config.crt) }
    }

    fun onRomSelected(romData: RomData?) {
        romData?.let { loadRom(it) }
    }

    fun onFpsUpdated(fps: Int) {
        this.fps = fps
        updateTitle()
    }

    fun setCrtEnabled(crtEnabled: Boolean) {
        _state.update { it.copy(isCrtEnabled = crtEnabled) }
    }

    private fun loadRom(romData: RomData) {
        this.rom = romData.name
        machine.insert(parser.parse(romData.bytes))
        machine.reset()
        _state.update { it.copy(isRunning = true) }
        updateTitle()
    }

    private fun updateTitle() {
        val elements = buildList {
            rom?.let { add(it) }
            fps?.let { add("$it fps") }
        }
        _state.update { current ->
            val values = if (elements.isNotEmpty()) {
                " | " + elements.joinToString(prefix = "[", postfix = "]") { it }
            } else {
                ""
            }
            current.copy(windowTitle = "CartridgeVM$values")
        }
    }
}

data class MainWindowState(
    val isRunning: Boolean = false,
    val windowTitle: String = "",
    val showRomPicker: Boolean = false,
    val isCrtEnabled: Boolean = false,
)