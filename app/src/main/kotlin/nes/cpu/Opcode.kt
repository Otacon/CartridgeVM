package nes.cpu

data class Opcode(val code: Int, val mnemonic: String, val mode: AddressingMode, val cycles: Int)
