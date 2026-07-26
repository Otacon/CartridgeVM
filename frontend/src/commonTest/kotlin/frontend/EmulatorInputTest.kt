package frontend

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EmulatorInputTest {
    @Test
    fun controlActionsAreReportedOncePerPress() {
        val input = TestInput()

        input.pause = true
        input.reset = true
        input.poll()
        assertTrue(input.consumePause())
        assertTrue(input.consumeReset())
        assertFalse(input.consumePause())
        assertFalse(input.consumeReset())

        input.poll()
        assertFalse(input.consumePause())
        assertFalse(input.consumeReset())

        input.pause = false
        input.reset = false
        input.poll()
        input.pause = true
        input.reset = true
        input.poll()
        assertTrue(input.consumePause())
        assertTrue(input.consumeReset())
    }

    private class TestInput : BaseEmulatorInput() {
        var pause = false
        var reset = false

        override fun poll() {
            updateControlEdges(pause, reset)
        }

        override fun quitRequested() = false
    }
}
