# Agent Handoff

CartridgeVM is a Kotlin/JVM NES emulator MVP. The near-term objective is practical compatibility with the original NTSC
Super Mario Bros. from a user-supplied Mapper 0 `.nes` ROM. Mapper 2 / UxROM, Mapper 3 / CNROM, and Mapper 4 / MMC3 are also supported.

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
./gradlew :app:test
./gradlew run --args="/path/to/game.nes"
./gradlew run --args="--debug /path/to/game.nes"
./gradlew run --args="--debug --unlimited /path/to/game.nes"
```

Use `--unlimited` only for debugging speed. Normal play should run with the frame limiter enabled at about NTSC `60.099`
FPS.

macOS GLFW requires `-XstartOnFirstThread`; this is configured in `app/build.gradle.kts` for Gradle `run` and generated
application scripts.

## Project Layout

Core emulator code is under `app/src/main/kotlin/nes` and should not depend on GLFW, OpenGL, or OpenAL.

Key packages:

```text
app/src/main/kotlin/app/          CLI entry point and argument parsing
app/src/main/kotlin/frontend/     GLFW, OpenGL presentation, OpenAL audio, keyboard input, pacing
app/src/main/kotlin/nes/          Machine orchestration and timing
app/src/main/kotlin/nes/apu/      APU audio generation
app/src/main/kotlin/nes/cartridge/iNES parser, cartridge socket, Mapper 0, Mapper 2, Mapper 3, and Mapper 4
app/src/main/kotlin/nes/cpu/      6502 CPU and CPU bus
app/src/main/kotlin/nes/input/    NES controller protocol
app/src/main/kotlin/nes/ppu/      PPU registers, memory, timing, and software rendering
```

Tests are in:

```text
app/src/test/kotlin/
```

## Implemented Scope

ROM/cartridge:

* iNES 1.0 parsing.
* Mapper 0 / NROM, Mapper 2 / UxROM, Mapper 3 / CNROM, and Mapper 4 / MMC3.
* NROM-128 and NROM-256.
* UxROM/UNROM with switchable 16 KiB lower PRG bank and fixed last 16 KiB upper PRG bank.
* CNROM with fixed PRG ROM and switchable 8 KiB CHR ROM banks.
* MMC3 with 8 KiB PRG banking, 1/2 KiB CHR banking, PRG RAM, runtime mirroring control, and approximate scanline IRQs.
* CHR ROM and CHR RAM.
* Horizontal and vertical mirroring.
* `Cartridge` stores ROM metadata/data plus a generic `Mapper` instance.
* `CartridgeSocket` simulates cartridge insertion/removal and is the only cartridge access point for CPU/PPU buses.
* Clear rejection for invalid headers, truncated ROMs, NES 2.0, unsupported mappers, four-screen mirroring, and invalid
  mapper sizes.

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
* Sprite rendering with 8x8 and 8x16 sprites, flips, priority, and approximate sprite-zero hit.
* Loopy scroll state: `v`, `t`, fine X, write latch, coarse X/Y increments, horizontal/vertical transfers.

APU/audio:

* SMB-focused mono PCM generation at 44.1 kHz.
* Pulse 1, pulse 2, triangle, and noise channels.
* Length counters, envelopes, approximate sweep, triangle linear counter, frame counter clocks.
* OpenAL frontend playback.
* Approximate DMC sample playback with CPU-memory sample reads.

Input/frontend:

* LWJGL GLFW window.
* OpenGL presents one nearest-neighbor texture from the software framebuffer.
* OpenAL queues generated audio samples.
* Keyboard controls:
    * Arrow keys: D-pad.
    * `Z`: A.
    * `X`: B.
    * Enter and keypad Enter: Start.
    * Right Shift: Select.
    * `P`: Pause.
    * `R`: Reset.
    * Escape: Quit.
* Controller is exposed through `$4016` strobe/serial reads.
* Opposite directions are filtered.

Diagnostics:

* `--debug` prints ROM metadata, frame limiter state, target FPS, FPS, and input edge logs.
* `--unlimited` disables frame limiting.

Architecture notes:

* `NesMachine` starts without a constructor cartridge argument; call `insert(cartridge)` before reset/run for normal use.
* `InesParser` validates iNES mapper numbers and creates the concrete mapper instance for parsed cartridges.
* CPU, PPU, APU, and bus packages should not depend on concrete mapper classes.
* Use shared bit/byte helpers from `nes.util.BitExtensions` instead of repeating raw truncation masks:
  `Byte.toUnsignedInt()` for byte-array reads, `Int.low8Bits()` for byte/register truncation, `Int.low16Bits()` for CPU
  address truncation, and `Int.pageBase()` for 6502 page-crossing checks. These helpers intentionally return `Int` and
  use direct masks internally for hot emulator paths. Keep explicit `and` masks for local flag or bitfield checks when
  the mask itself is meaningful.

## Known Limitations

* Compatibility target is Super Mario Bros. / Mapper 0, not broad NES compatibility.
* No mappers beyond Mapper 0, Mapper 2, Mapper 3, and Mapper 4.
* Mapper 4 scanline IRQ timing is approximate, not cycle-perfect MMC3 A12 timing.
* NTSC only.
* PPU is approximate, not cycle-perfect.
* Sprite-zero hit is approximate.
* Sprite overflow is not accurate.
* APU is approximate and SMB-focused.
* DMC sample playback is approximate.
* No save states, rewind, cheats, debugger UI, gamepad support, two-player input, ZIP loading, networking, shaders, ROM
  downloading, or ROM patching.
* Hot path still has avoidable allocations in some places, including CPU address helper objects, OAM DMA buffer
  allocation, and frontend framebuffer-size queries.

## Development Guidance

Prefer small targeted fixes with deterministic tests. For SMB issues, inspect PPU scroll/sprite-zero behavior first,
then input/controller protocol, then CPU/APU details.

Run at least:

```bash
./gradlew :app:test
```

Run full build before considering work complete:

```bash
./gradlew build
```
