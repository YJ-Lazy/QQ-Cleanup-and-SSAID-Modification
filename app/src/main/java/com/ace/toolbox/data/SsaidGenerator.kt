package com.ace.toolbox.data

import java.security.SecureRandom

/** Generates a 16-character lowercase hexadecimal SSAID value (8 random bytes). */
object SsaidGenerator {
    private val random = SecureRandom()
    private val hex = "0123456789abcdef".toCharArray()

    fun randomHex16(): String {
        val bytes = ByteArray(8)
        random.nextBytes(bytes)
        val out = CharArray(16)
        var i = 0
        for (b in bytes) {
            val v = b.toInt() and 0xff
            out[i++] = hex[v ushr 4]
            out[i++] = hex[v and 0x0f]
        }
        return String(out)
    }
}
