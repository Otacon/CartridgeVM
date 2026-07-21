package nes.cpu

class Cpu6502(private val bus: CpuBus) {
    companion object {
        const val C = 0x01; const val Z = 0x02; const val I = 0x04; const val D = 0x08
        const val B = 0x10; const val U = 0x20; const val V = 0x40; const val N = 0x80
    }

    var pc = 0; private set
    var a = 0; private set
    var x = 0; private set
    var y = 0; private set
    var sp = 0xFD; private set
    var status = I or U; private set
    var totalCycles = 0L; private set

    private var nmiPending = false
    private var irqPending = false

    fun reset() {
        a = 0; x = 0; y = 0; sp = 0xFD; status = I or U
        pc = read16(0xFFFC)
        totalCycles = 7
        nmiPending = false; irqPending = false
    }

    fun requestNmi() { nmiPending = true }
    fun requestIrq() { irqPending = true }

    fun step(): Int {
        if (nmiPending) { nmiPending = false; return interrupt(0xFFFA, false) }
        if (irqPending && !flag(I)) { irqPending = false; return interrupt(0xFFFE, false) }
        val op = read(pc); pc = (pc + 1) and 0xFFFF
        val cycles = execute(op) + bus.consumeDmaCycles()
        totalCycles += cycles.toLong()
        return cycles
    }

