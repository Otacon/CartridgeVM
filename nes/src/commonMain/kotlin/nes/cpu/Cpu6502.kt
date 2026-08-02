package nes.cpu

import nes.util.low16Bits
import nes.util.low8Bits
import nes.util.pageBase

private enum class AddressMode { IMP, ACC, IMM, ZP, ZPX, ZPY, ABS, AX, AY, IND, IX, IY, REL }

private data class DecodedOpcode(val instruction: String, val mode: AddressMode)

private val OPCODES: Array<DecodedOpcode> = run {
    val rows = arrayOf(
        "BRK:IMP ORA:IX KIL:IMP SLO:IX NOP:ZP ORA:ZP ASL:ZP SLO:ZP PHP:IMP ORA:IMM ASL:ACC ANC:IMM NOP:ABS ORA:ABS ASL:ABS SLO:ABS",
        "BPL:REL ORA:IY KIL:IMP SLO:IY NOP:ZPX ORA:ZPX ASL:ZPX SLO:ZPX CLC:IMP ORA:AY NOP:IMP SLO:AY NOP:AX ORA:AX ASL:AX SLO:AX",
        "JSR:ABS AND:IX KIL:IMP RLA:IX BIT:ZP AND:ZP ROL:ZP RLA:ZP PLP:IMP AND:IMM ROL:ACC ANC:IMM BIT:ABS AND:ABS ROL:ABS RLA:ABS",
        "BMI:REL AND:IY KIL:IMP RLA:IY NOP:ZPX AND:ZPX ROL:ZPX RLA:ZPX SEC:IMP AND:AY NOP:IMP RLA:AY NOP:AX AND:AX ROL:AX RLA:AX",
        "RTI:IMP EOR:IX KIL:IMP SRE:IX NOP:ZP EOR:ZP LSR:ZP SRE:ZP PHA:IMP EOR:IMM LSR:ACC ALR:IMM JMP:ABS EOR:ABS LSR:ABS SRE:ABS",
        "BVC:REL EOR:IY KIL:IMP SRE:IY NOP:ZPX EOR:ZPX LSR:ZPX SRE:ZPX CLI:IMP EOR:AY NOP:IMP SRE:AY NOP:AX EOR:AX LSR:AX SRE:AX",
        "RTS:IMP ADC:IX KIL:IMP RRA:IX NOP:ZP ADC:ZP ROR:ZP RRA:ZP PLA:IMP ADC:IMM ROR:ACC ARR:IMM JMP:IND ADC:ABS ROR:ABS RRA:ABS",
        "BVS:REL ADC:IY KIL:IMP RRA:IY NOP:ZPX ADC:ZPX ROR:ZPX RRA:ZPX SEI:IMP ADC:AY NOP:IMP RRA:AY NOP:AX ADC:AX ROR:AX RRA:AX",
        "NOP:IMM STA:IX NOP:IMM SAX:IX STY:ZP STA:ZP STX:ZP SAX:ZP DEY:IMP NOP:IMM TXA:IMP XAA:IMM STY:ABS STA:ABS STX:ABS SAX:ABS",
        "BCC:REL STA:IY KIL:IMP AHX:IY STY:ZPX STA:ZPX STX:ZPY SAX:ZPY TYA:IMP STA:AY TXS:IMP TAS:AY SHY:AX STA:AX SHX:AY AHX:AY",
        "LDY:IMM LDA:IX LDX:IMM LAX:IX LDY:ZP LDA:ZP LDX:ZP LAX:ZP TAY:IMP LDA:IMM TAX:IMP LAX:IMM LDY:ABS LDA:ABS LDX:ABS LAX:ABS",
        "BCS:REL LDA:IY KIL:IMP LAX:IY LDY:ZPX LDA:ZPX LDX:ZPY LAX:ZPY CLV:IMP LDA:AY TSX:IMP LAS:AY LDY:AX LDA:AX LDX:AY LAX:AY",
        "CPY:IMM CMP:IX NOP:IMM DCP:IX CPY:ZP CMP:ZP DEC:ZP DCP:ZP INY:IMP CMP:IMM DEX:IMP AXS:IMM CPY:ABS CMP:ABS DEC:ABS DCP:ABS",
        "BNE:REL CMP:IY KIL:IMP DCP:IY NOP:ZPX CMP:ZPX DEC:ZPX DCP:ZPX CLD:IMP CMP:AY NOP:IMP DCP:AY NOP:AX CMP:AX DEC:AX DCP:AX",
        "CPX:IMM SBC:IX NOP:IMM ISB:IX CPX:ZP SBC:ZP INC:ZP ISB:ZP INX:IMP SBC:IMM NOP:IMP SBC:IMM CPX:ABS SBC:ABS INC:ABS ISB:ABS",
        "BEQ:REL SBC:IY KIL:IMP ISB:IY NOP:ZPX SBC:ZPX INC:ZPX ISB:ZPX SED:IMP SBC:AY NOP:IMP ISB:AY NOP:AX SBC:AX INC:AX ISB:AX",
    )
    rows.flatMap { row ->
        row.split(' ').map { token ->
            val separator = token.indexOf(':')
            DecodedOpcode(token.substring(0, separator), AddressMode.valueOf(token.substring(separator + 1)))
        }
    }.toTypedArray()
}

