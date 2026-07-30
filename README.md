# CartridgeVM

CartridgeVM is a focused Kotlin/JVM NES emulator MVP. Its current compatibility target is Mapper 0 / NROM NTSC software, with validation intended against the original NTSC Super Mario Bros. when the user supplies a legally obtained ROM. Mapper 1 / MMC1, Mapper 2 / UxROM, Mapper 3 / CNROM, Mapper 4 / MMC3, and Mapper 7 / AxROM are also supported.

No ROMs, BIOS files, Nintendo assets, screenshots, extracted game data, disassemblies, or ROM patches are included in this repository.

## Requirements

Use JDK 21 or newer. The project uses Gradle with Kotlin DSL, Kotlin Multiplatform, Kotlin/JVM, Kotlin/Wasm, Compose/Skiko, WebGL, OpenAL/WebAudio, and Kotlin Test/JUnit 5.

## Build

```bash
./gradlew build
```

Run tests only:

```bash
./gradlew :nes:jvmTest :frontend:jvmTest
```

## Run

### Desktop

Provide your own legally obtained `.nes` ROM file:

```bash
./gradlew run --args="/path/to/game.nes"
```

Optional flags:

```bash
./gradlew run --args="--debug /path/to/game.nes"
./gradlew run --args="--unlimited /path/to/game.nes"
./gradlew run --args="--controller /path/to/game.nes"
./gradlew run --args="--crt /path/to/game.nes"
```

Running without a ROM opens the desktop application so a ROM can be selected from the File menu.

Use `--crt` to enable a SkSL consumer CRT simulation with overscan, scanline beam shaping, phosphor slot masking, analog color bleed, halation, and edge falloff. The default renderer remains pixel-sharp when the flag is omitted.

### Web

Run the browser build:

```bash
./gradlew :frontend:wasmJsBrowserDevelopmentRun
```

Then choose a legally obtained `.nes` ROM from the browser menubar. Browser audio is resumed from normal menu gestures such as opening a ROM, pausing, resetting, or toggling CRT. Keyboard input is always available, and the first connected browser Gamepad API controller is polled automatically.

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

Pass `--controller` on desktop to use the first connected controller through JInput. Without the flag, keyboard input is used. The web build polls the first connected browser Gamepad API controller automatically in addition to keyboard input.

| Controller          | NES input |
|---------------------| --- |
| A                   | A |
| B                   | B |
| View / Back         | Select |
| Menu / Start        | Start |
| D-pad               | D-pad |
| Right bumper        | Pause |
| Left bumper         | Reset |
| Guide               | Quit |

## Supported ROM Format

The loader supports iNES 1.0 and NES 2.0 ROMs using Mapper 0 / NROM, Mapper 1 / MMC1, Mapper 2 / UxROM, Mapper 3 / CNROM, Mapper 4 / MMC3, or Mapper 7 / AxROM with submapper 0:

* NROM-128 with 16 KiB PRG ROM mirrored at `$C000-$FFFF`
* NROM-256 with 32 KiB PRG ROM
* 8 KiB CHR ROM
* 8 KiB CHR RAM when CHR ROM size is zero
* MMC1 with serial register loading, 16/32 KiB PRG banking, 4/8 KiB CHR ROM/RAM banking, 8 KiB PRG RAM, and runtime mirroring
* UxROM/UNROM with 32 KiB to 256 KiB PRG ROM, switchable `$8000-$BFFF`, fixed last bank at `$C000-$FFFF`, and 8 KiB CHR RAM
* CNROM with 16 KiB or 32 KiB PRG ROM and switchable 8 KiB CHR ROM banks
* MMC3 with 32 KiB to 512 KiB PRG ROM, 8 KiB PRG bank switching, CHR ROM/RAM banking, PRG RAM, runtime mirroring control, and scanline IRQs
* AxROM with 32 KiB to 256 KiB PRG ROM, switchable 32 KiB PRG banks, 8 KiB CHR RAM, and mapper-controlled one-screen mirroring
* Horizontal and vertical nametable mirroring
* NES 2.0 extended mapper numbers, submapper validation, linear and exponent/multiplier ROM sizes, explicit CHR RAM/NVRAM sizes, and NTSC/PAL/Dendy timing modes

Unsupported formats are rejected with clear startup errors, including unsupported mappers/submappers, four-screen mirroring, nonstandard console types, miscellaneous ROM regions, mixed CHR ROM/RAM boards, invalid mapper PRG/CHR sizes, invalid headers, and truncated data.