    private fun execute(op: Int): Int = when (op) {
        0xA9 -> { a = imm(); zn(a); 2 }; 0xA5 -> { a = read(zp()); zn(a); 3 }; 0xB5 -> { a = read(zpx()); zn(a); 4 }
        0xAD -> { a = read(abs()); zn(a); 4 }; 0xBD -> { val r=absx(true); a=read(r.addr); zn(a); 4+r.page }; 0xB9 -> { val r=absy(true); a=read(r.addr); zn(a); 4+r.page }
        0xA1 -> { a = read(indx()); zn(a); 6 }; 0xB1 -> { val r=indy(true); a=read(r.addr); zn(a); 5+r.page }
        0xA2 -> { x = imm(); zn(x); 2 }; 0xA6 -> { x = read(zp()); zn(x); 3 }; 0xB6 -> { x = read(zpy()); zn(x); 4 }; 0xAE -> { x = read(abs()); zn(x); 4 }; 0xBE -> { val r=absy(true); x=read(r.addr); zn(x); 4+r.page }
        0xA0 -> { y = imm(); zn(y); 2 }; 0xA4 -> { y = read(zp()); zn(y); 3 }; 0xB4 -> { y = read(zpx()); zn(y); 4 }; 0xAC -> { y = read(abs()); zn(y); 4 }; 0xBC -> { val r=absx(true); y=read(r.addr); zn(y); 4+r.page }
        0x85 -> { write(zp(), a); 3 }; 0x95 -> { write(zpx(), a); 4 }; 0x8D -> { write(abs(), a); 4 }; 0x9D -> { write(absx(false).addr, a); 5 }; 0x99 -> { write(absy(false).addr, a); 5 }; 0x81 -> { write(indx(), a); 6 }; 0x91 -> { write(indy(false).addr, a); 6 }
        0x86 -> { write(zp(), x); 3 }; 0x96 -> { write(zpy(), x); 4 }; 0x8E -> { write(abs(), x); 4 }
        0x84 -> { write(zp(), y); 3 }; 0x94 -> { write(zpx(), y); 4 }; 0x8C -> { write(abs(), y); 4 }
        0xAA -> { x=a; zn(x); 2 }; 0xA8 -> { y=a; zn(y); 2 }; 0x8A -> { a=x; zn(a); 2 }; 0x98 -> { a=y; zn(a); 2 }; 0xBA -> { x=sp; zn(x); 2 }; 0x9A -> { sp=x; 2 }
        0x48 -> { push(a); 3 }; 0x68 -> { a=pull(); zn(a); 4 }; 0x08 -> { push(status or B or U); 3 }; 0x28 -> { status=(pull() or U) and B.inv(); 4 }
        0x69 -> { adc(imm()); 2 }; 0x65 -> { adc(read(zp())); 3 }; 0x75 -> { adc(read(zpx())); 4 }; 0x6D -> { adc(read(abs())); 4 }; 0x7D -> { val r=absx(true); adc(read(r.addr)); 4+r.page }; 0x79 -> { val r=absy(true); adc(read(r.addr)); 4+r.page }; 0x61 -> { adc(read(indx())); 6 }; 0x71 -> { val r=indy(true); adc(read(r.addr)); 5+r.page }
        0xE9,0xEB -> { sbc(imm()); 2 }; 0xE5 -> { sbc(read(zp())); 3 }; 0xF5 -> { sbc(read(zpx())); 4 }; 0xED -> { sbc(read(abs())); 4 }; 0xFD -> { val r=absx(true); sbc(read(r.addr)); 4+r.page }; 0xF9 -> { val r=absy(true); sbc(read(r.addr)); 4+r.page }; 0xE1 -> { sbc(read(indx())); 6 }; 0xF1 -> { val r=indy(true); sbc(read(r.addr)); 5+r.page }
        0x29 -> { a = a and imm(); zn(a); 2 }; 0x25 -> { a = a and read(zp()); zn(a); 3 }; 0x35 -> { a = a and read(zpx()); zn(a); 4 }; 0x2D -> { a = a and read(abs()); zn(a); 4 }; 0x3D -> { val r=absx(true); a=a and read(r.addr); zn(a); 4+r.page }; 0x39 -> { val r=absy(true); a=a and read(r.addr); zn(a); 4+r.page }; 0x21 -> { a=a and read(indx()); zn(a); 6 }; 0x31 -> { val r=indy(true); a=a and read(r.addr); zn(a); 5+r.page }
        0x09 -> { a = a or imm(); zn(a); 2 }; 0x05 -> { a = a or read(zp()); zn(a); 3 }; 0x15 -> { a = a or read(zpx()); zn(a); 4 }; 0x0D -> { a = a or read(abs()); zn(a); 4 }; 0x1D -> { val r=absx(true); a=a or read(r.addr); zn(a); 4+r.page }; 0x19 -> { val r=absy(true); a=a or read(r.addr); zn(a); 4+r.page }; 0x01 -> { a=a or read(indx()); zn(a); 6 }; 0x11 -> { val r=indy(true); a=a or read(r.addr); zn(a); 5+r.page }
        0x49 -> { a = a xor imm(); zn(a); 2 }; 0x45 -> { a = a xor read(zp()); zn(a); 3 }; 0x55 -> { a = a xor read(zpx()); zn(a); 4 }; 0x4D -> { a = a xor read(abs()); zn(a); 4 }; 0x5D -> { val r=absx(true); a=a xor read(r.addr); zn(a); 4+r.page }; 0x59 -> { val r=absy(true); a=a xor read(r.addr); zn(a); 4+r.page }; 0x41 -> { a=a xor read(indx()); zn(a); 6 }; 0x51 -> { val r=indy(true); a=a xor read(r.addr); zn(a); 5+r.page }
        0xC9 -> { cmp(a, imm()); 2 }; 0xC5 -> { cmp(a, read(zp())); 3 }; 0xD5 -> { cmp(a, read(zpx())); 4 }; 0xCD -> { cmp(a, read(abs())); 4 }; 0xDD -> { val r=absx(true); cmp(a, read(r.addr)); 4+r.page }; 0xD9 -> { val r=absy(true); cmp(a, read(r.addr)); 4+r.page }; 0xC1 -> { cmp(a, read(indx())); 6 }; 0xD1 -> { val r=indy(true); cmp(a, read(r.addr)); 5+r.page }
        0xE0 -> { cmp(x, imm()); 2 }; 0xE4 -> { cmp(x, read(zp())); 3 }; 0xEC -> { cmp(x, read(abs())); 4 }; 0xC0 -> { cmp(y, imm()); 2 }; 0xC4 -> { cmp(y, read(zp())); 3 }; 0xCC -> { cmp(y, read(abs())); 4 }
        0xE6 -> { inc(zp()); 5 }; 0xF6 -> { inc(zpx()); 6 }; 0xEE -> { inc(abs()); 6 }; 0xFE -> { inc(absx(false).addr); 7 }; 0xC6 -> { dec(zp()); 5 }; 0xD6 -> { dec(zpx()); 6 }; 0xCE -> { dec(abs()); 6 }; 0xDE -> { dec(absx(false).addr); 7 }
        0xE8 -> { x=(x+1) and 0xFF; zn(x); 2 }; 0xC8 -> { y=(y+1) and 0xFF; zn(y); 2 }; 0xCA -> { x=(x-1) and 0xFF; zn(x); 2 }; 0x88 -> { y=(y-1) and 0xFF; zn(y); 2 }
        0x0A -> { a = aslValue(a); 2 }; 0x06 -> { asl(zp()); 5 }; 0x16 -> { asl(zpx()); 6 }; 0x0E -> { asl(abs()); 6 }; 0x1E -> { asl(absx(false).addr); 7 }
        0x4A -> { a = lsrValue(a); 2 }; 0x46 -> { lsr(zp()); 5 }; 0x56 -> { lsr(zpx()); 6 }; 0x4E -> { lsr(abs()); 6 }; 0x5E -> { lsr(absx(false).addr); 7 }
        0x2A -> { a = rolValue(a); 2 }; 0x26 -> { rol(zp()); 5 }; 0x36 -> { rol(zpx()); 6 }; 0x2E -> { rol(abs()); 6 }; 0x3E -> { rol(absx(false).addr); 7 }
        0x6A -> { a = rorValue(a); 2 }; 0x66 -> { ror(zp()); 5 }; 0x76 -> { ror(zpx()); 6 }; 0x6E -> { ror(abs()); 6 }; 0x7E -> { ror(absx(false).addr); 7 }
        0x24 -> { bit(read(zp())); 3 }; 0x2C -> { bit(read(abs())); 4 }
        0x4C -> { pc = abs(); 3 }; 0x6C -> { pc = jmpIndirect(); 5 }; 0x20 -> { val addr=abs(); push16((pc-1) and 0xFFFF); pc=addr; 6 }; 0x60 -> { pc=(pull16()+1) and 0xFFFF; 6 }; 0x40 -> { status=(pull() or U) and B.inv(); pc=pull16(); 6 }
        0x00 -> { pc=(pc+1) and 0xFFFF; interrupt(0xFFFE, true) }
        0x10 -> branch(!flag(N)); 0x30 -> branch(flag(N)); 0x50 -> branch(!flag(V)); 0x70 -> branch(flag(V)); 0x90 -> branch(!flag(C)); 0xB0 -> branch(flag(C)); 0xD0 -> branch(!flag(Z)); 0xF0 -> branch(flag(Z))
        0x18 -> { set(C,false); 2 }; 0x38 -> { set(C,true); 2 }; 0x58 -> { set(I,false); 2 }; 0x78 -> { set(I,true); 2 }; 0xB8 -> { set(V,false); 2 }; 0xD8 -> { set(D,false); 2 }; 0xF8 -> { set(D,true); 2 }
        0xEA -> 2
        else -> error("Unsupported unofficial opcode 0x${op.toString(16).padStart(2, '0')}")
    }

