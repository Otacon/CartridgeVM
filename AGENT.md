# Agent Handoff Notes

This repository is a Kotlin/JVM minimal NES emulator MVP targeting Mapper 0 / NROM games, specifically intended to run a user-supplied original NTSC Super Mario Bros. ROM. Do not add ROMs, screenshots, Nintendo assets, disassemblies, extracted data, or patches.

## Environment And Build

Workspace path:

```text
/Volumes/External/code/cartridgevm
```

This directory is not currently a Git repository. `git status` fails because there is no `.git` parent.

Gradle wrapper exists. The project is a starter multi-module Gradle build with `app` and `utils`; the emulator lives in `app`. `utils` is still the original starter module and is not used by the emulator.

Java target is 21. Both convention build logic files were changed from Java 25 to Java 21:

```text
buildSrc/build.gradle.kts
buildSrc/src/main/kotlin/kotlin-jvm.gradle.kts
```

macOS GLFW requires `-XstartOnFirstThread`; this was added to `app/build.gradle.kts` for `run` and generated application scripts on macOS.

Commands:

```bash
./gradlew build
./gradlew :app:test
./gradlew run --args="/path/to/game.nes"
./gradlew run --args="--debug /path/to/game.nes"
./gradlew run --args="--debug --unlimited /path/to/game.nes"
```

Important: do not use `--unlimited` when checking runtime speed. The frame limiter targets about `60.099` NTSC FPS.

## Current Status

The emulator now runs far enough that the user reports Super Mario Bros. works after PPU fixes. A later scroll bug appeared after about one screen of gameplay; this was likely caused by ignoring the horizontal nametable bit when rendering background pixels and has been fixed with a regression test.

Last verified command:

```bash
./gradlew build
```

Result: pass.

If continuing, rerun `./gradlew build` after new edits.

## Project Layout

Main source files:

```text
app/src/main/kotlin/app/CliArguments.kt
app/src/main/kotlin/app/Main.kt

app/src/main/kotlin/nes/NesMachine.kt
app/src/main/kotlin/nes/Timing.kt

app/src/main/kotlin/nes/cartridge/Cartridge.kt
app/src/main/kotlin/nes/cartridge/InesParser.kt
app/src/main/kotlin/nes/cartridge/Mapper.kt
app/src/main/kotlin/nes/cartridge/Mapper0.kt

app/src/main/kotlin/nes/cpu/AddressingMode.kt
app/src/main/kotlin/nes/cpu/Cpu6502.kt
app/src/main/kotlin/nes/cpu/CpuBus.kt
app/src/main/kotlin/nes/cpu/Opcode.kt

app/src/main/kotlin/nes/input/NesController.kt

app/src/main/kotlin/nes/ppu/Palette.kt
app/src/main/kotlin/nes/ppu/Ppu.kt

app/src/main/kotlin/frontend/FramePacer.kt
app/src/main/kotlin/frontend/GlfwWindow.kt
app/src/main/kotlin/frontend/KeyboardInput.kt
app/src/main/kotlin/frontend/OpenGlRenderer.kt
```

Tests:

```text
app/src/test/kotlin/NesTestSupport.kt
app/src/test/kotlin/InesParserTest.kt
app/src/test/kotlin/Cpu6502Test.kt
app/src/test/kotlin/BusTest.kt
app/src/test/kotlin/PpuTest.kt
```

## Implemented Features

ROM/cartridge:

* iNES 1.0 parser.
* Validates `NES<EOF>` magic.
* Rejects NES 2.0, malformed/truncated ROMs, unsupported mappers, unsupported four-screen mirroring.
* Mapper 0 only.
* NROM-128 and NROM-256 PRG ROM.
* CHR ROM and CHR RAM.
* Horizontal and vertical mirroring.

CPU/bus:

* 6502/Ricoh 2A03-style CPU core with official opcode coverage in a large `when` dispatch.
* Unofficial opcode execution throws an explicit error.
* Reset vector, NMI, IRQ, BRK/RTI, stack behavior.
* Internal RAM mirroring.
* PPU register mirroring.
* Controller 1 at `$4016`.
* OAM DMA at `$4014`.
* APU registers are safe stubs returning `0`/ignored writes.

PPU:

* 256x240 `IntArray` framebuffer.
* PPU register interface `$2000-$2007`.
* Nametable/palette/CHR memory access.
* Palette mirroring.
* Buffered PPUDATA reads and palette-read special behavior.
* VBlank flag, status read side effects, NMI generation.
* Background rendering with attribute table decoding.
* Sprite rendering, including 8x8 and 8x16 sprites.
* Horizontal/vertical sprite flip.
* Sprite priority ordering corrected to draw selected sprites back-to-front so lower OAM indices win.
* Sprite-zero hit approximation.
* Loopy scroll state fields: `v`, `t`, `fineX`, write latch.
* Rendering-time coarse-X increment, Y increment, horizontal transfer, vertical transfer.
* Background rendering now includes horizontal and vertical nametable bits from `v` when deriving source pixels.

Frontend/input:

* LWJGL GLFW window.
* OpenGL uploads software framebuffer to a nearest-neighbor texture.
* Keyboard mappings:
  * Arrow keys = D-pad.
  * `Z` = A.
  * `X` = B.
  * `Enter` and keypad Enter = Start.
  * Right Shift = Select.
  * `P` = Pause.
  * `R` = Reset.
  * Escape = Quit.