## Current Emulator Scope

Implemented:

* Single command-line application in `app`
* iNES 1.0 / NES 2.0 parser and Mapper 0 / Mapper 1 / Mapper 2 / Mapper 3 / Mapper 4 / Mapper 7 cartridge mapping
* Cartridge socket abstraction for insertion/removal and CPU/PPU cartridge access
* 2A03-style 6502 CPU core for official opcodes
* CPU bus RAM/register/controller/OAM DMA mapping, with cartridge space routed through the cartridge socket
* Dedicated PPU bus for CHR ROM/RAM, nametable memory, palette memory, and PPU-side mirroring
* Background rendering, 8x8 sprite rendering, palette selection, sprite priority, sprite-zero hit approximation
* VBlank flag behavior, status read side effects, NMI triggering, buffered PPUDATA reads
* SMB-focused APU audio with pulse, triangle, noise, and approximate DMC channels
* One standard NES controller via `$4016` serial protocol
* Desktop Compose/Skiko presentation of a software framebuffer, an optional SkSL CRT effect, and OpenAL audio playback
* Kotlin/Wasm browser frontend with DOM menubar, WebGL presentation, WebAudio playback, keyboard input, and Gamepad API controller input
* Region-aware frame pacing for NTSC, PAL, Dendy, multi-region, and Japan/Famicom-timed cartridges with `--unlimited` for debugging
* Pause, reset, and quit controls

## Known Limitations

This is an MVP, not a cycle-perfect emulator.

* APU support is approximate and focused on Mario-era software; DMC sample playback is approximate
* Mapper 0, Mapper 1, Mapper 2, Mapper 3, Mapper 4, and Mapper 7 only
* Mapper 1 supports basic MMC1/submapper 0 boards; SUROM/SOROM/SXROM-style extended banking variants are not supported
* Mapper 4 scanline IRQ timing is approximate, not cycle-perfect MMC3 A12 timing
* Region timing is approximate and selected from ROM header metadata; multi-region software defaults to NTSC timing
* No save states, rewind, cheats, debugger UI, two-player input, ZIP loading, network features, downloading, or patching
* PPU rendering is approximate in several edge cases
* Sprite overflow behavior is not cycle-accurate
* The steady-state CPU path avoids collections in dispatch, but address helper objects remain and should be removed before claiming strict allocation-free operation

Super Mario Bros. compatibility has not been claimed unless tested locally with a legally supplied ROM and observed to satisfy the acceptance criteria.

## Architecture

Core emulator code is under `nes/src/commonMain/kotlin/nes` and does not depend on frontend graphics or audio APIs.

* `nes.cartridge`: iNES parsing, cartridge metadata, cartridge socket, Mapper abstraction, Mapper 0, Mapper 1, Mapper 2, Mapper 3, Mapper 4, Mapper 7
* `nes.cpu`: CPU core and CPU bus memory map
* `nes.apu`: pulse, triangle, noise, and DMC channel audio generation
* `nes.ppu`: PPU registers, PPU bus, memory, timing, and framebuffer generation
* `nes.input`: NES controller strobe/serial protocol
* `nes.NesMachine`: core CPU/PPU/APU/controller orchestration and cartridge insertion

The CPU bus and PPU bus do not depend on mapper classes directly. They communicate with `CartridgeSocket`, which delegates to the mapper stored by the currently inserted `Cartridge`. Parsed iNES ROMs are validated in `InesParser`; unsupported mapper numbers are rejected there before a cartridge is created.

Frontend code is under `frontend/src`.

* `ComposeSkiaScreen`: desktop Compose drawing and emulator thread lifecycle
* `PlatformKeyboardInput`: fixed keyboard bindings
* `PlatformControllerInput`: JInput gamepad bindings enabled with `--controller`
* `PlatformAudioPipeline`: queues generated mono PCM samples to OpenAL or WebAudio
* `PlatformRenderer`: presents the 256x240 framebuffer through Skiko/SkSL on desktop or WebGL on Web
* `FramePacer`: monotonic accumulated-deadline frame limiter

Desktop CLI code is under `frontend/src/jvmMain/kotlin/app`.

## Legal Notice

Users must provide their own legally obtained ROM files. Do not commit ROMs or copyrighted game assets to this repository. `.nes` files are ignored by `.gitignore` to help prevent accidental commits.
