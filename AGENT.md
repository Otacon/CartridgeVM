# Agent Handoff

Kassette is a Kotlin Multiplatform NES emulator MVP with desktop and browser frontends. The near-term objective is practical compatibility with user-supplied `.nes` ROMs, with Super Mario Bros. as the primary Mapper 0 reference. Mapper 1 / MMC1, Mapper 2 / UxROM, Mapper 3 / CNROM, Mapper 4 / MMC3, and Mapper 7 / AxROM are also supported.

Do not add ROMs, BIOS files, Nintendo assets, screenshots, extracted data, disassemblies, or ROM patches.

## Repository

Workspace:

```text
/Volumes/External/code/cartridgevm
```

This is a Git repository. Check the worktree before edits:

```bash
git status --short
```

Do not revert user changes unless explicitly requested.

## Build And Run

Java target: JDK 21.

Main commands:

```bash
./gradlew build
./gradlew :nes:jvmTest :frontend:jvmTest
./gradlew run --args="/path/to/game.nes"
./gradlew run --args="--debug /path/to/game.nes"
./gradlew run --args="--controller /path/to/game.nes"
./gradlew run --args="--crt /path/to/game.nes"
./gradlew :frontend:wasmJsBrowserDevelopmentRun
```

Desktop play uses region-aware frame pacing derived from cartridge timing. There is no current `--unlimited` flag.

## Project Layout

Core emulator code is under `nes/src/commonMain/kotlin/nes` and should not depend on frontend graphics, input, or audio APIs.

Key packages:

```text
nes/src/commonMain/kotlin/nes/                 Machine orchestration and timing
nes/src/commonMain/kotlin/nes/apu/             APU audio generation
nes/src/commonMain/kotlin/nes/cartridge/       Cartridge socket and Mapper 0, 1, 2, 3, 4, and 7
nes/src/commonMain/kotlin/nes/cpu/             6502 CPU and CPU bus
nes/src/commonMain/kotlin/nes/input/           NES controller protocol
nes/src/commonMain/kotlin/nes/ppu/             PPU registers, memory, timing, and software rendering
frontend/src/commonMain/kotlin/frontend/       Shared Compose UI, runtime host, parser, and platform contracts
frontend/src/jvmMain/kotlin/app/               Desktop CLI entry point and argument parsing
frontend/src/jvmMain/kotlin/frontend/          Desktop Compose/Skiko, OpenAL audio, JInput controller, keyboard, pacing
frontend/src/wasmJsMain/kotlin/app/            Browser app entry point
frontend/src/wasmJsMain/kotlin/frontend/       Browser WebGL, WebAudio, File API, keyboard, Gamepad API
```

Tests are in:

```text
nes/src/commonTest/kotlin/
frontend/src/commonTest/kotlin/
frontend/src/jvmTest/kotlin/
```

## Implemented Scope

ROM/cartridge:

* iNES 1.0 and NES 2.0 parsing.
* nes20db SHA-1 metadata overrides for known ROM region, mapper, submapper, and mirroring.
* Filename region fallback for common USA/Japan/Europe/PAL markers when header metadata is missing or ambiguous.
* Mapper 0 / NROM, Mapper 1 / MMC1, Mapper 2 / UxROM, Mapper 3 / CNROM, Mapper 4 / MMC3, and Mapper 7 / AxROM.
* NROM-128 and NROM-256.
* MMC1 with serial register loading, PRG/CHR banking, PRG RAM, and runtime one-screen/horizontal/vertical mirroring.
* UxROM/UNROM with switchable 16 KiB lower PRG bank and fixed last 16 KiB upper PRG bank.
* CNROM with fixed PRG ROM and switchable 8 KiB CHR ROM banks.
* MMC3 with precomputed 8 KiB PRG/1 KiB CHR page offsets, PRG RAM, runtime mirroring control, and approximate scanline IRQs.
* AxROM with switchable 32 KiB PRG banks up to 512 KiB PRG ROM, CHR RAM, and mapper-controlled one-screen mirroring.
* CHR ROM and CHR RAM.
* Horizontal and vertical mirroring.
* PAL, NTSC, Dendy, and multi-region timing metadata; multi-region currently maps to NTSC timing.
* `Cartridge` stores ROM metadata/data plus a generic `Mapper` instance.
* `CartridgeSocket` simulates cartridge insertion/removal and is the only cartridge access point for CPU/PPU buses.
* Clear rejection for invalid headers, truncated ROMs, unsupported mappers/submappers, unsupported NES 2.0 hardware,
  four-screen mirroring, and invalid mapper sizes.

CPU/bus:

* 6502/Ricoh 2A03-style CPU with official opcodes.
* Reset, NMI, IRQ, BRK/RTI, stack behavior, page-cross penalties, branch penalties, zero-page wrapping, indirect JMP
  wrap bug.
* CPU memory map for RAM, PPU registers, APU registers, OAM DMA, controller, and cartridge space.
* Cartridge CPU-space reads/writes go through `CartridgeSocket`, not directly through mapper classes.
* Unofficial opcodes intentionally throw explicit errors.

PPU:

* 256x240 software framebuffer.
* PPU register interface `$2000-$2007` with mirroring.
* Dedicated PPU bus for CHR ROM/RAM, nametable RAM, palette RAM, and PPU-side mirroring.
* CHR ROM/RAM access goes through `CartridgeSocket`, not directly through mapper classes.
* Buffered PPUDATA reads and palette read behavior.
* VBlank flag, status read side effects, NMI generation.
* Background rendering with attributes, scrolling, and nametable-bit handling.
* Sprite rendering with 8x8 and 8x16 sprites, flips, priority, approximate sprite-zero hit, and basic overflow detection.
* Loopy scroll state: `v`, `t`, fine X, write latch, coarse X/Y increments, horizontal/vertical transfers.
* The scanline renderer caches palette and tile fetch data; preserve primitive arrays and avoid per-pixel allocations.
* Rendered odd frames skip one pre-render PPU dot.

