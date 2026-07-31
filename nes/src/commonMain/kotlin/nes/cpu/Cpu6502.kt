package nes.cpu

import nes.util.low16Bits
import nes.util.low8Bits
import nes.util.pageBase

class Cpu6502(
    private val bus: CpuBus
) {
    companion object {
        const val C = 0x01
        const val Z = 0x02
        const val I = 0x04
        const val D = 0x08
        const val B = 0x10
        const val U = 0x20
        const val V = 0x40
        const val N = 0x80

        const val OP_BRK = 0x00
        const val OP_ORA_INDX = 0x01
        const val OP_ORA_ZP = 0x05
        const val OP_ASL_ZP = 0x06
        const val OP_PHP = 0x08
        const val OP_ORA_IMM = 0x09
        const val OP_ASL_ACC = 0x0A
        const val OP_ORA_ABS = 0x0D
        const val OP_ASL_ABS = 0x0E
        const val OP_BPL = 0x10
        const val OP_ORA_INDY = 0x11
        const val OP_ORA_ZPX = 0x15
        const val OP_ASL_ZPX = 0x16
        const val OP_CLC = 0x18
        const val OP_ORA_ABSY = 0x19
        const val OP_ORA_ABSX = 0x1D
        const val OP_ASL_ABSX = 0x1E
        const val OP_JSR_ABS = 0x20
        const val OP_AND_INDX = 0x21
        const val OP_BIT_ZP = 0x24
        const val OP_AND_ZP = 0x25
        const val OP_ROL_ZP = 0x26
        const val OP_PLP = 0x28
        const val OP_AND_IMM = 0x29
        const val OP_ROL_ACC = 0x2A
        const val OP_BIT_ABS = 0x2C
        const val OP_AND_ABS = 0x2D
        const val OP_ROL_ABS = 0x2E
        const val OP_BMI = 0x30
        const val OP_AND_INDY = 0x31
        const val OP_AND_ZPX = 0x35
        const val OP_ROL_ZPX = 0x36
        const val OP_SEC = 0x38
        const val OP_AND_ABSY = 0x39
        const val OP_AND_ABSX = 0x3D
        const val OP_ROL_ABSX = 0x3E
        const val OP_RTI = 0x40
        const val OP_EOR_INDX = 0x41
        const val OP_EOR_ZP = 0x45
        const val OP_LSR_ZP = 0x46
        const val OP_PHA = 0x48
        const val OP_EOR_IMM = 0x49
        const val OP_LSR_ACC = 0x4A
        const val OP_JMP_ABS = 0x4C
        const val OP_EOR_ABS = 0x4D
        const val OP_LSR_ABS = 0x4E
        const val OP_BVC = 0x50
        const val OP_EOR_INDY = 0x51
        const val OP_EOR_ZPX = 0x55
        const val OP_LSR_ZPX = 0x56
        const val OP_CLI = 0x58
        const val OP_EOR_ABSY = 0x59
        const val OP_EOR_ABSX = 0x5D
        const val OP_LSR_ABSX = 0x5E
        const val OP_RTS = 0x60
        const val OP_ADC_INDX = 0x61
        const val OP_ADC_ZP = 0x65
        const val OP_ROR_ZP = 0x66
        const val OP_PLA = 0x68
        const val OP_ADC_IMM = 0x69
        const val OP_ROR_ACC = 0x6A
        const val OP_JMP_IND = 0x6C
        const val OP_ADC_ABS = 0x6D
        const val OP_ROR_ABS = 0x6E
        const val OP_BVS = 0x70
        const val OP_ADC_INDY = 0x71
        const val OP_ADC_ZPX = 0x75
        const val OP_ROR_ZPX = 0x76
        const val OP_SEI = 0x78
        const val OP_ADC_ABSY = 0x79
        const val OP_ADC_ABSX = 0x7D
        const val OP_ROR_ABSX = 0x7E
        const val OP_STA_INDX = 0x81
        const val OP_STY_ZP = 0x84
        const val OP_STA_ZP = 0x85
        const val OP_STX_ZP = 0x86
        const val OP_DEY = 0x88
        const val OP_TXA = 0x8A
        const val OP_STY_ABS = 0x8C
        const val OP_STA_ABS = 0x8D
        const val OP_STX_ABS = 0x8E
        const val OP_BCC = 0x90
        const val OP_STA_INDY = 0x91
        const val OP_STY_ZPX = 0x94
        const val OP_STA_ZPX = 0x95
        const val OP_STX_ZPY = 0x96
        const val OP_TYA = 0x98
        const val OP_STA_ABSY = 0x99
        const val OP_TXS = 0x9A
        const val OP_STA_ABSX = 0x9D
        const val OP_LDY_IMM = 0xA0
        const val OP_LDA_INDX = 0xA1
        const val OP_LDX_IMM = 0xA2
        const val OP_LDY_ZP = 0xA4
        const val OP_LDA_ZP = 0xA5
        const val OP_LDX_ZP = 0xA6
        const val OP_TAY = 0xA8
        const val OP_LDA_IMM = 0xA9
        const val OP_TAX = 0xAA
        const val OP_LDY_ABS = 0xAC
        const val OP_LDA_ABS = 0xAD
        const val OP_LDX_ABS = 0xAE
        const val OP_BCS = 0xB0
        const val OP_LDA_INDY = 0xB1
        const val OP_LDY_ZPX = 0xB4
        const val OP_LDA_ZPX = 0xB5
        const val OP_LDX_ZPY = 0xB6
        const val OP_CLV = 0xB8
        const val OP_LDA_ABSY = 0xB9
        const val OP_TSX = 0xBA
        const val OP_LDY_ABSX = 0xBC
        const val OP_LDA_ABSX = 0xBD
        const val OP_LDX_ABSY = 0xBE
        const val OP_CPY_IMM = 0xC0
        const val OP_CMP_INDX = 0xC1
        const val OP_CPY_ZP = 0xC4
        const val OP_CMP_ZP = 0xC5
        const val OP_DEC_ZP = 0xC6
        const val OP_INY = 0xC8
        const val OP_CMP_IMM = 0xC9
        const val OP_DEX = 0xCA
        const val OP_CPY_ABS = 0xCC
        const val OP_CMP_ABS = 0xCD
        const val OP_DEC_ABS = 0xCE
        const val OP_BNE = 0xD0
        const val OP_CMP_INDY = 0xD1
        const val OP_CMP_ZPX = 0xD5
        const val OP_DEC_ZPX = 0xD6
        const val OP_CLD = 0xD8
        const val OP_CMP_ABSY = 0xD9
        const val OP_CMP_ABSX = 0xDD
        const val OP_DEC_ABSX = 0xDE
        const val OP_CPX_IMM = 0xE0
        const val OP_SBC_INDX = 0xE1
        const val OP_CPX_ZP = 0xE4
        const val OP_SBC_ZP = 0xE5
        const val OP_INC_ZP = 0xE6
        const val OP_INX = 0xE8
        const val OP_SBC_IMM = 0xE9
        const val OP_NOP = 0xEA
        const val OP_SBC_IMM_UNOFFICIAL = 0xEB
        const val OP_CPX_ABS = 0xEC
        const val OP_SBC_ABS = 0xED
        const val OP_INC_ABS = 0xEE
        const val OP_BEQ = 0xF0
        const val OP_SBC_INDY = 0xF1
        const val OP_SBC_ZPX = 0xF5
        const val OP_INC_ZPX = 0xF6
        const val OP_SED = 0xF8
        const val OP_SBC_ABSY = 0xF9
        const val OP_SBC_ABSX = 0xFD
        const val OP_INC_ABSX = 0xFE
    }

    var pc = 0
        private set
    var a = 0
        private set
    var x = 0
        private set
    var y = 0
        private set
    var sp = 0xFD
        private set
    var status = I or U
        private set
    var totalCycles = 0L
        private set

    private var nmiPending = false
    private var irqLine = false

    /**
     * Resets the CPU registers, status flags, pending interrupts, cycle count, and program counter.
     * The reset vector at `$FFFC/$FFFD` supplies the initial program counter.
     */
    fun reset() {
        bus.reset()
        a = 0
        x = 0
        y = 0
        sp = 0xFD
        status = I or U
        pc = read16(0xFFFC)
        totalCycles = 7
        nmiPending = false
        irqLine = false
    }

    /**
     * Queues a non-maskable interrupt to be serviced before the next opcode is executed.
     */
    fun requestNmi() {
        nmiPending = true
    }

    /**
     * Updates the level-sensitive IRQ input sampled before the next opcode.
     */
    fun setIrqLine(asserted: Boolean) {
        irqLine = asserted
    }

    /**
     * Executes one CPU step, servicing a pending interrupt first when applicable.
     *
     * @return the number of CPU cycles consumed, including any pending DMA stall cycles.
     */
    fun step(): Int {
        val pendingStallCycles = bus.consumeDmaCycles()
        if (pendingStallCycles > 0) {
            totalCycles += pendingStallCycles.toLong()
            return pendingStallCycles
        }
        val instructionCycles = when {
            nmiPending -> {
                nmiPending = false
                interrupt(0xFFFA, false)
            }
            irqLine && !flag(I) -> interrupt(0xFFFE, false)
            else -> execute(fetchByte())
        }
        val cycles = instructionCycles + bus.consumeDmaCycles()
        totalCycles += cycles.toLong()
        return cycles
    }

    /**
     * Decodes and executes a single opcode that has already been fetched from memory.
     *
     * @param op the opcode byte to execute.
     * @return the base CPU cycles consumed by the instruction, including page-cross penalties.
     */
    private fun execute(op: Int): Int {
        return when (op) {
            OP_LDA_IMM -> {
                a = fetchByte()
                zn(a)
                2
            }

            OP_LDA_ZP -> {
                a = read(zp())
                zn(a)
                3
            }

            OP_LDA_ZPX -> {
                a = read(zpx())
                zn(a)
                4
            }

            OP_LDA_ABS -> {
                a = read(abs())
                zn(a)
                4
            }

            OP_LDA_ABSX -> {
                val r = absxWithPageCrossPenalty()
                a = read(resultAddr(r))
                zn(a)
                4 + resultPage(r)
            }

            OP_LDA_ABSY -> {
                val r = absyWithPageCrossPenalty()
                a = read(resultAddr(r))
                zn(a)
                4 + resultPage(r)
            }

            OP_LDA_INDX -> {
                a = read(indx())
                zn(a)
                6
            }

            OP_LDA_INDY -> {
                val r = indyWithPageCrossPenalty()
                a = read(resultAddr(r))
                zn(a)
                5 + resultPage(r)
            }

            OP_LDX_IMM -> {
                x = fetchByte()
                zn(x)
                2
            }

            OP_LDX_ZP -> {
                x = read(zp())
                zn(x)
                3
            }

            OP_LDX_ZPY -> {
                x = read(zpy())
                zn(x)
                4
            }

            OP_LDX_ABS -> {
                x = read(abs())
                zn(x)
                4
            }

            OP_LDX_ABSY -> {
                val r = absyWithPageCrossPenalty()
                x = read(resultAddr(r))
                zn(x)
                4 + resultPage(r)
            }

            OP_LDY_IMM -> {
                y = fetchByte()
                zn(y)
                2
            }

            OP_LDY_ZP -> {
                y = read(zp())
                zn(y)
                3
            }

            OP_LDY_ZPX -> {
                y = read(zpx())
                zn(y)
                4
            }

            OP_LDY_ABS -> {
                y = read(abs())
                zn(y)
                4
            }

            OP_LDY_ABSX -> {
                val r = absxWithPageCrossPenalty()
                y = read(resultAddr(r))
                zn(y)
                4 + resultPage(r)
            }

            OP_STA_ZP -> {
                write(zp(), a)
                3
            }

            OP_STA_ZPX -> {
                write(zpx(), a)
                4
            }

            OP_STA_ABS -> {
                write(abs(), a)
                4
            }

            OP_STA_ABSX -> {
                write(absx(), a)
                5
            }

            OP_STA_ABSY -> {
                write(absy(), a)
                5
            }

            OP_STA_INDX -> {
                write(indx(), a)
                6
            }

            OP_STA_INDY -> {
                write(indy(), a)
                6
            }

            OP_STX_ZP -> {
                write(zp(), x)
                3
            }

            OP_STX_ZPY -> {
                write(zpy(), x)
                4
            }

            OP_STX_ABS -> {
                write(abs(), x)
                4
            }

            OP_STY_ZP -> {
                write(zp(), y)
                3
            }

            OP_STY_ZPX -> {
                write(zpx(), y)
                4
            }

            OP_STY_ABS -> {
                write(abs(), y)
                4
            }

            OP_TAX -> {
                x = a
                zn(x)
                2
            }

            OP_TAY -> {
                y = a
                zn(y)
                2
            }

            OP_TXA -> {
                a = x
                zn(a)
                2
            }

            OP_TYA -> {
                a = y
                zn(a)
                2
            }

            OP_TSX -> {
                x = sp
                zn(x)
                2
            }

            OP_TXS -> {
                sp = x
                2
            }

            OP_PHA -> {
                push(a)
                3
            }

            OP_PLA -> {
                a = pull()
                zn(a)
                4
            }

            OP_PHP -> {
                push(status or B or U)
                3
            }

            OP_PLP -> {
                status = (pull() or U) and B.inv()
                4
            }

            OP_ADC_IMM -> {
                adc(fetchByte())
                2
            }

            OP_ADC_ZP -> {
                adc(read(zp()))
                3
            }

            OP_ADC_ZPX -> {
                adc(read(zpx()))
                4
            }

            OP_ADC_ABS -> {
                adc(read(abs()))
                4
            }

            OP_ADC_ABSX -> {
                val r = absxWithPageCrossPenalty()
                adc(read(resultAddr(r)))
                4 + resultPage(r)
            }

            OP_ADC_ABSY -> {
                val r = absyWithPageCrossPenalty()
                adc(read(resultAddr(r)))
                4 + resultPage(r)
            }

            OP_ADC_INDX -> {
                adc(read(indx()))
                6
            }

            OP_ADC_INDY -> {
                val r = indyWithPageCrossPenalty()
                adc(read(resultAddr(r)))
                5 + resultPage(r)
            }

            OP_SBC_IMM, OP_SBC_IMM_UNOFFICIAL -> {
                sbc(fetchByte())
                2
            }

            OP_SBC_ZP -> {
                sbc(read(zp()))
                3
            }

            OP_SBC_ZPX -> {
                sbc(read(zpx()))
                4
            }

            OP_SBC_ABS -> {
                sbc(read(abs()))
                4
            }

            OP_SBC_ABSX -> {
                val r = absxWithPageCrossPenalty()
                sbc(read(resultAddr(r)))
                4 + resultPage(r)
            }

            OP_SBC_ABSY -> {
                val r = absyWithPageCrossPenalty()
                sbc(read(resultAddr(r)))
                4 + resultPage(r)
            }

            OP_SBC_INDX -> {
                sbc(read(indx()))
                6
            }

            OP_SBC_INDY -> {
                val r = indyWithPageCrossPenalty()
                sbc(read(resultAddr(r)))
                5 + resultPage(r)
            }

            OP_AND_IMM -> {
                a = a and fetchByte()
                zn(a)
                2
            }

            OP_AND_ZP -> {
                a = a and read(zp())
                zn(a)
                3
            }

            OP_AND_ZPX -> {
                a = a and read(zpx())
                zn(a)
                4
            }

            OP_AND_ABS -> {
                a = a and read(abs())
                zn(a)
                4
            }

            OP_AND_ABSX -> {
                val r = absxWithPageCrossPenalty()
                a = a and read(resultAddr(r))
                zn(a)
                4 + resultPage(r)
            }

            OP_AND_ABSY -> {
                val r = absyWithPageCrossPenalty()
                a = a and read(resultAddr(r))
                zn(a)
                4 + resultPage(r)
            }

            OP_AND_INDX -> {
                a = a and read(indx())
                zn(a)
                6
            }

            OP_AND_INDY -> {
                val r = indyWithPageCrossPenalty()
                a = a and read(resultAddr(r))
                zn(a)
                5 + resultPage(r)
            }

            OP_ORA_IMM -> {
                a = a or fetchByte()
                zn(a)
                2
            }

            OP_ORA_ZP -> {
                a = a or read(zp())
                zn(a)
                3
            }

            OP_ORA_ZPX -> {
                a = a or read(zpx())
                zn(a)
                4
            }

            OP_ORA_ABS -> {
                a = a or read(abs())
                zn(a)
                4
            }

            OP_ORA_ABSX -> {
                val r = absxWithPageCrossPenalty()
                a = a or read(resultAddr(r))
                zn(a)
                4 + resultPage(r)
            }

            OP_ORA_ABSY -> {
                val r = absyWithPageCrossPenalty()
                a = a or read(resultAddr(r))
                zn(a)
                4 + resultPage(r)
            }

            OP_ORA_INDX -> {
                a = a or read(indx())
                zn(a)
                6
            }

            OP_ORA_INDY -> {
                val r = indyWithPageCrossPenalty()
                a = a or read(resultAddr(r))
                zn(a)
                5 + resultPage(r)
            }

            OP_EOR_IMM -> {
                a = a xor fetchByte()
                zn(a)
                2
            }

            OP_EOR_ZP -> {
                a = a xor read(zp())
                zn(a)
                3
            }

            OP_EOR_ZPX -> {
                a = a xor read(zpx())
                zn(a)
                4
            }

            OP_EOR_ABS -> {
                a = a xor read(abs())
                zn(a)
                4
            }

            OP_EOR_ABSX -> {
                val r = absxWithPageCrossPenalty()
                a = a xor read(resultAddr(r))
                zn(a)
                4 + resultPage(r)
            }

            OP_EOR_ABSY -> {
                val r = absyWithPageCrossPenalty()
                a = a xor read(resultAddr(r))
                zn(a)
                4 + resultPage(r)
            }

            OP_EOR_INDX -> {
                a = a xor read(indx())
                zn(a)
                6
            }

            OP_EOR_INDY -> {
                val r = indyWithPageCrossPenalty()
                a = a xor read(resultAddr(r))
                zn(a)
                5 + resultPage(r)
            }

            OP_CMP_IMM -> {
                cmp(a, fetchByte())
                2
            }

            OP_CMP_ZP -> {
                cmp(a, read(zp()))
                3
            }

            OP_CMP_ZPX -> {
                cmp(a, read(zpx()))
                4
            }

            OP_CMP_ABS -> {
                cmp(a, read(abs()))
                4
            }

            OP_CMP_ABSX -> {
                val r = absxWithPageCrossPenalty()
                cmp(a, read(resultAddr(r)))
                4 + resultPage(r)
            }

            OP_CMP_ABSY -> {
                val r = absyWithPageCrossPenalty()
                cmp(a, read(resultAddr(r)))
                4 + resultPage(r)
            }

            OP_CMP_INDX -> {
                cmp(a, read(indx()))
                6
            }

            OP_CMP_INDY -> {
                val r = indyWithPageCrossPenalty()
                cmp(a, read(resultAddr(r)))
                5 + resultPage(r)
            }

            OP_CPX_IMM -> {
                cmp(x, fetchByte())
                2
            }

            OP_CPX_ZP -> {
                cmp(x, read(zp()))
                3
            }

            OP_CPX_ABS -> {
                cmp(x, read(abs()))
                4
            }

            OP_CPY_IMM -> {
                cmp(y, fetchByte())
                2
            }

            OP_CPY_ZP -> {
                cmp(y, read(zp()))
                3
            }

            OP_CPY_ABS -> {
                cmp(y, read(abs()))
                4
            }

            OP_INC_ZP -> {
                inc(zp())
                5
            }

            OP_INC_ZPX -> {
                inc(zpx())
                6
            }

            OP_INC_ABS -> {
                inc(abs())
                6
            }

            OP_INC_ABSX -> {
                inc(absx())
                7
            }

            OP_DEC_ZP -> {
                dec(zp())
                5
            }

            OP_DEC_ZPX -> {
                dec(zpx())
                6
            }

            OP_DEC_ABS -> {
                dec(abs())
                6
            }

            OP_DEC_ABSX -> {
                dec(absx())
                7
            }

            OP_INX -> {
                x = (x + 1).low8Bits()
                zn(x)
                2
            }

            OP_INY -> {
                y = (y + 1).low8Bits()
                zn(y)
                2
            }

            OP_DEX -> {
                x = (x - 1).low8Bits()
                zn(x)
                2
            }

            OP_DEY -> {
                y = (y - 1).low8Bits()
                zn(y)
                2
            }

            OP_ASL_ACC -> {
                a = aslValue(a)
                2
            }

            OP_ASL_ZP -> {
                asl(zp())
                5
            }

            OP_ASL_ZPX -> {
                asl(zpx())
                6
            }

            OP_ASL_ABS -> {
                asl(abs())
                6
            }

            OP_ASL_ABSX -> {
                asl(absx())
                7
            }

            OP_LSR_ACC -> {
                a = lsrValue(a)
                2
            }

            OP_LSR_ZP -> {
                lsr(zp())
                5
            }

            OP_LSR_ZPX -> {
                lsr(zpx())
                6
            }

            OP_LSR_ABS -> {
                lsr(abs())
                6
            }

            OP_LSR_ABSX -> {
                lsr(absx())
                7
            }

            OP_ROL_ACC -> {
                a = rolValue(a)
                2
            }

            OP_ROL_ZP -> {
                rol(zp())
                5
            }

            OP_ROL_ZPX -> {
                rol(zpx())
                6
            }

            OP_ROL_ABS -> {
                rol(abs())
                6
            }

            OP_ROL_ABSX -> {
                rol(absx())
                7
            }

            OP_ROR_ACC -> {
                a = rorValue(a)
                2
            }

            OP_ROR_ZP -> {
                ror(zp())
                5
            }

            OP_ROR_ZPX -> {
                ror(zpx())
                6
            }

            OP_ROR_ABS -> {
                ror(abs())
                6
            }

            OP_ROR_ABSX -> {
                ror(absx())
                7
            }

            OP_BIT_ZP -> {
                bit(read(zp()))
                3
            }

            OP_BIT_ABS -> {
                bit(read(abs()))
                4
            }

            OP_JMP_ABS -> {
                pc = abs()
                3
            }

            OP_JMP_IND -> {
                pc = jmpIndirect()
                5
            }

            OP_JSR_ABS -> {
                val addr = abs()
                push16((pc - 1).low16Bits())
                pc = addr
                6
            }

            OP_RTS -> {
                pc = (pull16() + 1).low16Bits()
                6
            }

            OP_RTI -> {
                status = (pull() or U) and B.inv()
                pc = pull16()
                6
            }

            OP_BRK -> {
                pc = (pc + 1).low16Bits()
                interrupt(0xFFFE, true)
            }

            OP_BPL -> {
                branch(!flag(N))
            }

            OP_BMI -> {
                branch(flag(N))
            }

            OP_BVC -> {
                branch(!flag(V))
            }

            OP_BVS -> {
                branch(flag(V))
            }

            OP_BCC -> {
                branch(!flag(C))
            }

            OP_BCS -> {
                branch(flag(C))
            }

            OP_BNE -> {
                branch(!flag(Z))
            }

            OP_BEQ -> {
                branch(flag(Z))
            }

            OP_CLC -> {
                set(C, false)
                2
            }

            OP_SEC -> {
                set(C, true)
                2
            }

            OP_CLI -> {
                set(I, false)
                2
            }

            OP_SEI -> {
                set(I, true)
                2
            }

            OP_CLV -> {
                set(V, false)
                2
            }

            OP_CLD -> {
                set(D, false)
                2
            }

            OP_SED -> {
                set(D, true)
                2
            }

            OP_NOP -> {
                2
            }

            else -> error("Unsupported unofficial opcode 0x${op.toString(16).padStart(2, '0')}")
        }
    }

    /**
     * Reads one byte from the CPU bus at the supplied 16-bit address.
     */
    private fun read(addr: Int): Int {
        return bus.read(addr)
    }

    /**
     * Writes one byte to the CPU bus at the supplied 16-bit address.
     */
    private fun write(addr: Int, value: Int) {
        bus.write(addr, value)
    }

    /**
     * Reads the next instruction byte and advances the program counter.
     */
    private fun fetchByte(): Int {
        val v = read(pc)
        pc = (pc + 1).low16Bits()
        return v
    }

    /**
     * Resolves zero-page addressing from the next instruction byte.
     */
    private fun zp(): Int {
        return fetchByte()
    }

    /**
     * Resolves zero-page,X addressing with 8-bit zero-page wraparound.
     */
    private fun zpx(): Int {
        return (fetchByte() + x).low8Bits()
    }

    /**
     * Resolves zero-page,Y addressing with 8-bit zero-page wraparound.
     */
    private fun zpy(): Int {
        return (fetchByte() + y).low8Bits()
    }

    /**
     * Resolves absolute addressing from the next two instruction bytes in little-endian order.
     */
    private fun abs(): Int {
        val lo = fetchByte()
        val hi = fetchByte()
        return lo or (hi shl 8)
    }

    /**
     * Resolves absolute,X addressing.
     */
    private fun absx(): Int {
        return (abs() + x).low16Bits()
    }

    /**
     * Resolves absolute,X addressing and reports a page-crossing cycle penalty.
     */
    private fun absxWithPageCrossPenalty(): Int {
        val b = abs()
        val a = (b + x).low16Bits()
        return addressWithPageCrossPenalty(a, b.pageBase() != a.pageBase())
    }

    /**
     * Resolves absolute,Y addressing.
     */
    private fun absy(): Int {
        return (abs() + y).low16Bits()
    }

    /**
     * Resolves absolute,Y addressing and reports a page-crossing cycle penalty.
     */
    private fun absyWithPageCrossPenalty(): Int {
        val b = abs()
        val a = (b + y).low16Bits()
        return addressWithPageCrossPenalty(a, b.pageBase() != a.pageBase())
    }

    /**
     * Resolves indexed-indirect `(operand,X)` addressing through zero-page pointer wraparound.
     */
    private fun indx(): Int {
        val p = (fetchByte() + x).low8Bits()
        return read(p) or (read((p + 1).low8Bits()) shl 8)
    }

    /**
     * Resolves indirect-indexed `(operand),Y` addressing.
     */
    private fun indy(): Int {
        val p = fetchByte()
        val b = read(p) or (read((p + 1).low8Bits()) shl 8)
        return (b + y).low16Bits()
    }

    /**
     * Resolves indirect-indexed `(operand),Y` addressing and reports page crossing.
     */
    private fun indyWithPageCrossPenalty(): Int {
        val p = fetchByte()
        val b = read(p) or (read((p + 1).low8Bits()) shl 8)
        val a = (b + y).low16Bits()
        return addressWithPageCrossPenalty(a, b.pageBase() != a.pageBase())
    }

    private fun addressWithPageCrossPenalty(address: Int, pageCrossed: Boolean): Int {
        return address or ((if (pageCrossed) 1 else 0) shl 16)
    }

    private fun resultAddr(result: Int): Int {
        return result and 0xFFFF
    }

    private fun resultPage(result: Int): Int {
        return result ushr 16
    }

    /**
     * Reads a 16-bit little-endian value from the CPU bus.
     */
    private fun read16(addr: Int): Int {
        return read(addr) or (read((addr + 1).low16Bits()) shl 8)
    }

    /**
     * Resolves `JMP (addr)` using the 6502 page-wrap hardware bug for the high byte fetch.
     */
    private fun jmpIndirect(): Int {
        val p = abs()
        return read(p) or (read(p.pageBase() or ((p + 1).low8Bits())) shl 8)
    }

    /**
     * Checks whether a status-register flag is currently set.
     */
    private fun flag(f: Int): Boolean {
        return (status and f) != 0
    }

    /**
     * Sets or clears a status-register flag while keeping the unused status bit set.
     */
    private fun set(f: Int, on: Boolean) {
        status = if (on) {
            status or f
        } else {
            status and f.inv()
        }
    }

    /**
     * Updates the zero and negative flags from an 8-bit result value.
     */
    private fun zn(v: Int) {
        val value = v.low8Bits()
        status = status and (Z or N).inv()
        if (value == 0) status = status or Z
        status = status or (value and N)
    }

    /**
     * Pushes one byte onto the stack page and decrements the stack pointer.
     */
    private fun push(v: Int) {
        write(0x100 or sp, v)
        sp = (sp - 1).low8Bits()
    }

    /**
     * Increments the stack pointer and pulls one byte from the stack page.
     */
    private fun pull(): Int {
        sp = (sp + 1).low8Bits()
        return read(0x100 or sp)
    }

    /**
     * Pushes a 16-bit value onto the stack in 6502 order: high byte first, then low byte.
     */
    private fun push16(v: Int) {
        push(v shr 8)
        push(v)
    }

    /**
     * Pulls a 16-bit little-endian value from the stack.
     */
    private fun pull16(): Int {
        val lo = pull()
        val hi = pull()
        return lo or (hi shl 8)
    }

    /**
     * Handles BRK, IRQ, or NMI entry by pushing CPU state, setting interrupt disable, and loading the vector.
     */
    private fun interrupt(vector: Int, brk: Boolean): Int {
        push16(pc)
        push((status or U or if (brk) B else 0) and if (brk) 0xFF else B.inv())
        set(I, true)
        pc = read16(vector)
        return 7
    }

    /**
     * Adds a byte plus carry to the accumulator and updates carry, overflow, zero, and negative flags.
     */
    private fun adc(v: Int) {
        val sum = a + v + if (flag(C)) 1 else 0
        val result = sum.low8Bits()
        var flags = result and N
        if (sum > 0xFF) flags = flags or C
        if (((a xor sum) and (v xor sum) and 0x80) != 0) flags = flags or V
        if (result == 0) flags = flags or Z
        status = (status and (C or Z or V or N).inv()) or flags
        a = result
    }

    /**
     * Subtracts a byte from the accumulator using 6502 carry semantics.
     */
    private fun sbc(v: Int) {
        adc(v xor 0xFF)
    }

    /**
     * Compares a register value with a byte and updates carry, zero, and negative flags.
     */
    private fun cmp(r: Int, v: Int) {
        val result = (r - v).low8Bits()
        var flags = result and N
        if (r >= v) flags = flags or C
        if (result == 0) flags = flags or Z
        status = (status and (C or Z or N).inv()) or flags
    }

    /**
     * Performs the BIT test against the accumulator and copies bits 6 and 7 into overflow and negative flags.
     */
    private fun bit(v: Int) {
        var flags = v and (V or N)
        if ((a and v) == 0) flags = flags or Z
        status = (status and (Z or V or N).inv()) or flags
    }

    /**
     * Increments the byte at an address and updates zero and negative flags.
     */
    private fun inc(addr: Int) {
        val v = (read(addr) + 1).low8Bits()
        write(addr, v)
        zn(v)
    }

    /**
     * Decrements the byte at an address and updates zero and negative flags.
     */
    private fun dec(addr: Int) {
        val v = (read(addr) - 1).low8Bits()
        write(addr, v)
        zn(v)
    }

    /**
     * Arithmetic-shifts the byte at an address left and writes the result back.
     */
    private fun asl(addr: Int) {
        val v = aslValue(read(addr))
        write(addr, v)
    }

    /**
     * Logical-shifts the byte at an address right and writes the result back.
     */
    private fun lsr(addr: Int) {
        val v = lsrValue(read(addr))
        write(addr, v)
    }

    /**
     * Rotates the byte at an address left through carry and writes the result back.
     */
    private fun rol(addr: Int) {
        val v = rolValue(read(addr))
        write(addr, v)
    }

    /**
     * Rotates the byte at an address right through carry and writes the result back.
     */
    private fun ror(addr: Int) {
        val v = rorValue(read(addr))
        write(addr, v)
    }

    /**
     * Arithmetic-shifts an 8-bit value left and updates carry, zero, and negative flags.
     */
    private fun aslValue(v: Int): Int {
        set(C, (v and 0x80) != 0)
        val r = (v shl 1).low8Bits()
        zn(r)
        return r
    }

    /**
     * Logical-shifts an 8-bit value right and updates carry, zero, and negative flags.
     */
    private fun lsrValue(v: Int): Int {
        set(C, (v and 1) != 0)
        val r = (v shr 1).low8Bits()
        zn(r)
        return r
    }

    /**
     * Rotates an 8-bit value left through carry and updates carry, zero, and negative flags.
     */
    private fun rolValue(v: Int): Int {
        val c = if (flag(C)) 1 else 0
        set(C, (v and 0x80) != 0)
        val r = ((v shl 1) or c).low8Bits()
        zn(r)
        return r
    }

    /**
     * Rotates an 8-bit value right through carry and updates carry, zero, and negative flags.
     */
    private fun rorValue(v: Int): Int {
        val c = if (flag(C)) 0x80 else 0
        set(C, (v and 1) != 0)
        val r = ((v shr 1) or c).low8Bits()
        zn(r)
        return r
    }

    /**
     * Applies a signed relative branch when the condition is true and returns the instruction cycle count.
     */
    private fun branch(cond: Boolean): Int {
        val off = fetchByte()
        if (!cond) {
            return 2
        }
        val old = pc
        val signed = if (off < 0x80) off else off - 0x100
        pc = (pc + signed).low16Bits()
        return if (old.pageBase() != pc.pageBase()) {
            4
        } else {
            3
        }
    }

}
