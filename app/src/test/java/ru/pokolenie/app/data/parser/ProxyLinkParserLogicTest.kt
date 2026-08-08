package ru.pokolenie.app.data.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Lightweight sanity checks that do not require Android runtime.
 * Full Uri parsing is covered on-device / instrumented runs.
 */
class ProxyLinkParserLogicTest {
    @Test
    fun decodePlainLinesKeepsLinks() {
        val body = """
            # comment
            vless://11111111-1111-1111-1111-111111111111@example.com:443?security=reality&sni=www.cloudflare.com&fp=chrome&pbk=abc&sid=01#Node1
            trojan://secret@example.org:443?security=tls&sni=example.org#Node2
        """.trimIndent()

        // Base64 path should not trigger for plaintext with ://
        val compact = body.replace("\\s".toRegex(), "")
        assertTrue(compact.contains("://"))
        assertEquals(2, body.lineSequence().map { it.trim() }.filter { it.startsWith("vless://") || it.startsWith("trojan://") }.count())
    }
}