    private data class Addr(val addr: Int, val page: Int)
    private fun read(addr: Int) = bus.read(addr)
    private fun write(addr: Int, value: Int) = bus.write(addr, value)
    private fun imm(): Int { val v = read(pc); pc = (pc + 1) and 0xFFFF; return v }
    private fun zp() = imm()
    private fun zpx() = (imm() + x) and 0xFF
    private fun zpy() = (imm() + y) and 0xFF
    private fun abs(): Int { val lo=imm(); val hi=imm(); return lo or (hi shl 8) }
    private fun absx(page: Boolean): Addr { val b=abs(); val a=(b+x) and 0xFFFF; return Addr(a, if (page && (b and 0xFF00)!=(a and 0xFF00)) 1 else 0) }
    private fun absy(page: Boolean): Addr { val b=abs(); val a=(b+y) and 0xFFFF; return Addr(a, if (page && (b and 0xFF00)!=(a and 0xFF00)) 1 else 0) }
    private fun indx(): Int { val p=(imm()+x) and 0xFF; return read(p) or (read((p+1) and 0xFF) shl 8) }
    private fun indy(page: Boolean): Addr { val p=imm(); val b=read(p) or (read((p+1) and 0xFF) shl 8); val a=(b+y) and 0xFFFF; return Addr(a, if (page && (b and 0xFF00)!=(a and 0xFF00)) 1 else 0) }
    private fun read16(addr: Int) = read(addr) or (read((addr + 1) and 0xFFFF) shl 8)
    private fun jmpIndirect(): Int { val p=abs(); return read(p) or (read((p and 0xFF00) or ((p + 1) and 0xFF)) shl 8) }
    private fun flag(f: Int) = (status and f) != 0
    private fun set(f: Int, on: Boolean) { status = if (on) status or f else status and f.inv(); status = status or U }
    private fun zn(v: Int) { set(Z, (v and 0xFF) == 0); set(N, (v and 0x80) != 0) }
    private fun push(v: Int) { write(0x100 or sp, v); sp = (sp - 1) and 0xFF }
    private fun pull(): Int { sp = (sp + 1) and 0xFF; return read(0x100 or sp) }
    private fun push16(v: Int) { push(v shr 8); push(v) }
    private fun pull16(): Int { val lo=pull(); val hi=pull(); return lo or (hi shl 8) }

