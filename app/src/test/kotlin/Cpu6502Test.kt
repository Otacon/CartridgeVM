import kotlin.test.*
import nes.cpu.Cpu6502

class Cpu6502Test {
    @Test fun resetVector() { val (cpu, _, _) = cpuWithProgram(byteArrayOf(0xEA.toByte()), 0x8123); assertEquals(0x8123, cpu.pc) }
    @Test fun loadsStores() { val (cpu, bus, _) = cpuWithProgram(byteArrayOf(0xA9.toByte(), 0x44, 0x85.toByte(), 0x10)); cpu.step(); cpu.step(); assertEquals(0x44, bus.read(0x10)) }
    @Test fun arithmeticCarryOverflow() { val (cpu, _, _) = cpuWithProgram(byteArrayOf(0xA9.toByte(), 0x50, 0x69, 0x50)); cpu.step(); cpu.step(); assertEquals(0xA0, cpu.a); assertTrue((cpu.status and Cpu6502.V) != 0) }
    @Test fun comparisons() { val (cpu, _, _) = cpuWithProgram(byteArrayOf(0xA2.toByte(), 3, 0xE0.toByte(), 3)); cpu.step(); cpu.step(); assertTrue((cpu.status and Cpu6502.C) != 0); assertTrue((cpu.status and Cpu6502.Z) != 0) }
    @Test fun branches() { val (cpu, _, _) = cpuWithProgram(byteArrayOf(0xA9.toByte(), 0, 0xF0.toByte(), 2, 0xA9.toByte(), 1, 0xA9.toByte(), 2)); repeat(4) { cpu.step() }; assertEquals(2, cpu.a) }
    @Test fun stackOperations() { val (cpu, _, _) = cpuWithProgram(byteArrayOf(0xA9.toByte(), 0x7F, 0x48, 0xA9.toByte(), 0, 0x68)); repeat(4) { cpu.step() }; assertEquals(0x7F, cpu.a) }
    @Test fun jsrRts() { val (cpu, _, _) = cpuWithProgram(byteArrayOf(0x20, 0x06, 0x80.toByte(), 0xA9.toByte(), 2, 0xEA.toByte(), 0xA9.toByte(), 1, 0x60)); repeat(4) { cpu.step() }; assertEquals(2, cpu.a) }
    @Test fun brkRti() { val (cpu, bus, _) = cpuWithProgram(byteArrayOf(0x00, 0xEA.toByte())); bus.write(0x9100, 0x40); cpu.step(); assertEquals(0x9100, cpu.pc); cpu.step(); assertEquals(0x8002, cpu.pc) }
    @Test fun nmi() { val (cpu, bus, _) = cpuWithProgram(byteArrayOf(0xEA.toByte())); bus.write(0x9000, 0xEA); cpu.requestNmi(); cpu.step(); assertEquals(0x9000, cpu.pc) }
    @Test fun pageCrossing() { val (cpu, _, _) = cpuWithProgram(byteArrayOf(0xA2.toByte(), 1, 0xBD.toByte(), 0xFF.toByte(), 0x80.toByte())); cpu.step(); val cycles = cpu.step(); assertEquals(5, cycles) }
    @Test fun zeroPageWrapping() { val (cpu, bus, _) = cpuWithProgram(byteArrayOf(0xA2.toByte(), 1, 0xB5.toByte(), 0xFF.toByte())); bus.write(0, 0x33); cpu.step(); cpu.step(); assertEquals(0x33, cpu.a) }
    @Test fun indirectJmpWraparound() { val (cpu, bus, _) = cpuWithProgram(byteArrayOf(0x6C, 0xFF.toByte(), 0x02)); bus.write(0x02FF, 0x34); bus.write(0x0200, 0x12); cpu.step(); assertEquals(0x1234, cpu.pc) }
    @Test fun statusRegisterBehavior() { val (cpu, _, _) = cpuWithProgram(byteArrayOf(0xA9.toByte(), 0)); cpu.step(); assertTrue((cpu.status and Cpu6502.Z) != 0); assertFalse((cpu.status and Cpu6502.N) != 0) }
}
