package frontend

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EmulatorInputTest {
    @Test
    fun controlActionsAreReportedOncePerPress() {
        val input = TestInput()

        input.reset = true
        input.poll()
        assertTrue(input.consumeReset())
        assertFalse(input.consumeReset())

        input.poll()
        assertFalse(input.consumeReset())

        input.reset = false
        input.poll()
        input.reset = true
        input.poll()
        assertTrue(input.consumeReset())
    }

    private class TestInput : BaseEmulatorInput() {
        var reset = false

        override fun poll() {
            updateControlEdges(reset)
        }

        override fun quitRequested() = false
    }
}