* Debug input edge logging prints lines such as `Input: START pressed`.
* Opposite directions are filtered in `NesController`.

Timing:

* NTSC target frame duration in `Timing.FRAME_NANOS`.
* `FramePacer` uses accumulated deadline and `LockSupport.parkNanos`.
* Debug prints frame limiter state and target FPS.

## Important Fixes Already Made

macOS GLFW:

* Error: `GLFW may only be used on the main thread... -XstartOnFirstThread`.
* Fixed by adding macOS JVM arg in `app/build.gradle.kts`.

LWJGL Unsafe warning:

* User saw terminal deprecation warnings from `sun.misc.Unsafe` through LWJGL on newer JVMs.
* This is harmless, but `run` now uses Java 21 toolchain to avoid newer-JVM terminal warnings where possible.

Input:

* User pressed Start and initially saw no effect.
* Added keypad Enter and debug input edge logging.
* User confirmed Start is intercepted.

SMB title screen broken/stuck:

* Screenshot showed title screen rendering badly with vertical duplication and game not progressing.
* Determined issue was emulator/PPU, not macOS/LWJGL.
* Added Loopy scroll rendering increments/transfers.
* Fixed vertical scroll double-application in background rendering.
* Disabled prefetch coarse-X increments in this scanline renderer because they polluted next scanline start state.
* Added 8x16 sprite rendering because SMB uses it.
* Fixed sprite priority ordering.
* Fixed background tile row bug: `ty` was `(sy shr 3) and 29`; changed to `(sy shr 3) and 31`.
* After these, user reported: `Good it works now`.

FPS too high:

* User reported `1200+ fps` instead of roughly 60/NTSC.
* `FramePacer` originally used `Thread.sleep(1)` and `Thread.yield()`; FPS was counted before wait.
* Changed to `LockSupport.parkNanos` with accumulated deadline.
* Moved FPS count/report after pacing.
* Added debug lines:
  * `Frame limiter: enabled` or `disabled`.
  * `Target FPS: 60.099`.
* Tests passed afterward. User has not yet confirmed runtime after this fix.

SMB scroll wrap broken after about one screen:

* User reported physics continued correctly, but level rendering broke after scrolling, looking like the next chunk was vertically flipped.
* Found likely renderer bug: `renderBackground` derived `scrollX` and `scrollY` from coarse coordinates/fine scroll but ignored nametable select bits in `v`.
* Fixed `scrollX` to include bit `0x0400` as +256 pixels.
* Fixed `scrollY` to include bit `0x0800` as +256 pixels.
* Added `PpuTest.backgroundUsesHorizontalNametableBit()` to verify rendering can start from `$2400` on vertical mirroring.
* `./gradlew :app:test` and `./gradlew build` passed after the fix.

## Known Limitations / Risks

Do not claim full SMB acceptance unless tested locally with a legally supplied ROM and observed.

Current likely limitations:

* PPU is still scanline-ish/approximate, not dot-cycle accurate.
* Sprite-zero hit is approximate and may still fail in edge cases.
* Sprite overflow is not accurate.
* APU is not implemented.
* Mapper 0 only.
* No PAL support.
* CPU addressing helper `data class Addr` allocates per certain addressing modes. Hot path is not strictly allocation-free yet.
* `CpuBus.doOamDma` currently allocates a new `ByteArray(256)` per DMA. This violates strict no-allocation hot path and should be changed to a reusable buffer if optimizing.
* `GlfwWindow.width()` and `height()` allocate small `IntArray`s each call; frontend per-frame allocation should be removed eventually by reusing arrays or tracking framebuffer size callback.
* `OpenGlRenderer.present()` uploads the whole framebuffer each frame; acceptable for MVP.
* Palette is an approximate public-domain-style palette, not copied from proprietary assets.

## Next Debugging Priorities

If user still sees high FPS:

1. Confirm they are not passing `--unlimited`.
2. Ask for debug startup lines: `Frame limiter: ...` and `Target FPS: ...`.
3. If limiter says enabled but FPS remains high, inspect `FramePacer.waitForNextFrame()` and verify it is called each loop.
4. Consider adding temporary debug timing output showing `remaining` nanoseconds before sleeping.
5. Consider using `Thread.sleep(ms, ns)` or `LockSupport.parkUntil` if `parkNanos` behaves poorly on their JVM, but `parkNanos` should generally work.

If gameplay is visually wrong:

1. Inspect `Ppu.renderBackground` and `renderSprites` first.
2. SMB depends on scroll behavior and sprite-zero hit. Focus there before CPU unless an explicit opcode error occurs.
3. Add small deterministic PPU tests for any corrected behavior.

If game freezes/waits forever:

1. Check sprite-zero hit behavior in `Ppu.renderSprite`.
2. Confirm `status` bit `0x40` is set when sprite 0 overlaps non-transparent background.
3. Confirm status bit clears on pre-render scanline cycle 1.
4. Confirm CPU reads `$2002` via mirrored register path and sees bit `0x40`.

If input stops working:

1. Run with `--debug`.
2. Check for `Input: START pressed`.
3. If edge is printed but game ignores input, inspect `NesController.write/read` strobe/latch behavior and CPU bus `$4016` reads.

## Style And Constraints

Maintain core/frontend separation: `nes` core must not depend on GLFW/OpenGL.

Prefer small targeted fixes over rewrites. Add tests for core behavior. Do not introduce ROMs or copyrighted assets.

Use `apply_patch` for manual edits. Avoid destructive git commands. There is no git repo at this path right now.
