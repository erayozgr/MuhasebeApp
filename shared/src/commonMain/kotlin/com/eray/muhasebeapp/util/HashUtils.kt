package com.eray.muhasebeapp.util

import kotlin.experimental.and

object HashUtils {
    /**
     * Computes SHA-256 hash of the given string and returns it as a Hex string.
     */
    fun sha256(input: String): String {
        val bytes = input.encodeToByteArray()
        val md = Sha256Digest()
        md.update(bytes, 0, bytes.size)
        val result = ByteArray(32)
        md.doFinal(result, 0)
        return result.toHexString()
    }

    private fun ByteArray.toHexString(): String {
        return joinToString("") {
            (it.toInt() and 0xFF).toString(16).padStart(2, '0')
        }
    }
}

/**
 * Simple SHA-256 implementation in pure Kotlin.
 */
private class Sha256Digest {
    private val h = IntArray(8)
    private val w = IntArray(64)
    private var buffer = ByteArray(64)
    private var bufferPos = 0
    private var count = 0L

    init {
        reset()
    }

    fun reset() {
        h[0] = 0x6a09e667.toInt()
        h[1] = 0xbb67ae85.toInt()
        h[2] = 0x3c6ef372.toInt()
        h[3] = 0xa54ff53a.toInt()
        h[4] = 0x510e527f.toInt()
        h[5] = 0x9b05688c.toInt()
        h[6] = 0x1f83d9ab.toInt()
        h[7] = 0x5be0cd19.toInt()
        bufferPos = 0
        count = 0L
    }

    fun update(input: ByteArray, offset: Int, len: Int) {
        var i = 0
        while (i < len) {
            buffer[bufferPos++] = input[offset + i]
            if (bufferPos == 64) {
                processBlock()
                bufferPos = 0
            }
            i++
        }
        count += len
    }

    fun doFinal(out: ByteArray, outOffset: Int) {
        val bitCount = count * 8
        update(byteArrayOf(0x80.toByte()), 0, 1)
        while (bufferPos != 56) {
            update(byteArrayOf(0x00.toByte()), 0, 1)
        }
        
        update(byteArrayOf(
            (bitCount ushr 56).toByte(),
            (bitCount ushr 48).toByte(),
            (bitCount ushr 40).toByte(),
            (bitCount ushr 32).toByte(),
            (bitCount ushr 24).toByte(),
            (bitCount ushr 16).toByte(),
            (bitCount ushr 8).toByte(),
            (bitCount ushr 0).toByte()
        ), 0, 8)

        for (i in 0..7) {
            out[outOffset + i * 4 + 0] = (h[i] ushr 24).toByte()
            out[outOffset + i * 4 + 1] = (h[i] ushr 16).toByte()
            out[outOffset + i * 4 + 2] = (h[i] ushr 8).toByte()
            out[outOffset + i * 4 + 3] = (h[i] ushr 0).toByte()
        }
    }

    private fun processBlock() {
        for (i in 0..15) {
            w[i] = ((buffer[i * 4 + 0].toInt() and 0xff) shl 24) or
                   ((buffer[i * 4 + 1].toInt() and 0xff) shl 16) or
                   ((buffer[i * 4 + 2].toInt() and 0xff) shl 8) or
                   (buffer[i * 4 + 3].toInt() and 0xff)
        }
        for (i in 16..63) {
            val s0 = (w[i - 15] ushr 7 or (w[i - 15] shl 25)) xor
                     (w[i - 15] ushr 18 or (w[i - 15] shl 14)) xor
                     (w[i - 15] ushr 3)
            val s1 = (w[i - 2] ushr 17 or (w[i - 2] shl 15)) xor
                     (w[i - 2] ushr 19 or (w[i - 2] shl 13)) xor
                     (w[i - 2] ushr 10)
            w[i] = s1 + w[i - 7] + s0 + w[i - 16]
        }

        var a = h[0]; var b = h[1]; var c = h[2]; var d = h[3]
        var e = h[4]; var f = h[5]; var g = h[6]; var hh = h[7]

        for (i in 0..63) {
            val s1 = (e ushr 6 or (e shl 26)) xor
                     (e ushr 11 or (e shl 21)) xor
                     (e ushr 25 or (e shl 7))
            val ch = (e and f) xor (e.inv() and g)
            val temp1 = hh + s1 + ch + K[i] + w[i]
            val s0 = (a ushr 2 or (a shl 30)) xor
                     (a ushr 13 or (a shl 19)) xor
                     (a ushr 22 or (a shl 10))
            val maj = (a and b) xor (a and c) xor (b and c)
            val temp2 = s0 + maj

            hh = g; g = f; f = e; e = d + temp1
            d = c; c = b; b = a; a = temp1 + temp2
        }

        h[0] += a; h[1] += b; h[2] += c; h[3] += d
        h[4] += e; h[5] += f; h[6] += g; h[7] += hh
    }

    companion object {
        private val K = intArrayOf(
            0x428a2f98.toInt(), 0x71374491.toInt(), 0xb5c0fbcf.toInt(), 0xe9b5dba5.toInt(),
            0x3956c25b.toInt(), 0x59f111f1.toInt(), 0x923f82a4.toInt(), 0xab1c5ed5.toInt(),
            0xd807aa98.toInt(), 0x12835b01.toInt(), 0x243185be.toInt(), 0x550c7dc3.toInt(),
            0x72be5d74.toInt(), 0x80deb1fe.toInt(), 0x9bdc06a7.toInt(), 0xc19bf174.toInt(),
            0xe49b69c1.toInt(), 0xefbe4786.toInt(), 0x0fc19dc6.toInt(), 0x240ca1cc.toInt(),
            0x2de92c6f.toInt(), 0x4a7484aa.toInt(), 0x5cb0a9dc.toInt(), 0x76f988da.toInt(),
            0x983e5152.toInt(), 0xa831c66d.toInt(), 0xb00327c8.toInt(), 0xbf597fc7.toInt(),
            0xc6e00bf3.toInt(), 0xd5a79147.toInt(), 0x06ca6351.toInt(), 0x14292967.toInt(),
            0x27b70a85.toInt(), 0x2e1b2138.toInt(), 0x4d2c6dfc.toInt(), 0x53380d13.toInt(),
            0x650a7354.toInt(), 0x766a0abb.toInt(), 0x81c2c92e.toInt(), 0x92722c85.toInt(),
            0xa2bfe8a1.toInt(), 0xa81a664b.toInt(), 0xc24b8b70.toInt(), 0xc76c51a3.toInt(),
            0xd192e819.toInt(), 0xd6990624.toInt(), 0xf40e3585.toInt(), 0x106aa070.toInt(),
            0x19a4c116.toInt(), 0x1e376c08.toInt(), 0x2748774c.toInt(), 0x34b0bcb5.toInt(),
            0x391c0cb3.toInt(), 0x4ed8aa4a.toInt(), 0x5b9cca4f.toInt(), 0x682e6ff3.toInt(),
            0x748f82ee.toInt(), 0x78a5636f.toInt(), 0x84c87814.toInt(), 0x8cc70208.toInt(),
            0x90befffa.toInt(), 0xa4506ceb.toInt(), 0xbef9a3f7.toInt(), 0xc67178f2.toInt()
        )
    }
}