    private fun interrupt(vector: Int, brk: Boolean): Int {
        push16(pc); push((status or U or if (brk) B else 0) and if (brk) 0xFF else B.inv())
        set(I, true); pc = read16(vector); return if (brk) 7 else 7
    }

    private fun adc(v: Int) { val sum=a+v+if(flag(C))1 else 0; set(C,sum>0xFF); set(V, ((a xor sum) and (v xor sum) and 0x80)!=0); a=sum and 0xFF; zn(a) }
    private fun sbc(v: Int) = adc(v xor 0xFF)
    private fun cmp(r: Int, v: Int) { val d=(r-v) and 0x1FF; set(C,r>=v); zn(d and 0xFF) }
    private fun bit(v: Int) { set(Z,(a and v)==0); set(V,(v and 0x40)!=0); set(N,(v and 0x80)!=0) }
    private fun inc(addr: Int) { val v=(read(addr)+1) and 0xFF; write(addr,v); zn(v) }
    private fun dec(addr: Int) { val v=(read(addr)-1) and 0xFF; write(addr,v); zn(v) }
    private fun asl(addr: Int) { val v=aslValue(read(addr)); write(addr,v) }
    private fun lsr(addr: Int) { val v=lsrValue(read(addr)); write(addr,v) }
    private fun rol(addr: Int) { val v=rolValue(read(addr)); write(addr,v) }
    private fun ror(addr: Int) { val v=rorValue(read(addr)); write(addr,v) }
    private fun aslValue(v: Int): Int { set(C,(v and 0x80)!=0); val r=(v shl 1) and 0xFF; zn(r); return r }
    private fun lsrValue(v: Int): Int { set(C,(v and 1)!=0); val r=(v shr 1) and 0xFF; zn(r); return r }
    private fun rolValue(v: Int): Int { val c=if(flag(C))1 else 0; set(C,(v and 0x80)!=0); val r=((v shl 1) or c) and 0xFF; zn(r); return r }
    private fun rorValue(v: Int): Int { val c=if(flag(C))0x80 else 0; set(C,(v and 1)!=0); val r=((v shr 1) or c) and 0xFF; zn(r); return r }
    private fun branch(cond: Boolean): Int { val off=imm(); if(!cond) return 2; val old=pc; val signed=if(off<0x80) off else off-0x100; pc=(pc+signed) and 0xFFFF; return 3 + if ((old and 0xFF00)!=(pc and 0xFF00)) 1 else 0 }
}
