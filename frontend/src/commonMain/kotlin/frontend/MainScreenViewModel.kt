package frontend

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Inject
import nes.ConsoleRegion
import nes.NesMachine
import nes.cartridge.InesParserComposite
import nes.cartridge.RomData

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
    private var region: ConsoleRegion? = null

    fun onCreate() {
        viewModelScope.launch {
            machine.isPoweredOn.collect { isPoweredOn ->
                _state.update { it.copy(isRunning = isPoweredOn) }
            }
        }
        config.rom?.let { loadRom(it) }
        _state.update {
            it.copy(isCrtEnabled = config.crt)
        }
    }

    fun onRomSelected(romData: RomData?) {
        romData?.let { loadRom(it) }
    }

    fun onFpsUpdated(fps: Int) {
        this.fps = fps
        updateTitle()
    }

    fun setCrtEnabled(crtEnabled: Boolean) = _state.update {
        it.copy(isCrtEnabled = crtEnabled)
    }

    private fun loadRom(romData: RomData) = viewModelScope.launch {
        this@MainScreenViewModel.rom = romData.name
        val cartridge = parser.parse(romData)
        this@MainScreenViewModel.region = cartridge.region
        machine.powerOff()
        machine.insert(cartridge)
        machine.powerOn()
        updateTitle()
    }

    private fun updateTitle() = _state.update { current ->
        val elements = buildList {
            rom?.let { add(it) }
            region?.let { add(it.name) }
            fps?.let { add("$it fps") }
        }

        val values = if (elements.isNotEmpty()) {
            " | " + elements.joinToString(prefix = "[", postfix = "]") { it }
        } else {
            ""
        }
        current.copy(windowTitle = "Kassette$values")
    }
}

data class MainWindowState(
    val isRunning: Boolean = false,
    val windowTitle: String = "",
    val showRomPicker: Boolean = false,
    val isCrtEnabled: Boolean = false,
)
