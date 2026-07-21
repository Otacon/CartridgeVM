package app

data class CliArguments(val romPath: String?, val debug: Boolean, val unlimited: Boolean) {
    companion object {
        fun parse(args: Array<String>): CliArguments {
            var debug = false
            var unlimited = false
            var rom: String? = null
            for (arg in args) {
                when (arg) {
                    "--debug" -> debug = true
                    "--unlimited" -> unlimited = true
                    "--help", "-h" -> return CliArguments(null, debug, unlimited)
                    else -> if (rom == null) rom = arg else throw IllegalArgumentException("Unexpected argument: $arg")
                }
            }
            return CliArguments(rom, debug, unlimited)
        }

        fun usage() = "Usage: ./gradlew run --args=\"[--debug] [--unlimited] /path/to/game.nes\""
    }
}
