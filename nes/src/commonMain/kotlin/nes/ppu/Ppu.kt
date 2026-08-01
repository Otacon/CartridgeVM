package nes.ppu

import nes.PpuTiming
import nes.Timing
import nes.util.low8Bits
import nes.util.toUnsignedInt

class Ppu(
    private val bus: PpuBus,
) {
    val framebuffer = IntArray(SCREEN_WIDTH * SCREEN_HEIGHT)
    val oam = ByteArray(256)
    private val scanlineSprites = IntArray(8)
    private val paletteColors = IntArray(32)

    var ctrl = 0
        private set
    var mask = 0
        private set
    var status = 0
        private set
    var oamAddress = 0
        private set
    var v = 0
        private set
    var t = 0
        private set
    var fineX = 0
        private set
    var writeLatch = false
        private set
    var scanline = 0
        private set
    var cycle = 0
        private set
    var frameComplete = false
        private set
    var nmiRequested = false
        private set

    var timing: Timing = Timing.DEFAULT

    private var readBuffer = 0
    private var pendingSpriteZeroHitCycle = -1
    private var oddFrame = false
    private val bgOpaque = BooleanArray(256)
    private val spriteClaimed = BooleanArray(256)

    fun reset() {
        ctrl = 0
        mask = 0
        status = 0
        oamAddress = 0
        v = 0
        t = 0
        fineX = 0
        writeLatch = false
        scanline = 0
        cycle = 0
        frameComplete = false
        nmiRequested = false
        readBuffer = 0
        pendingSpriteZeroHitCycle = -1
        oddFrame = false
    }

    fun pollNmi(): Boolean {
        val value = nmiRequested
        nmiRequested = false
        return value
    }

    fun clearFrameComplete() {
        frameComplete = false
    }

    fun step() {
        val rendering = renderingEnabled()
        val visibleScanline = scanline in 0 until SCREEN_HEIGHT
        val preRenderScanline = timing.scanlinesPerFrame - 1
        if (visibleScanline && cycle == FIRST_VISIBLE_DOT) renderScanline(scanline)
        if (visibleScanline && cycle == pendingSpriteZeroHitCycle) status = status or STATUS_SPRITE_ZERO_HIT
        if (scanline == VBLANK_SCANLINE && cycle == FIRST_VISIBLE_DOT) {
            status = status or STATUS_VBLANK
            if ((ctrl and 0x80) != 0) nmiRequested = true
        }
        if (scanline == preRenderScanline && cycle == FIRST_VISIBLE_DOT) status = status and 0x1F
        if (rendering && (visibleScanline || scanline == preRenderScanline)) {
            if (cycle in FIRST_VISIBLE_DOT..LAST_VISIBLE_DOT && (cycle and 7) == 0) incrementCoarseX()
            if (cycle == LAST_VISIBLE_DOT) incrementY()
            if (cycle == HORIZONTAL_TRANSFER_DOT) transferHorizontalAddress()
            if (cycle == MAPPER_SCANLINE_DOT && (visibleScanline || scanline == preRenderScanline)) bus.clockScanline()
            if (scanline == preRenderScanline && cycle in 280..304) transferVerticalAddress()
        }
        if (timing.skipsOddFrameDot && rendering && oddFrame && scanline == preRenderScanline && cycle == ODD_FRAME_LAST_DOT) {
            finishFrame()
            return
        }
        cycle++
        if (cycle >= PpuTiming.PPU_CYCLES_PER_SCANLINE) {
            cycle = 0
            scanline++
            if (scanline >= timing.scanlinesPerFrame) finishFrame()
        }
    }

    fun cpuRead(register: Int): Int = when (register and 7) {
        2 -> {
            val result = status
            status = status and 0x7F
            writeLatch = false
            result
        }

        4 -> oam[oamAddress].toUnsignedInt()
        7 -> readData()
        else -> 0
    }

    fun cpuWrite(register: Int, value: Int) {
        val data = value.low8Bits()
        when (register and 7) {
            0 -> {
                val old = ctrl
                ctrl = data
                t = (t and 0xF3FF) or ((data and 0x03) shl 10)
                if ((old and 0x80) == 0 && (ctrl and 0x80) != 0 && (status and 0x80) != 0) nmiRequested = true
            }

            1 -> mask = data
            3 -> oamAddress = data
            4 -> {
                oam[oamAddress] = data.toByte(); oamAddress = (oamAddress + 1).low8Bits()
            }

            5 -> if (!writeLatch) {
                fineX = data and 7
                t = (t and 0xFFE0) or (data shr 3)
                writeLatch = true
            } else {
                t = (t and 0x8FFF) or ((data and 7) shl 12)
                t = (t and 0xFC1F) or ((data and 0xF8) shl 2)
                writeLatch = false
            }

            6 -> if (!writeLatch) {
                t = (t and 0x00FF) or ((data and 0x3F) shl 8)
                writeLatch = true
            } else {
                t = (t and 0x7F00) or data
                v = t
                writeLatch = false
            }

            7 -> writeData(data)
        }
    }

    fun writeOamDma(page: ByteArray) {
        val firstCopyLength = oam.size - oamAddress
        page.copyInto(oam, oamAddress, 0, firstCopyLength)
        if (firstCopyLength < page.size) page.copyInto(oam, 0, firstCopyLength, page.size)
    }

    fun ppuRead(address: Int): Int = bus.read(address)

    fun ppuWrite(address: Int, value: Int) = bus.write(address, value)

    private fun readData(): Int {
        val addr = v and 0x3FFF
        val result = if (addr >= 0x3F00) {
            readBuffer = ppuRead(addr - 0x1000)
            ppuRead(addr)
        } else {
            val buffered = readBuffer
            readBuffer = ppuRead(addr)
            buffered
        }
        incrementVramAddress()
        return result
    }

    private fun writeData(value: Int) {
        ppuWrite(v, value)
        incrementVramAddress()
    }

    private fun incrementVramAddress() {
        v = (v + if ((ctrl and 0x04) != 0) 32 else 1) and 0x7FFF
    }

    private fun renderingEnabled() = (mask and 0x18) != 0

    private fun incrementCoarseX() {
        v = if ((v and 0x001F) == 31) {
            (v and 0xFFE0) xor 0x0400
        } else {
            (v + 1) and 0x7FFF
        }
    }

    private fun incrementY() {
        v = if ((v and 0x7000) != 0x7000) {
            (v + 0x1000) and 0x7FFF
        } else {
            var next = v and 0x8FFF
            var y = (v and 0x03E0) shr 5
            if (y == 29) {
                y = 0
                next = next xor 0x0800
            } else if (y == 31) {
                y = 0
            } else {
                y++
            }
            (next and 0xFC1F) or (y shl 5)
        }
    }

    private fun transferHorizontalAddress() {
        v = (v and 0xFBE0) or (t and 0x041F)
    }

    private fun transferVerticalAddress() {
        v = (v and 0x841F) or (t and 0x7BE0)
    }

    private fun renderScanline(y: Int) {
        val bgEnabled = (mask and 0x08) != 0
        val spritesEnabled = (mask and 0x10) != 0
        pendingSpriteZeroHitCycle = -1
        var paletteIndex = 0
        while (paletteIndex < paletteColors.size) {
            paletteColors[paletteIndex] = Palette.COLORS[ppuRead(0x3F00 + paletteIndex) and 0x3F]
            paletteIndex++
        }
        val rowStart = y * SCREEN_WIDTH
        framebuffer.fill(paletteColors[0], rowStart, rowStart + SCREEN_WIDTH)
        bgOpaque.fill(false)
        spriteClaimed.fill(false)
        if (bgEnabled) renderBackground(rowStart)
        if (bgEnabled || spritesEnabled) {
            val spriteHeight = if ((ctrl and 0x20) != 0) 16 else 8
            val selectedSprites = evaluateSprites(y, spriteHeight)
            if (spritesEnabled) renderSprites(y, rowStart, spriteHeight, selectedSprites)
        }
    }

    private fun renderBackground(rowStart: Int) {
        val showLeftBackground = (mask and 0x02) != 0
        val scrollX = (((v and 0x001F) shl 3) + fineX + if ((v and 0x0400) != 0) 256 else 0) and 0x1FF
        val scrollY =
            ((((v shr 5) and 0x1F) shl 3) + ((v shr 12) and 7) + if ((v and 0x0800) != 0) 256 else 0) and 0x1FF
        val patternBase = if ((ctrl and 0x10) != 0) 0x1000 else 0
        val ty = (scrollY shr 3) and 31
        val fineY = scrollY and 7
        val verticalNametable = ((scrollY shr 8) and 1) shl 1
        var cachedTileKey = -1
        var palette = 0
        var lo = 0
        var hi = 0
        var x = 0
        while (x < SCREEN_WIDTH) {
            val sx = (x + scrollX) and 0x1FF
            val nt = ((sx shr 8) and 1) or verticalNametable
            val tx = (sx shr 3) and 31
            val tileKey = (nt shl 5) or tx
            if (tileKey != cachedTileKey) {
                cachedTileKey = tileKey
                val ntBase = 0x2000 + nt * 0x400
                val tile = ppuRead(ntBase + ty * 32 + tx)
                val attr = ppuRead(ntBase + 0x3C0 + (ty shr 2) * 8 + (tx shr 2))
                val shift = ((ty and 2) shl 1) or (tx and 2)
                palette = (attr shr shift) and 3
                lo = ppuRead(patternBase + tile * 16 + fineY)
                hi = ppuRead(patternBase + tile * 16 + fineY + 8)
            }
            val bit = 7 - (sx and 7)
            val color = (((hi shr bit) and 1) shl 1) or ((lo shr bit) and 1)
            if (color != 0 && (x >= 8 || showLeftBackground)) {
                bgOpaque[x] = true
                framebuffer[rowStart + x] = paletteColors[palette * 4 + color]
            }
            x++
        }
    }

    private fun evaluateSprites(y: Int, spriteHeight: Int): Int {
        var selected = 0
        var i = 0
        while (i < 64) {
            val base = i * 4
            val sy = oam[base].toUnsignedInt() + 1
            if (y >= sy && y < sy + spriteHeight) {
                if (selected < scanlineSprites.size) {
                    scanlineSprites[selected] = i
                    selected++
                } else {
                    status = status or STATUS_SPRITE_OVERFLOW
                    break
                }
            }
            i++
        }
        return selected
    }

    private fun renderSprites(y: Int, rowStart: Int, spriteHeight: Int, selectedSprites: Int) {
        val spritePatternBase = if ((ctrl and 0x08) != 0) 0x1000 else 0
        val showLeftSprites = (mask and 0x04) != 0
        var s = 0
        while (s < selectedSprites) {
            renderSprite(scanlineSprites[s], y, rowStart, spriteHeight, spritePatternBase, showLeftSprites)
            s++
        }
    }

    private fun renderSprite(
        i: Int,
        y: Int,
        rowStart: Int,
        spriteHeight: Int,
        spritePatternBase: Int,
        showLeftSprites: Boolean,
    ) {
        val base = i * 4
        val sy = oam[base].toUnsignedInt() + 1
        val tile = oam[base + 1].toUnsignedInt()
        val attr = oam[base + 2].toUnsignedInt()
        val sx = oam[base + 3].toUnsignedInt()
        var row = y - sy
        if ((attr and 0x80) != 0) row = spriteHeight - 1 - row
        val patternAddress = if (spriteHeight == 16) {
            val table = (tile and 1) * 0x1000
            val topTile = tile and 0xFE
            table + (topTile + (row shr 3)) * 16 + (row and 7)
        } else {
            spritePatternBase + tile * 16 + row
        }
        val lo = ppuRead(patternAddress)
        val hi = ppuRead(patternAddress + 8)
        var px = 0
        while (px < 8) {
            val bit = if ((attr and 0x40) != 0) px else 7 - px
            val color = (((hi shr bit) and 1) shl 1) or ((lo shr bit) and 1)
            val x = sx + px
            if (color != 0 && x in 0..255 && (x >= 8 || showLeftSprites) && !spriteClaimed[x]) {
                spriteClaimed[x] = true
                if (i == 0 && bgOpaque[x] && x != 255 && pendingSpriteZeroHitCycle == -1) {
                    pendingSpriteZeroHitCycle = x + 1
                }
                if ((attr and 0x20) == 0 || !bgOpaque[x]) {
                    val pal = attr and 3
                    framebuffer[rowStart + x] = paletteColors[0x10 + pal * 4 + color]
                }
            }
            px++
        }
    }

    private fun finishFrame() {
        cycle = 0
        scanline = 0
        frameComplete = true
        oddFrame = !oddFrame
    }

    companion object {
        private const val SCREEN_WIDTH = 256
        private const val SCREEN_HEIGHT = 240
        private const val VBLANK_SCANLINE = 241
        private const val FIRST_VISIBLE_DOT = 1
        private const val LAST_VISIBLE_DOT = 256
        private const val HORIZONTAL_TRANSFER_DOT = 257
        private const val MAPPER_SCANLINE_DOT = 260
        private const val ODD_FRAME_LAST_DOT = 339
        private const val STATUS_SPRITE_OVERFLOW = 0x20
        private const val STATUS_SPRITE_ZERO_HIT = 0x40
        private const val STATUS_VBLANK = 0x80
    }
}