private val BRANCHES = setOf("BPL", "BMI", "BVC", "BVS", "BCC", "BCS", "BNE", "BEQ")
private val WRITES = setOf("STA", "STX", "STY", "SAX", "AHX", "SHX", "SHY", "TAS")
private val UNSTABLE_WRITES = setOf("AHX", "SHX", "SHY", "TAS")
private val READ_MODIFY_WRITES =
    setOf("ASL", "LSR", "ROL", "ROR", "INC", "DEC", "SLO", "RLA", "SRE", "RRA", "DCP", "ISB")

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
    private var irqPending = false
    private var irqSample = false
    private var halted = false

    fun reset() = reset(softReset = false)

    fun reset(softReset: Boolean) {
        bus.reset()
        totalCycles = -1
        if (softReset) {
            sp = (sp - 3).low8Bits()
            set(I, true)
        } else {
            a = 0
            x = 0
            y = 0
            sp = 0xFD
            status = I or U
        }
        pc = bus.read(0xFFFC) or (bus.read(0xFFFD) shl 8)
        repeat(8) {
            bus.idle(CpuBus.CycleType.RESET)
            totalCycles++
        }
        nmiPending = false
        irqLine = false
        irqPending = false
        irqSample = false
        halted = false
    }

    fun requestNmi() {
        nmiPending = true
    }

    fun setIrqLine(asserted: Boolean) {
        irqLine = asserted
        irqPending = asserted && !flag(I)
    }

    fun sampleIrqLine(asserted: Boolean) {
        irqLine = asserted
        irqPending = irqSample
        irqSample = asserted && !flag(I)
    }

    fun step(): Int {
        val start = totalCycles
        val stalls = bus.consumeDmaCycles()
        if (stalls > 0) {
            repeat(stalls) {
                bus.idle(CpuBus.CycleType.STALL)
                totalCycles++
            }
            return stalls
        }

        when {
            halted -> execute(OPCODES[fetchOpcode()])
            nmiPending -> {
                nmiPending = false
                serviceInterrupt(0xFFFA)
            }
            irqPending -> serviceInterrupt(0xFFFE)
            else -> {
                val opcode = fetchOpcode()
                execute(OPCODES[opcode])
            }
        }
        return (totalCycles - start).toInt()
    }

    private fun execute(opcode: DecodedOpcode) {
        val instruction = opcode.instruction
        val mode = opcode.mode
        when (instruction) {
            "BRK" -> brk()
            "JSR" -> jsr()
            "JMP" -> jump(mode)
            "RTS" -> rts()
            "RTI" -> rti()
            "PHP" -> pushInstruction(status or B or U)
            "PHA" -> pushInstruction(a)
            "PLP" -> {
                impliedRead()
                dummyRead(0x100 or sp)
                status = (pull() and (B or U).inv()) or U
            }
            "PLA" -> {
                impliedRead()
                dummyRead(0x100 or sp)
                a = pull()
                zn(a)
            }
            in BRANCHES -> branch(instruction, fetch())
            in UNSTABLE_WRITES -> unstableStore(instruction, mode)
            in WRITES -> {
                val target = address(mode, write = true)
                lastAddress = target
                write(target, storeValue(instruction))
            }
            "ASL", "LSR", "ROL", "ROR" -> {
                if (mode == AddressMode.ACC) {
                    impliedRead()
                    a = transform(instruction, a)
                } else {
                    modify(address(mode, write = true), instruction)
                }
            }
            in READ_MODIFY_WRITES -> modify(address(mode, write = true), instruction)
            "KIL" -> {
                pc = (pc - 1).low16Bits()
                halted = true
                irqPending = false
                nmiPending = false
            }
            else -> executeReadOrImplied(instruction, mode)
        }
    }

    private fun executeReadOrImplied(instruction: String, mode: AddressMode) {
        if (mode == AddressMode.IMP) {
            impliedRead()
            when (instruction) {
                "CLC" -> set(C, false)
                "SEC" -> set(C, true)
                "CLI" -> set(I, false)
                "SEI" -> set(I, true)
                "CLV" -> set(V, false)
                "CLD" -> set(D, false)
                "SED" -> set(D, true)
                "TAX" -> { x = a; zn(x) }
                "TAY" -> { y = a; zn(y) }
                "TXA" -> { a = x; zn(a) }
                "TYA" -> { a = y; zn(a) }
                "TSX" -> { x = sp; zn(x) }
                "TXS" -> sp = x
                "DEX" -> { x = (x - 1).low8Bits(); zn(x) }
                "DEY" -> { y = (y - 1).low8Bits(); zn(y) }
                "INX" -> { x = (x + 1).low8Bits(); zn(x) }
                "INY" -> { y = (y + 1).low8Bits(); zn(y) }
                "NOP" -> Unit
                else -> error("Unsupported implied instruction $instruction")
            }
            return
        }

        val value = readOperand(mode)
        when (instruction) {
            "ORA" -> { a = a or value; zn(a) }
            "AND" -> { a = a and value; zn(a) }
            "EOR" -> { a = a xor value; zn(a) }
            "ADC" -> adc(value)
            "SBC" -> sbc(value)
            "CMP" -> compare(a, value)
            "CPX" -> compare(x, value)
            "CPY" -> compare(y, value)
            "BIT" -> bit(value)
            "LDA" -> { a = value; zn(a) }
            "LDX" -> { x = value; zn(x) }
            "LDY" -> { y = value; zn(y) }
            "LAX" -> { a = value; x = value; zn(value) }
            "LAS" -> { val result = value and sp; a = result; x = result; sp = result; zn(result) }
            "ANC" -> { a = a and value; zn(a); set(C, flag(N)) }
            "ALR" -> { a = lsrValue(a and value) }
            "ARR" -> arr(value)
            "XAA" -> { a = (a or 0xEE) and x and value; zn(a) }
            "AXS" -> axs(value)
            "NOP" -> Unit
            else -> error("Unsupported read instruction $instruction")
        }
    }

    private fun readOperand(mode: AddressMode): Int = when (mode) {
        AddressMode.IMM -> fetch()
        else -> read(address(mode, write = false))
    }

    private fun address(mode: AddressMode, write: Boolean): Int = when (mode) {
        AddressMode.ZP -> fetch()
        AddressMode.ZPX, AddressMode.ZPY -> {
            val base = fetch()
            dummyRead(base)
            (base + if (mode == AddressMode.ZPX) x else y).low8Bits()
        }
        AddressMode.ABS -> absolute()
        AddressMode.AX, AddressMode.AY -> indexedAbsolute(if (mode == AddressMode.AX) x else y, write)
        AddressMode.IX -> {
            val operand = fetch()
            dummyRead(operand)
            val pointer = (operand + x).low8Bits()
            read(pointer) or (read((pointer + 1).low8Bits()) shl 8)
        }
        AddressMode.IY -> {
            val pointer = fetch()
            val base = read(pointer) or (read((pointer + 1).low8Bits()) shl 8)
            val result = (base + y).low16Bits()
            if (write || base.pageBase() != result.pageBase()) {
                dummyRead(base.pageBase() or result.low8Bits())
            }
            result
        }
        else -> error("Address mode $mode has no memory address")
    }

    private fun indexedAbsolute(index: Int, alwaysDummy: Boolean): Int {
        val base = absolute()
        val result = (base + index).low16Bits()
        if (alwaysDummy || base.pageBase() != result.pageBase()) {
            dummyRead(base.pageBase() or result.low8Bits())
        }
        return result
    }

    private fun modify(address: Int, instruction: String) {
        val old = read(address)
        dummyWrite(address, old)
        val result = when (instruction) {
            "SLO" -> transform("ASL", old).also { a = a or it; zn(a) }
            "RLA" -> transform("ROL", old).also { a = a and it; zn(a) }
            "SRE" -> transform("LSR", old).also { a = a xor it; zn(a) }
            "RRA" -> transform("ROR", old).also(::adc)
            "DCP" -> (old - 1).low8Bits().also { compare(a, it) }
            "ISB" -> (old + 1).low8Bits().also(::sbc)
            else -> transform(instruction, old)
        }
        write(address, result)
    }

    private fun transform(instruction: String, value: Int): Int = when (instruction) {
        "ASL" -> aslValue(value)
        "LSR" -> lsrValue(value)
        "ROL" -> rolValue(value)
        "ROR" -> rorValue(value)
        "INC" -> (value + 1).low8Bits().also(::zn)
        "DEC" -> (value - 1).low8Bits().also(::zn)
        else -> error("Unsupported RMW instruction $instruction")
    }

    private fun storeValue(instruction: String): Int = when (instruction) {
        "STA" -> a
        "STX" -> x
        "STY" -> y
        "SAX" -> a and x
        "AHX" -> a and x and (((lastAddress shr 8) + 1).low8Bits())
        "SHX" -> x and (((lastAddress shr 8) + 1).low8Bits())
        "SHY" -> y and (((lastAddress shr 8) + 1).low8Bits())
        "TAS" -> {
            sp = a and x
            sp and (((lastAddress shr 8) + 1).low8Bits())
        }
        else -> error("Unsupported store instruction $instruction")
    }

    private fun unstableStore(instruction: String, mode: AddressMode) {
        val base = if (mode == AddressMode.IY) {
            val pointer = fetch()
            read(pointer) or (read((pointer + 1).low8Bits()) shl 8)
        } else {
            absolute()
        }
        val index = if (mode == AddressMode.AX) x else y
        val target = (base + index).low16Bits()
        dummyRead(base.pageBase() or target.low8Bits())
        val valueRegister = when (instruction) {
            "SHY" -> y
            "SHX" -> x
            "AHX" -> a and x
            "TAS" -> (a and x).also { sp = it }
            else -> 0
        }
        val value = valueRegister and (((base shr 8) + 1).low8Bits())
        val destination = if (base.pageBase() != target.pageBase()) {
            target.low8Bits() or (((target shr 8) and valueRegister) shl 8)
        } else {
            target
        }
        write(destination, value)
    }

    private var lastAddress = 0

    private fun write(address: Int, value: Int) {
        lastAddress = address
        bus.cpuWrite(address, value)
        totalCycles++
    }

    private fun dummyWrite(address: Int, value: Int) {
        bus.cpuWrite(address, value, dummy = true)
        totalCycles++
    }

    private fun read(address: Int, opcodeFetch: Boolean = false): Int {
        lastAddress = address.low16Bits()
        val result = bus.cpuRead(address, totalCycles, opcodeFetch = opcodeFetch)
        totalCycles += result.cycles
        return result.value
    }

    private fun dummyRead(address: Int, opcodeFetch: Boolean = false): Int {
        val result = bus.cpuRead(address, totalCycles, dummy = true, opcodeFetch = opcodeFetch)
        totalCycles += result.cycles
        return result.value
    }

    private fun fetch(): Int {
        val value = read(pc)
        pc = (pc + 1).low16Bits()
        return value
    }

    private fun fetchOpcode(): Int {
        val value = read(pc, opcodeFetch = true)
        pc = (pc + 1).low16Bits()
        return value
    }

    private fun impliedRead() {
        dummyRead(pc)
    }

    private fun absolute(): Int = fetch() or (fetch() shl 8)

    private fun push(value: Int) {
        write(0x100 or sp, value)
        sp = (sp - 1).low8Bits()
    }

    private fun pull(): Int {
        sp = (sp + 1).low8Bits()
        return read(0x100 or sp)
    }

    private fun pushInstruction(value: Int) {
        impliedRead()
        push(value)
    }

    private fun serviceInterrupt(vector: Int) {
        dummyRead(pc, opcodeFetch = true)
        dummyRead(pc)
        push(pc shr 8)
        push(pc)
        val selectedVector = if (nmiPending) {
            nmiPending = false
            0xFFFA
        } else {
            vector
        }
        push((status or U) and B.inv())
        set(I, true)
        pc = read(selectedVector) or (read(selectedVector + 1) shl 8)
        irqPending = false
    }

    private fun brk() {
        fetch() // BRK's padding byte is a real read.
        push(pc shr 8)
        push(pc)
        val vector = if (nmiPending) {
            nmiPending = false
            0xFFFA
        } else {
            0xFFFE
        }
        push(status or B or U)
        set(I, true)
        pc = read(vector) or (read(vector + 1) shl 8)
    }

    private fun jsr() {
        val low = fetch()
        dummyRead(0x100 or sp)
        push(pc shr 8)
        push(pc)
        pc = low or (fetch() shl 8)
    }

    private fun jump(mode: AddressMode) {
        if (mode == AddressMode.ABS) {
            pc = absolute()
        } else {
            val pointer = absolute()
            val highAddress = pointer.pageBase() or ((pointer + 1).low8Bits())
            pc = read(pointer) or (read(highAddress) shl 8)
        }
    }

    private fun rts() {
        impliedRead()
        dummyRead(0x100 or sp)
        val low = pull()
        val high = pull()
        val returnAddress = low or (high shl 8)
        dummyRead(returnAddress)
        pc = (returnAddress + 1).low16Bits()
    }

    private fun rti() {
        impliedRead()
        dummyRead(0x100 or sp)
        status = (pull() and (B or U).inv()) or U
        pc = pull() or (pull() shl 8)
    }

    private fun branch(instruction: String, offset: Int) {
        val take = when (instruction) {
            "BPL" -> !flag(N)
            "BMI" -> flag(N)
            "BVC" -> !flag(V)
            "BVS" -> flag(V)
            "BCC" -> !flag(C)
            "BCS" -> flag(C)
            "BNE" -> !flag(Z)
            "BEQ" -> flag(Z)
            else -> false
        }
        if (!take) return
        val oldPc = pc
        dummyRead(oldPc)
        val signed = if (offset < 0x80) offset else offset - 0x100
        val target = (oldPc + signed).low16Bits()
        if (oldPc.pageBase() != target.pageBase()) {
            dummyRead(oldPc.pageBase() or target.low8Bits())
        }
        pc = target
    }

    private fun flag(flag: Int): Boolean = (status and flag) != 0

    private fun set(flag: Int, enabled: Boolean) {
        status = if (enabled) status or flag else status and flag.inv()
        status = (status or U) and B.inv()
    }

    private fun zn(value: Int) {
        val result = value.low8Bits()
        set(Z, result == 0)
        set(N, (result and N) != 0)
    }

    private fun adc(value: Int) {
        val sum = a + value + if (flag(C)) 1 else 0
        val result = sum.low8Bits()
        set(C, sum > 0xFF)
        set(V, ((a xor result) and (value xor result) and 0x80) != 0)
        a = result
        zn(a)
    }

    private fun sbc(value: Int) = adc(value xor 0xFF)

    private fun compare(register: Int, value: Int) {
        val result = (register - value).low8Bits()
        set(C, register >= value)
        zn(result)
    }

    private fun bit(value: Int) {
        set(Z, (a and value) == 0)
        set(V, (value and V) != 0)
        set(N, (value and N) != 0)
    }

    private fun aslValue(value: Int): Int {
        set(C, (value and 0x80) != 0)
        return (value shl 1).low8Bits().also(::zn)
    }

    private fun lsrValue(value: Int): Int {
        set(C, (value and 1) != 0)
        return (value ushr 1).low8Bits().also(::zn)
    }

    private fun rolValue(value: Int): Int {
        val carry = if (flag(C)) 1 else 0
        set(C, (value and 0x80) != 0)
        return ((value shl 1) or carry).low8Bits().also(::zn)
    }

    private fun rorValue(value: Int): Int {
        val carry = if (flag(C)) 0x80 else 0
        set(C, (value and 1) != 0)
        return ((value ushr 1) or carry).low8Bits().also(::zn)
    }

    private fun arr(value: Int) {
        a = (a and value) ushr 1 or if (flag(C)) 0x80 else 0
        zn(a)
        set(C, (a and 0x40) != 0)
        set(V, ((a shr 6) xor (a shr 5)) and 1 != 0)
    }

    private fun axs(value: Int) {
        val source = a and x
        x = (source - value).low8Bits()
        set(C, source >= value)
        zn(x)
    }


}
