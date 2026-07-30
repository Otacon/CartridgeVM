package io

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class Sha1Test {
    @Test
    fun `calculates SHA-1 hex`() = runBlocking {
        assertEquals(
            "a9993e364706816aba3e25717850c26c9cd0d89d",
            sha1Hex("abc".encodeToByteArray()),
        )
    }
}
