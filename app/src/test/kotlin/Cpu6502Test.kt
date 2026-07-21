import nes.cpu.Cpu6502
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class Cpu6502Test {
    private fun program(vararg bytes: Number): ByteArray {
        return bytes.map { it.toByte() }.toByteArray()
    }

    @Test
    fun `reset reads program counter from reset vector`() {
        val resetVectorProgram = program(Cpu6502.OP_NOP)
        val (cpu, _, _) = cpuWithProgram(resetVectorProgram, 0x8123)

        assertEquals(0x8123, cpu.pc, "Program counter matches reset vector")
    }

    @Test
    fun `load and store opcodes move accumulator into zero page memory`() {
        val loadStoreProgram = program(
                Cpu6502.OP_LDA_IMM,
                0x44,
                Cpu6502.OP_STA_ZP,
                0x10
        )
        val (cpu, bus, _) = cpuWithProgram(loadStoreProgram)

        cpu.step()
        cpu.step()

        assertEquals(0x44, bus.read(0x10), "Accumulator value is stored in zero page")
    }

    @Test
    fun `adc immediate sets accumulator and overflow flag`() {
        val adcOverflowProgram = program(
                Cpu6502.OP_LDA_IMM,
                0x50,
                Cpu6502.OP_ADC_IMM,
                0x50
        )
        val (cpu, _, _) = cpuWithProgram(adcOverflowProgram)

        cpu.step()
        cpu.step()

        assertEquals(0xA0, cpu.a, "Accumulator contains ADC result")
        assertTrue((cpu.status and Cpu6502.V) != 0, "Overflow flag is set")
    }

    @Test
    fun `cpx immediate sets carry and zero flags when values match`() {
        val cpxEqualProgram = program(
                Cpu6502.OP_LDX_IMM,
                3,
                Cpu6502.OP_CPX_IMM,
                3
        )
        val (cpu, _, _) = cpuWithProgram(cpxEqualProgram)

        cpu.step()
        cpu.step()

        assertTrue((cpu.status and Cpu6502.C) != 0, "Carry flag is set for equal comparison")
        assertTrue((cpu.status and Cpu6502.Z) != 0, "Zero flag is set for equal comparison")
    }

    @Test
    fun `beq skips instructions when zero flag is set`() {
        val branchWhenZeroProgram = program(
                Cpu6502.OP_LDA_IMM,
                0,
                Cpu6502.OP_BEQ,
                2,
                Cpu6502.OP_LDA_IMM,
                1,
                Cpu6502.OP_LDA_IMM,
                2
        )
        val (cpu, _, _) = cpuWithProgram(branchWhenZeroProgram)

        repeat(4) { cpu.step() }

        assertEquals(2, cpu.a, "Branch skips the first replacement accumulator value")
    }

    @Test
    fun `pha and pla restore accumulator through the stack`() {
        val accumulatorStackProgram = program(
                Cpu6502.OP_LDA_IMM,
                0x7F,
                Cpu6502.OP_PHA,
                Cpu6502.OP_LDA_IMM,
                0,
                Cpu6502.OP_PLA
        )
        val (cpu, _, _) = cpuWithProgram(accumulatorStackProgram)

        repeat(4) { cpu.step() }

        assertEquals(0x7F, cpu.a, "Accumulator is restored from the stack")
    }

    @Test
    fun `jsr and rts execute subroutine then return`() {
        val subroutineProgram = program(
                Cpu6502.OP_JSR_ABS,
                0x06,
                0x80,
                Cpu6502.OP_LDA_IMM,
                2,
                Cpu6502.OP_NOP,
                Cpu6502.OP_LDA_IMM,
                1,
                Cpu6502.OP_RTS
        )
        val (cpu, _, _) = cpuWithProgram(subroutineProgram)

        repeat(4) { cpu.step() }

        assertEquals(2, cpu.a, "Execution resumes after JSR once RTS returns")
    }

    @Test
    fun `brk jumps to irq vector and rti returns`() {
        val breakProgram = program(Cpu6502.OP_BRK, Cpu6502.OP_NOP)
        val (cpu, bus, _) = cpuWithProgram(breakProgram)
        bus.write(0x9100, Cpu6502.OP_RTI)

        cpu.step()
        assertEquals(0x9100, cpu.pc, "BRK loads the IRQ vector")

        cpu.step()
        assertEquals(0x8002, cpu.pc, "RTI restores the interrupted program counter")
    }

    @Test
    fun `nmi request jumps to nmi vector before next opcode`() {
        val nmiProgram = program(Cpu6502.OP_NOP)
        val (cpu, bus, _) = cpuWithProgram(nmiProgram)
        bus.write(0x9000, Cpu6502.OP_NOP)

        cpu.requestNmi()
        cpu.step()

        assertEquals(0x9000, cpu.pc, "NMI vector is loaded")
    }

    @Test
    fun `absolute x load adds a cycle when page boundary is crossed`() {
        val pageCrossingProgram = program(
                Cpu6502.OP_LDX_IMM,
                1,
                Cpu6502.OP_LDA_ABSX,
                0xFF,
                0x80
        )
        val (cpu, _, _) = cpuWithProgram(pageCrossingProgram)

        cpu.step()
        val cycles = cpu.step()

        assertEquals(5, cycles, "Page crossing adds one cycle")
    }

    @Test
    fun `zero page x addressing wraps within zero page`() {
        val zeroPageWrapProgram = program(Cpu6502.OP_LDX_IMM, 1, Cpu6502.OP_LDA_ZPX, 0xFF)
        val (cpu, bus, _) = cpuWithProgram(zeroPageWrapProgram)
        bus.write(0, 0x33)

        cpu.step()
        cpu.step()

        assertEquals(0x33, cpu.a, "Zero page indexed address wraps to address zero")
    }

    @Test
    fun `indirect jmp emulates 6502 page wraparound bug`() {
        val indirectJumpProgram = program(Cpu6502.OP_JMP_IND, 0xFF, 0x02)
        val (cpu, bus, _) = cpuWithProgram(indirectJumpProgram)
        bus.write(0x02FF, 0x34)
        bus.write(0x0200, 0x12)

        cpu.step()

        assertEquals(0x1234, cpu.pc, "Indirect JMP high byte wraps within the same page")
    }

    @Test
    fun `load accumulator updates zero and negative status flags`() {
        val zeroFlagProgram = program(
                Cpu6502.OP_LDA_IMM,
                0
        )
        val (cpu, _, _) = cpuWithProgram(zeroFlagProgram)

        cpu.step()

        assertTrue((cpu.status and Cpu6502.Z) != 0, "Zero flag is set for zero value")
        assertFalse((cpu.status and Cpu6502.N) != 0, "Negative flag is clear for zero value")
    }

    @Test
    fun `lda opcodes load accumulator from every addressing mode`() {
        val ldaAddressingProgram = program(
            Cpu6502.OP_LDX_IMM,
            0x04,
            Cpu6502.OP_LDY_IMM,
            0x05,
            Cpu6502.OP_LDA_IMM,
            0x11,
            Cpu6502.OP_LDA_ZP,
            0x10,
            Cpu6502.OP_LDA_ZPX,
            0x10,
            Cpu6502.OP_LDA_ABS,
            0x00,
            0x02,
            Cpu6502.OP_LDA_ABSX,
            0x00,
            0x02,
            Cpu6502.OP_LDA_ABSY,
            0x00,
            0x02,
            Cpu6502.OP_LDA_INDX,
            0x30,
            Cpu6502.OP_LDA_INDY,
            0x40
        )
        val (cpu, bus, _) = cpuWithProgram(ldaAddressingProgram)
        bus.write(0x0010, 0x22)
        bus.write(0x0014, 0x33)
        bus.write(0x0200, 0x44)
        bus.write(0x0204, 0x55)
        bus.write(0x0205, 0x66)
        bus.write(0x0034, 0x50)
        bus.write(0x0035, 0x02)
        bus.write(0x0250, 0x77)
        bus.write(0x0040, 0x60)
        bus.write(0x0041, 0x02)
        bus.write(0x0265, 0x88)

        cpu.step()
        cpu.step()

        cpu.step()
        assertEquals(0x11, cpu.a, "LDA immediate loads the accumulator")

        cpu.step()
        assertEquals(0x22, cpu.a, "LDA zero page loads the accumulator")

        cpu.step()
        assertEquals(0x33, cpu.a, "LDA zero page,X loads the accumulator")

        cpu.step()
        assertEquals(0x44, cpu.a, "LDA absolute loads the accumulator")

        cpu.step()
        assertEquals(0x55, cpu.a, "LDA absolute,X loads the accumulator")

        cpu.step()
        assertEquals(0x66, cpu.a, "LDA absolute,Y loads the accumulator")

        cpu.step()
        assertEquals(0x77, cpu.a, "LDA indexed-indirect loads the accumulator")

        cpu.step()
        assertEquals(0x88, cpu.a, "LDA indirect-indexed loads the accumulator")
    }

    @Test
    fun `ldx and ldy opcodes load index registers from every addressing mode`() {
        val loadIndexRegistersProgram = program(
            Cpu6502.OP_LDY_IMM,
            0x03,
            Cpu6502.OP_LDX_IMM,
            0x11,
            Cpu6502.OP_LDX_ZP,
            0x10,
            Cpu6502.OP_LDX_ZPY,
            0x10,
            Cpu6502.OP_LDX_ABS,
            0x00,
            0x02,
            Cpu6502.OP_LDX_ABSY,
            0x00,
            0x02,
            Cpu6502.OP_LDY_IMM,
            0x22,
            Cpu6502.OP_LDY_ZP,
            0x11,
            Cpu6502.OP_LDY_ZPX,
            0x10,
            Cpu6502.OP_LDY_ABS,
            0x30,
            0x02,
            Cpu6502.OP_LDY_ABSX,
            0x30,
            0x02
        )
        val (cpu, bus, _) = cpuWithProgram(loadIndexRegistersProgram)
        bus.write(0x0010, 0x33)
        bus.write(0x0013, 0x44)
        bus.write(0x0200, 0x55)
        bus.write(0x0203, 0x66)
        bus.write(0x0011, 0x77)
        bus.write(0x0076, 0x88)
        bus.write(0x0230, 0x99)
        bus.write(0x0296, 0xAA)

        cpu.step()
        assertEquals(0x03, cpu.y, "LDY immediate prepares the Y index")

        cpu.step()
        assertEquals(0x11, cpu.x, "LDX immediate loads the X register")

        cpu.step()
        assertEquals(0x33, cpu.x, "LDX zero page loads the X register")

        cpu.step()
        assertEquals(0x44, cpu.x, "LDX zero page,Y loads the X register")

        cpu.step()
        assertEquals(0x55, cpu.x, "LDX absolute loads the X register")

        cpu.step()
        assertEquals(0x66, cpu.x, "LDX absolute,Y loads the X register")

        cpu.step()
        assertEquals(0x22, cpu.y, "LDY immediate loads the Y register")

        cpu.step()
        assertEquals(0x77, cpu.y, "LDY zero page loads the Y register")

        cpu.step()
        assertEquals(0x88, cpu.y, "LDY zero page,X loads the Y register")

        cpu.step()
        assertEquals(0x99, cpu.y, "LDY absolute loads the Y register")

        cpu.step()
        assertEquals(0xAA, cpu.y, "LDY absolute,X loads the Y register")
    }

    @Test
    fun `sta stx and sty opcodes store registers through every addressing mode`() {
        val storeRegistersProgram = program(
            Cpu6502.OP_LDA_IMM,
            0x44,
            Cpu6502.OP_LDX_IMM,
            0x04,
            Cpu6502.OP_LDY_IMM,
            0x05,
            Cpu6502.OP_STA_ZP,
            0x10,
            Cpu6502.OP_STA_ZPX,
            0x10,
            Cpu6502.OP_STA_ABS,
            0x00,
            0x02,
            Cpu6502.OP_STA_ABSX,
            0x00,
            0x02,
            Cpu6502.OP_STA_ABSY,
            0x00,
            0x02,
            Cpu6502.OP_STA_INDX,
            0x30,
            Cpu6502.OP_STA_INDY,
            0x40,
            Cpu6502.OP_STX_ZP,
            0x50,
            Cpu6502.OP_STX_ZPY,
            0x50,
            Cpu6502.OP_STX_ABS,
            0x30,
            0x02,
            Cpu6502.OP_STY_ZP,
            0x60,
            Cpu6502.OP_STY_ZPX,
            0x60,
            Cpu6502.OP_STY_ABS,
            0x40,
            0x02
        )
        val (cpu, bus, _) = cpuWithProgram(storeRegistersProgram)
        bus.write(0x0034, 0x70)
        bus.write(0x0035, 0x02)
        bus.write(0x0040, 0x80)
        bus.write(0x0041, 0x02)

        repeat(18) { cpu.step() }

        assertEquals(0x44, bus.read(0x0010), "STA zero page stores the accumulator")
        assertEquals(0x44, bus.read(0x0014), "STA zero page,X stores the accumulator")
        assertEquals(0x44, bus.read(0x0200), "STA absolute stores the accumulator")
        assertEquals(0x44, bus.read(0x0204), "STA absolute,X stores the accumulator")
        assertEquals(0x44, bus.read(0x0205), "STA absolute,Y stores the accumulator")
        assertEquals(0x44, bus.read(0x0270), "STA indexed-indirect stores the accumulator")
        assertEquals(0x44, bus.read(0x0285), "STA indirect-indexed stores the accumulator")
        assertEquals(0x04, bus.read(0x0050), "STX zero page stores the X register")
        assertEquals(0x04, bus.read(0x0055), "STX zero page,Y stores the X register")
        assertEquals(0x04, bus.read(0x0230), "STX absolute stores the X register")
        assertEquals(0x05, bus.read(0x0060), "STY zero page stores the Y register")
        assertEquals(0x05, bus.read(0x0064), "STY zero page,X stores the Y register")
        assertEquals(0x05, bus.read(0x0240), "STY absolute stores the Y register")
    }

    @Test
    fun `transfer opcodes copy values between registers and stack pointer`() {
        val transferRegistersProgram = program(
            Cpu6502.OP_LDA_IMM,
            0x12,
            Cpu6502.OP_TAX,
            Cpu6502.OP_TAY,
            Cpu6502.OP_LDA_IMM,
            0x00,
            Cpu6502.OP_TXA,
            Cpu6502.OP_TYA,
            Cpu6502.OP_LDX_IMM,
            0xF0.toByte(),
            Cpu6502.OP_TXS,
            Cpu6502.OP_TSX
        )
        val (cpu, _, _) = cpuWithProgram(transferRegistersProgram)

        cpu.step()
        cpu.step()
        assertEquals(0x12, cpu.x, "TAX copies the accumulator to X")

        cpu.step()
        assertEquals(0x12, cpu.y, "TAY copies the accumulator to Y")

        cpu.step()
        cpu.step()
        assertEquals(0x12, cpu.a, "TXA copies X to the accumulator")

        cpu.step()
        assertEquals(0x12, cpu.a, "TYA copies Y to the accumulator")

        cpu.step()
        cpu.step()
        assertEquals(0xF0, cpu.sp, "TXS copies X to the stack pointer")

        cpu.step()
        assertEquals(0xF0, cpu.x, "TSX copies the stack pointer to X")
    }

    @Test
    fun `stack opcodes push and pull accumulator and status`() {
        val stackProgram = program(
            Cpu6502.OP_LDA_IMM,
            0x7F,
            Cpu6502.OP_PHA,
            Cpu6502.OP_LDA_IMM,
            0x00,
            Cpu6502.OP_PLA,
            Cpu6502.OP_SEC,
            Cpu6502.OP_PHP,
            Cpu6502.OP_CLC,
            Cpu6502.OP_PLP
        )
        val (cpu, _, _) = cpuWithProgram(stackProgram)

        repeat(6) { cpu.step() }
        assertEquals(0x7F, cpu.a, "PHA and PLA restore the accumulator")

        repeat(4) { cpu.step() }
        assertTrue((cpu.status and Cpu6502.C) != 0, "PHP and PLP restore the carry flag")
    }

    @Test
    fun `adc and sbc opcodes update accumulator carry and overflow`() {
        val arithmeticProgram = program(
            Cpu6502.OP_CLC,
            Cpu6502.OP_LDA_IMM,
            0x50,
            Cpu6502.OP_ADC_IMM,
            0x50,
            Cpu6502.OP_SBC_IMM,
            0x20,
            Cpu6502.OP_SBC_IMM_UNOFFICIAL,
            0x10,
            Cpu6502.OP_ADC_ZP,
            0x10,
            Cpu6502.OP_ADC_ZPX,
            0x10,
            Cpu6502.OP_ADC_ABS,
            0x00,
            0x02,
            Cpu6502.OP_ADC_ABSX,
            0x00,
            0x02,
            Cpu6502.OP_ADC_ABSY,
            0x00,
            0x02,
            Cpu6502.OP_ADC_INDX,
            0x30,
            Cpu6502.OP_ADC_INDY,
            0x40,
            Cpu6502.OP_SBC_ZP,
            0x11,
            Cpu6502.OP_SBC_ZPX,
            0x11,
            Cpu6502.OP_SBC_ABS,
            0x01,
            0x02,
            Cpu6502.OP_SBC_ABSX,
            0x01,
            0x02,
            Cpu6502.OP_SBC_ABSY,
            0x01,
            0x02,
            Cpu6502.OP_SBC_INDX,
            0x50,
            Cpu6502.OP_SBC_INDY,
            0x60
        )
        val (cpu, bus, _) = cpuWithProgram(arithmeticProgram)
        bus.write(0x0010, 0x01)
        bus.write(0x0014, 0x01)
        bus.write(0x0200, 0x01)
        bus.write(0x0204, 0x01)
        bus.write(0x0205, 0x01)
        bus.write(0x0034, 0x70)
        bus.write(0x0035, 0x02)
        bus.write(0x0270, 0x01)
        bus.write(0x0040, 0x80)
        bus.write(0x0041, 0x02)
        bus.write(0x0280, 0x01)
        bus.write(0x0011, 0x01)
        bus.write(0x0015, 0x01)
        bus.write(0x0201, 0x01)
        bus.write(0x0205, 0x01)
        bus.write(0x0206, 0x01)
        bus.write(0x0054, 0x90)
        bus.write(0x0055, 0x02)
        bus.write(0x0290, 0x01)
        bus.write(0x0060, 0xA0)
        bus.write(0x0061, 0x02)
        bus.write(0x02A5, 0x01)

        cpu.step()
        cpu.step()
        cpu.step()
        assertEquals(0xA0, cpu.a, "ADC immediate adds to the accumulator")
        assertTrue((cpu.status and Cpu6502.V) != 0, "ADC immediate sets overflow for signed overflow")

        repeat(16) { cpu.step() }
        assertEquals(0x70, cpu.a, "ADC and SBC addressing variants update the accumulator")
    }

    @Test
    fun `logical opcodes and ora and eor combine accumulator with memory`() {
        val logicalProgram = program(
            Cpu6502.OP_LDA_IMM,
            0xF0.toByte(),
            Cpu6502.OP_AND_IMM,
            0x0F,
            Cpu6502.OP_ORA_IMM,
            0x30,
            Cpu6502.OP_EOR_IMM,
            0x10,
            Cpu6502.OP_AND_ZP,
            0x10,
            Cpu6502.OP_ORA_ZP,
            0x11,
            Cpu6502.OP_EOR_ZP,
            0x12
        )
        val (cpu, bus, _) = cpuWithProgram(logicalProgram)
        bus.write(0x0010, 0x2F)
        bus.write(0x0011, 0x80)
        bus.write(0x0012, 0xFF)

        cpu.step()
        cpu.step()
        assertEquals(0x00, cpu.a, "AND immediate masks the accumulator")
        assertTrue((cpu.status and Cpu6502.Z) != 0, "AND immediate updates the zero flag")

        cpu.step()
        assertEquals(0x30, cpu.a, "ORA immediate sets accumulator bits")

        cpu.step()
        assertEquals(0x20, cpu.a, "EOR immediate toggles accumulator bits")

        cpu.step()
        assertEquals(0x20, cpu.a, "AND zero page masks accumulator bits")

        cpu.step()
        assertEquals(0xA0, cpu.a, "ORA zero page sets accumulator bits from memory")

        cpu.step()
        assertEquals(0x5F, cpu.a, "EOR zero page toggles accumulator bits from memory")
    }

    @Test
    fun `compare opcodes update carry zero and negative flags`() {
        val compareProgram = program(
            Cpu6502.OP_LDA_IMM,
            0x20,
            Cpu6502.OP_CMP_IMM,
            0x20,
            Cpu6502.OP_CMP_ZP,
            0x10,
            Cpu6502.OP_LDX_IMM,
            0x30,
            Cpu6502.OP_CPX_IMM,
            0x20,
            Cpu6502.OP_CPX_ZP,
            0x11,
            Cpu6502.OP_LDY_IMM,
            0x10,
            Cpu6502.OP_CPY_IMM,
            0x20,
            Cpu6502.OP_CPY_ZP,
            0x12
        )
        val (cpu, bus, _) = cpuWithProgram(compareProgram)
        bus.write(0x0010, 0x30)
        bus.write(0x0011, 0x30)
        bus.write(0x0012, 0x10)

        cpu.step()
        cpu.step()
        assertTrue((cpu.status and Cpu6502.Z) != 0, "CMP immediate sets zero when values match")

        cpu.step()
        assertTrue((cpu.status and Cpu6502.N) != 0, "CMP zero page sets negative when register is smaller")

        cpu.step()
        cpu.step()
        assertTrue((cpu.status and Cpu6502.C) != 0, "CPX immediate sets carry when register is greater")

        cpu.step()
        assertTrue((cpu.status and Cpu6502.Z) != 0, "CPX zero page sets zero when values match")

        cpu.step()
        cpu.step()
        assertTrue((cpu.status and Cpu6502.N) != 0, "CPY immediate sets negative when register is smaller")

        cpu.step()
        assertTrue((cpu.status and Cpu6502.Z) != 0, "CPY zero page sets zero when values match")
    }

    @Test
    fun `inc dec inx iny dex and dey opcodes mutate values and flags`() {
        val incrementDecrementProgram = program(
            Cpu6502.OP_INC_ZP,
            0x10,
            Cpu6502.OP_DEC_ZP,
            0x10,
            Cpu6502.OP_LDX_IMM,
            0xFF.toByte(),
            Cpu6502.OP_INX,
            Cpu6502.OP_DEX,
            Cpu6502.OP_LDY_IMM,
            0x00,
            Cpu6502.OP_DEY,
            Cpu6502.OP_INY
        )
        val (cpu, bus, _) = cpuWithProgram(incrementDecrementProgram)
        bus.write(0x0010, 0x7F)

        cpu.step()
        assertEquals(0x80, bus.read(0x0010), "INC zero page increments memory")
        assertTrue((cpu.status and Cpu6502.N) != 0, "INC zero page updates the negative flag")

        cpu.step()
        assertEquals(0x7F, bus.read(0x0010), "DEC zero page decrements memory")

        cpu.step()
        cpu.step()
        assertEquals(0x00, cpu.x, "INX increments X with 8-bit wraparound")
        assertTrue((cpu.status and Cpu6502.Z) != 0, "INX updates the zero flag")

        cpu.step()
        assertEquals(0xFF, cpu.x, "DEX decrements X with 8-bit wraparound")

        cpu.step()
        cpu.step()
        assertEquals(0xFF, cpu.y, "DEY decrements Y with 8-bit wraparound")

        cpu.step()
        assertEquals(0x00, cpu.y, "INY increments Y with 8-bit wraparound")
    }

    @Test
    fun `shift and rotate opcodes move bits through carry`() {
        val shiftRotateProgram = program(
            Cpu6502.OP_LDA_IMM,
            0x81.toByte(),
            Cpu6502.OP_ASL_ACC,
            Cpu6502.OP_LSR_ACC,
            Cpu6502.OP_ROL_ACC,
            Cpu6502.OP_ROR_ACC,
            Cpu6502.OP_ASL_ZP,
            0x10,
            Cpu6502.OP_LSR_ZP,
            0x11,
            Cpu6502.OP_ROL_ZP,
            0x12,
            Cpu6502.OP_ROR_ZP,
            0x13
        )
        val (cpu, bus, _) = cpuWithProgram(shiftRotateProgram)
        bus.write(0x0010, 0x80)
        bus.write(0x0011, 0x01)
        bus.write(0x0012, 0x40)
        bus.write(0x0013, 0x02)

        cpu.step()
        cpu.step()
        assertEquals(0x02, cpu.a, "ASL accumulator shifts the accumulator left")
        assertTrue((cpu.status and Cpu6502.C) != 0, "ASL accumulator moves bit seven into carry")

        cpu.step()
        assertEquals(0x01, cpu.a, "LSR accumulator shifts the accumulator right")

        cpu.step()
        assertEquals(0x02, cpu.a, "ROL accumulator rotates the accumulator left through carry")

        cpu.step()
        assertEquals(0x01, cpu.a, "ROR accumulator rotates the accumulator right through carry")

        cpu.step()
        assertEquals(0x00, bus.read(0x0010), "ASL zero page writes the shifted value to memory")

        cpu.step()
        assertEquals(0x00, bus.read(0x0011), "LSR zero page writes the shifted value to memory")

        cpu.step()
        assertEquals(0x81, bus.read(0x0012), "ROL zero page writes the rotated value to memory")

        cpu.step()
        assertEquals(0x01, bus.read(0x0013), "ROR zero page writes the rotated value to memory")
    }

    @Test
    fun `bit opcode updates zero overflow and negative flags from memory`() {
        val bitProgram = program(
            Cpu6502.OP_LDA_IMM,
            0x0F,
            Cpu6502.OP_BIT_ZP,
            0x10,
            Cpu6502.OP_BIT_ABS,
            0x00,
            0x02
        )
        val (cpu, bus, _) = cpuWithProgram(bitProgram)
        bus.write(0x0010, 0xC0)
        bus.write(0x0200, 0x01)

        cpu.step()
        cpu.step()
        assertTrue((cpu.status and Cpu6502.Z) != 0, "BIT zero page sets zero when accumulator and memory do not overlap")
        assertTrue((cpu.status and Cpu6502.V) != 0, "BIT zero page copies bit six into overflow")
        assertTrue((cpu.status and Cpu6502.N) != 0, "BIT zero page copies bit seven into negative")

        cpu.step()
        assertFalse((cpu.status and Cpu6502.Z) != 0, "BIT absolute clears zero when accumulator and memory overlap")
    }

    @Test
    fun `jump subroutine return break and interrupt opcodes control program counter`() {
        val controlFlowProgram = program(
            Cpu6502.OP_JSR_ABS,
            0x07,
            0x80.toByte(),
            Cpu6502.OP_JMP_ABS,
            0x0A,
            0x80.toByte(),
            Cpu6502.OP_NOP,
            Cpu6502.OP_LDA_IMM,
            0x01,
            Cpu6502.OP_RTS,
            Cpu6502.OP_JMP_IND,
            0x20,
            0x00,
            Cpu6502.OP_BRK
        )
        val (cpu, bus, _) = cpuWithProgram(controlFlowProgram)
        bus.write(0x0020, 0x0D)
        bus.write(0x0021, 0x80)
        bus.write(0x9100, Cpu6502.OP_RTI)

        cpu.step()
        assertEquals(0x8007, cpu.pc, "JSR absolute jumps to the subroutine address")

        cpu.step()
        assertEquals(0x01, cpu.a, "Subroutine body executes after JSR")

        cpu.step()
        assertEquals(0x8003, cpu.pc, "RTS returns to the instruction after JSR")

        cpu.step()
        assertEquals(0x800A, cpu.pc, "JMP absolute changes the program counter")

        cpu.step()
        assertEquals(0x800D, cpu.pc, "JMP indirect loads the target from memory")

        cpu.step()
        assertEquals(0x9100, cpu.pc, "BRK loads the IRQ vector")

        cpu.step()
        assertEquals(0x800F, cpu.pc, "RTI restores the program counter from the stack")
    }

    @Test
    fun `branch opcodes change program counter based on status flags`() {
        val branchProgram = program(
            Cpu6502.OP_LDA_IMM,
            0x00,
            Cpu6502.OP_BEQ,
            0x02,
            Cpu6502.OP_LDA_IMM,
            0x01,
            Cpu6502.OP_LDA_IMM,
            0x80.toByte(),
            Cpu6502.OP_BMI,
            0x02,
            Cpu6502.OP_LDA_IMM,
            0x02,
            Cpu6502.OP_CLV,
            Cpu6502.OP_BVC,
            0x02,
            Cpu6502.OP_LDA_IMM,
            0x03,
            Cpu6502.OP_SEC,
            Cpu6502.OP_BCS,
            0x02,
            Cpu6502.OP_LDA_IMM,
            0x04,
            Cpu6502.OP_CLC,
            Cpu6502.OP_BCC,
            0x02,
            Cpu6502.OP_LDA_IMM,
            0x05,
            Cpu6502.OP_BNE,
            0x02,
            Cpu6502.OP_LDA_IMM,
            0x06,
            Cpu6502.OP_LDA_IMM,
            0x40,
            Cpu6502.OP_ADC_IMM,
            0x40,
            Cpu6502.OP_BVS,
            0x02,
            Cpu6502.OP_LDA_IMM,
            0x07,
            Cpu6502.OP_BPL,
            0x02,
            Cpu6502.OP_LDA_IMM,
            0x08
        )
        val (cpu, _, _) = cpuWithProgram(branchProgram)

        repeat(17) { cpu.step() }

        assertEquals(0x08, cpu.a, "All branch opcodes route execution according to status flags")
    }

    @Test
    fun `flag opcodes set and clear status bits`() {
        val flagProgram = program(
            Cpu6502.OP_SEC,
            Cpu6502.OP_CLC,
            Cpu6502.OP_SEI,
            Cpu6502.OP_CLI,
            Cpu6502.OP_SED,
            Cpu6502.OP_CLD,
            Cpu6502.OP_LDA_IMM,
            0x40,
            Cpu6502.OP_ADC_IMM,
            0x40,
            Cpu6502.OP_CLV
        )
        val (cpu, _, _) = cpuWithProgram(flagProgram)

        cpu.step()
        assertTrue((cpu.status and Cpu6502.C) != 0, "SEC sets carry")

        cpu.step()
        assertFalse((cpu.status and Cpu6502.C) != 0, "CLC clears carry")

        cpu.step()
        assertTrue((cpu.status and Cpu6502.I) != 0, "SEI sets interrupt disable")

        cpu.step()
        assertFalse((cpu.status and Cpu6502.I) != 0, "CLI clears interrupt disable")

        cpu.step()
        assertTrue((cpu.status and Cpu6502.D) != 0, "SED sets decimal mode")

        cpu.step()
        assertFalse((cpu.status and Cpu6502.D) != 0, "CLD clears decimal mode")

        cpu.step()
        cpu.step()
        assertTrue((cpu.status and Cpu6502.V) != 0, "ADC sets overflow before CLV")

        cpu.step()
        assertFalse((cpu.status and Cpu6502.V) != 0, "CLV clears overflow")
    }

    @Test
    fun `nop opcode only advances the program counter`() {
        val nopProgram = program(Cpu6502.OP_NOP)
        val (cpu, _, _) = cpuWithProgram(nopProgram)

        val cycles = cpu.step()

        assertEquals(0x8001, cpu.pc, "NOP advances the program counter by one byte")
        assertEquals(2, cycles, "NOP consumes two cycles")
    }
}
