# CartridgeVM

CartridgeVM is a focused Kotlin/JVM NES emulator MVP. Its current target is Mapper 0 / NROM NTSC software, with validation intended against the original NTSC Super Mario Bros. when the user supplies a legally obtained ROM.

No ROMs, BIOS files, Nintendo assets, screenshots, extracted game data, disassemblies, or ROM patches are included in this repository.

## Requirements

Use JDK 21 or newer. The project uses Gradle with Kotlin DSL, Kotlin/JVM, LWJGL, GLFW, OpenGL, and Kotlin Test/JUnit 5.

## Build

```bash
./gradlew build
```

Run tests only:

```bash
./gradlew :app:test
```

## Run

Provide your own legally obtained `.nes` ROM file:

```bash
./gradlew run --args="/path/to/game.nes"
```

Optional flags:

```bash
./gradlew run --args="--debug /path/to/game.nes"
./gradlew run --args="--unlimited /path/to/game.nes"
```

Running without a ROM path prints usage information and exits non-zero.

## Controls

| Key | NES input |
| --- | --- |
| Arrow Up | D-pad Up |
| Arrow Down | D-pad Down |
| Arrow Left | D-pad Left |
| Arrow Right | D-pad Right |
| Z | A |
| X | B |
| Enter | Start |
| Right Shift | Select |
| P | Pause |
| R | Reset |
| Escape | Quit |

Opposite directions are filtered so left/right and up/down are not both sent to the emulated controller at the same time.

## Supported ROM Format

The loader supports iNES 1.0 Mapper 0 / NROM only:

* NROM-128 with 16 KiB PRG ROM mirrored at `$C000-$FFFF`
* NROM-256 with 32 KiB PRG ROM
* 8 KiB CHR ROM
* 8 KiB CHR RAM when CHR ROM size is zero
* Horizontal and vertical nametable mirroring

Unsupported formats are rejected with clear startup errors, including NES 2.0, unsupported mappers, four-screen mirroring, invalid Mapper 0 PRG/CHR sizes, invalid headers, and truncated data.

## Current Emulator Scope

Implemented:

* Single command-line application in `app`
* iNES parser and Mapper 0 cartridge mapping
* 2A03-style 6502 CPU core for official opcodes
* CPU bus RAM/register/cartridge/controller/OAM DMA mapping
* PPU registers, nametable and palette memory, CHR ROM/RAM access
* Background rendering, 8x8 sprite rendering, palette selection, sprite priority, sprite-zero hit approximation
* VBlank flag behavior, status read side effects, NMI triggering, buffered PPUDATA reads
* One standard NES controller via `$4016` serial protocol
* LWJGL GLFW window and OpenGL texture presentation of a software framebuffer
* NTSC-oriented frame pacing with `--unlimited` for debugging
* Pause, reset, and quit controls

## Known Limitations

This is an MVP, not a cycle-perfect emulator.

* No audio or APU emulation beyond safe CPU-bus stubs
* Mapper 0 only
* NTSC timing only
* No save states, rewind, cheats, debugger UI, gamepad support, two-player input, ZIP loading, network features, shaders, downloading, or patching
* PPU rendering is approximate in several edge cases
* Sprite overflow behavior is not cycle-accurate
* The steady-state CPU path avoids collections in dispatch, but address helper objects remain and should be removed before claiming strict allocation-free operation

Super Mario Bros. compatibility has not been claimed unless tested locally with a legally supplied ROM and observed to satisfy the acceptance criteria.

## Architecture

Core emulator code is under `app/src/main/kotlin/nes` and does not depend on GLFW or OpenGL.

* `nes.cartridge`: iNES parsing, cartridge metadata, Mapper 0
* `nes.cpu`: CPU core and CPU bus memory map
* `nes.ppu`: PPU registers, memory, timing, and framebuffer generation
* `nes.input`: NES controller strobe/serial protocol
* `nes.NesMachine`: core CPU/PPU/controller orchestration

Frontend code is under `app/src/main/kotlin/frontend`.

* `GlfwWindow`: window/context lifecycle
* `KeyboardInput`: fixed keyboard bindings
* `OpenGlRenderer`: uploads the 256x240 software framebuffer to one nearest-neighbor OpenGL texture
* `FramePacer`: monotonic accumulated-deadline frame limiter

CLI code is under `app/src/main/kotlin/app`.

## Legal Notice

Users must provide their own legally obtained ROM files. Do not commit ROMs or copyrighted game assets to this repository. `.nes` files are ignored by `.gitignore` to help prevent accidental commits.