APU/audio:

* Region-aware mono PCM generation at 44.1 kHz for the 2A03/2A07-style APU.
* Pulse 1, pulse 2, triangle, noise, and DMC channels with region-specific noise and DMC periods.
* Length counters, envelopes, pulse sweep and overflow muting, triangle linear counter/DAC hold, and frame-counter
  clocks with frame IRQ status, inhibit, and acknowledgement behavior.
* APU behavior is partially aligned with MesenCE for pulse sweep divider periods, DMC startup bit-counter silence, and
  NTSC/PAL/Dendy DMC period tables.
* NES nonlinear pulse/TND mixing followed by 90 Hz and 440 Hz high-pass filters and a 14 kHz low-pass filter.
* OpenAL desktop playback and WebAudio browser playback.
* DMC sample playback with CPU-memory reads, buffered output after reader disable, looping, IRQ state, address wrapping,
  and fixed four-cycle CPU stalls.

Input/frontend:

* Desktop Compose/Skiko window and browser Compose viewport.
* Desktop renderer presents the software framebuffer through Skiko/SkSL; browser renderer uses WebGL.
* Optional CRT shader/effect via `--crt` on desktop or the menu toggle in the UI.
* OpenAL queues generated audio samples on desktop; WebAudio queues PCM buffers in the browser.
* Keyboard controls:
    * Arrow keys: D-pad.
    * `Z`: A.
    * `X`: B.
    * Enter: Start.
    * Right Shift: Select.
    * `R`: Reset.
    * Escape: Quit.
* Desktop controller support is available through `--controller` using JInput.
* Browser controller support polls the first connected Gamepad API device automatically.
* Controller is exposed through `$4016` strobe/serial reads.
* Opposite directions are filtered.
* Input state is sampled before each frame and approximately every 2 ms during frame emulation to reduce input-to-emulation latency. Reset edges must remain latched across repeated within-frame polls.

Diagnostics:

* `--debug` enables debug-level Kermit logging.
* Runtime window title includes ROM name, selected region, and measured FPS.

Architecture notes:

* `NesMachine` starts without a constructor cartridge argument; call `insert(cartridge)` before reset/run for normal use.
* `InesParserComposite` hashes ROM data excluding the iNES header/trainer payload, consults nes20db, routes to iNES 1.0 or NES 2.0 parsing, and applies database metadata when present.
* `InesParser` validates iNES mapper numbers and creates the concrete mapper instance for parsed cartridges.
* CPU, PPU, APU, and bus packages should not depend on concrete mapper classes.
* NMI is edge-latched; IRQ is a level sampled at CPU instruction boundaries from mapper and APU sources.
* `CpuStall` owns pending OAM/DMC CPU stalls. `CpuBus` drains stalls as part of a CPU step; DMA components request them.
* Machine reset resets CPU, PPU, APU, controller protocol, pending stalls, and active mapper runtime state while preserving RAM.
* Use shared bit/byte helpers from `nes.util.BitExtensions` instead of repeating raw truncation masks:
  `Byte.toUnsignedInt()` for byte-array reads, `Int.low8Bits()` for byte/register truncation, `Int.low16Bits()` for CPU
  address truncation, and `Int.pageBase()` for 6502 page-crossing checks. These helpers intentionally return `Int` and
  use direct masks internally for hot emulator paths. Keep explicit `and` masks for local flag or bitfield checks when
  the mask itself is meaningful.

## Known Limitations

* Compatibility target is Super Mario Bros. / Mapper 0, not broad NES compatibility.
* No mappers beyond Mapper 0, Mapper 1, Mapper 2, Mapper 3, Mapper 4, and Mapper 7.
* Mapper 1 does not support SUROM/SOROM/SXROM-style extended banking variants.
* Mapper 4 scanline IRQ timing is approximate, not cycle-perfect MMC3 A12 timing.
* NTSC/PAL/Dendy timing is supported from nes20db, ROM header metadata, and filename fallback; timing remains approximate.
* PPU is approximate, not cycle-perfect.
* Sprite-zero hit is approximate.
* Sprite overflow uses simple ninth-sprite detection and does not emulate the hardware evaluation bug.
* APU register effects and frame-counter events are instruction-batched; `$4017`'s parity-dependent 3/4-cycle reset
  delay, exact frame IRQ edge timing, and Mesen's full event scheduler are not modeled.
* DMC DMA fetches immediately and always stalls for four cycles. Exact 3/4-cycle alignment, OAM DMA conflicts, and
  cycle-level bus arbitration are not modeled.
* APU PCM uses point sampling plus the output filter chain, not band-limited synthesis, so high-frequency aliasing can
  remain.
* No save states, rewind, cheats, debugger UI, two-player input, ZIP loading, networking, ROM
  downloading, or ROM patching.
* PPU rendering remains scanline-based, so mid-scanline palette, scroll, CHR bank, mask, and OAM changes are approximate.
* OAM DMA uses a fixed 513-cycle stall; exact 513/514 parity requires intra-instruction CPU bus-cycle timing.

## Development Guidance

Prefer small targeted fixes with deterministic tests. For SMB issues, inspect PPU scroll/sprite-zero behavior first,
then input/controller protocol, then CPU/APU details.

Run at least:

```bash
./gradlew allTests
```

For faster focused checks, run:

```bash
./gradlew :nes:jvmTest :frontend:jvmTest
```

Run full build before considering work complete:

```bash
./gradlew build
```
