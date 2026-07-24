package nes.ppu

import di.AppScope
import me.tatarka.inject.annotations.Inject

@Inject
@AppScope
class Ppu(
    private val bus: PpuBus,
) {
    val framebuffer = IntArray(256 * 240)
    val oam = ByteArray(256)
    private val scanlineSprites = IntArray(8)

    var ctrl = 0; private set
    var mask = 0; private set
    var status = 0; private set
    var oamAddress = 0; private set
    var v = 0; private set
    var t = 0; private set
    var fineX = 0; private set
    var writeLatch = false; private set
    var scanline = 0; private set
    var cycle = 0; private set
    var frameComplete = false; private set
    var nmiRequested = false; private set

    private var readBuffer = 0
    private val bgOpaque = BooleanArray(256)

    fun reset() {
        ctrl = 0; mask = 0; status = 0; oamAddress = 0; v = 0; t = 0; fineX = 0
        writeLatch = false; scanline = 0; cycle = 0; frameComplete = false; nmiRequested = false; readBuffer = 0
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
        if (scanline in 0..239 && cycle == 1) renderScanline(scanline)
        if (scanline == 241 && cycle == 1) {
            status = status or 0x80
            if ((ctrl and 0x80) != 0) nmiRequested = true
        }
        if (scanline == 261 && cycle == 1) status = status and 0x1F
        if (rendering && (scanline in 0..239 || scanline == 261)) {
            if (cycle in 1..256 && (cycle and 7) == 0) incrementCoarseX()
            if (cycle == 256) incrementY()
            if (cycle == 257) transferHorizontalAddress()
            if (scanline == 261 && cycle in 280..304) transferVerticalAddress()
        }
        cycle++
        if (cycle >= 341) {
            cycle = 0
            scanline++
            if (scanline >= 262) {
                scanline = 0
                frameComplete = true
            }
        }
    }

    fun cpuRead(register: Int): Int = when (register and 7) {
        2 -> {
            val result = status
            status = status and 0x7F
            writeLatch = false
            result
        }

        4 -> oam[oamAddress].toInt() and 0xFF
        7 -> readData()
        else -> 0
    }

    fun cpuWrite(register: Int, value: Int) {
        val data = value and 0xFF
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
                oam[oamAddress] = data.toByte(); oamAddress = (oamAddress + 1) and 0xFF
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
        var i = 0
        while (i < 256) {
            oam[(oamAddress + i) and 0xFF] = page[i]
            i++
        }
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
        var x = 0
        while (x < 256) {
            bgOpaque[x] = false; framebuffer[y * 256 + x] = Palette.COLORS[ppuRead(0x3F00) and 0x3F]; x++
        }
        if (bgEnabled) renderBackground(y)
        if (spritesEnabled) renderSprites(y)
    }

    private fun renderBackground(y: Int) {
        val scrollX = (((v and 0x001F) shl 3) + fineX + if ((v and 0x0400) != 0) 256 else 0) and 0x1FF
        val scrollY =
            ((((v shr 5) and 0x1F) shl 3) + ((v shr 12) and 7) + if ((v and 0x0800) != 0) 256 else 0) and 0x1FF
        val patternBase = if ((ctrl and 0x10) != 0) 0x1000 else 0
        var x = 0
        while (x < 256) {
            val sx = (x + scrollX) and 0x1FF
            val sy = scrollY
            val nt = ((sx shr 8) and 1) or (((sy shr 8) and 1) shl 1)
            val tx = (sx shr 3) and 31
            val ty = (sy shr 3) and 31
            val fineY = sy and 7
            val ntBase = 0x2000 + nt * 0x400
            val tile = ppuRead(ntBase + ty * 32 + tx)
            val attr = ppuRead(ntBase + 0x3C0 + (ty shr 2) * 8 + (tx shr 2))
            val shift = ((ty and 2) shl 1) or (tx and 2)
            val palette = (attr shr shift) and 3
            val lo = ppuRead(patternBase + tile * 16 + fineY)
            val hi = ppuRead(patternBase + tile * 16 + fineY + 8)
            val bit = 7 - (sx and 7)
            val color = (((hi shr bit) and 1) shl 1) or ((lo shr bit) and 1)
            if (color != 0) {
                bgOpaque[x] = true
                framebuffer[y * 256 + x] = Palette.COLORS[ppuRead(0x3F00 + palette * 4 + color) and 0x3F]
            }
            x++
        }
    }

    private fun renderSprites(y: Int) {
        val spriteHeight = if ((ctrl and 0x20) != 0) 16 else 8
        val spritePatternBase = if ((ctrl and 0x08) != 0) 0x1000 else 0
        var selected = 0
        var i = 0
        while (i < 64 && selected < 8) {
            val base = i * 4
            val sy = (oam[base].toInt() and 0xFF) + 1
            if (y >= sy && y < sy + spriteHeight) {
                scanlineSprites[selected] = i
                selected++
            }
            i++
        }
        var s = selected - 1
        while (s >= 0) {
            renderSprite(scanlineSprites[s], y, spriteHeight, spritePatternBase)
            s--
        }
    }

    private fun renderSprite(i: Int, y: Int, spriteHeight: Int, spritePatternBase: Int) {
        val base = i * 4
        val sy = (oam[base].toInt() and 0xFF) + 1
        val tile = oam[base + 1].toInt() and 0xFF
        val attr = oam[base + 2].toInt() and 0xFF
        val sx = oam[base + 3].toInt() and 0xFF
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
            if (color != 0 && x in 0..255) {
                if (i == 0 && bgOpaque[x] && x != 255) status = status or 0x40
                if ((attr and 0x20) == 0 || !bgOpaque[x]) {
                    val pal = attr and 3
                    framebuffer[y * 256 + x] = Palette.COLORS[ppuRead(0x3F10 + pal * 4 + color) and 0x3F]
                }
            }
            px++
        }
    }
}
