package com.konbini.magicplateuhf.utils

import java.nio.charset.Charset
import java.util.*

object ByteUtil {
    private fun forDigit(digit: Int, radix: Int): Char {
        if (digit >= radix || digit < 0) {
            return '\u0000'
        }
        if (radix < Character.MIN_RADIX || radix > Character.MAX_RADIX) {
            return '\u0000'
        }
        return if (digit < 10) {
            ('0'.toInt() + digit).toChar()
        } else ('A'.toInt() - 10 + digit).toChar()
    }

    fun byte2Hex(num: Byte): String {
        return value2Hex(num)
    }

    fun short2Hex(num: Short): String {
        return value2Hex(num)
    }

    fun int2Hex(num: Int): String {
        return value2Hex(num)
    }

    fun long2Hex(num: Long): String {
        return value2Hex(num)
    }

    fun value2Hex(number: Number): String {
        var bytes: ByteArray? = null
        if (number is Byte) {
            bytes = long2bytes(number.toLong(), 1)
        } else if (number is Short) {
            bytes = long2bytes(number.toLong(), 2)
        } else if (number is Int) {
            bytes = long2bytes(number.toLong(), 4)
        } else if (number is Long) {
            bytes = long2bytes(number.toLong(), 8)
        }
        return bytes?.let { bytes2HexStr(it) } ?: "00"
    }

    fun bytes2HexStr(src: ByteArray?): String {
        val builder = StringBuilder()
        if (src == null || src.size <= 0) {
            return ""
        }
        val buffer = CharArray(2)
        for (i in src.indices) {
            buffer[0] = forDigit(src[i].toInt() ushr 4 and 0x0F, 16)
            buffer[1] = forDigit(src[i].toInt() and 0x0F, 16)
            builder.append(buffer)
        }
        return builder.toString()
    }

    fun bytes2Ascii(src : ByteArray) : String {
        return String(src, charset("UTF-8"))
    }

    fun bytes2HexStrWithSpace(src: ByteArray?): String {
        val builder = StringBuilder()
        if (src == null || src.size <= 0) {
            return ""
        }
        val buffer = CharArray(2)
        for (i in src.indices) {
            buffer[0] = forDigit(src[i].toInt() ushr 4 and 0x0F, 16)
            buffer[1] = forDigit(src[i].toInt() and 0x0F, 16)
            builder.append(buffer)

            builder.append(" ")

        }
        return builder.toString().trim()
    }


    fun bytes2HexStr(src: ByteArray?, dec: Int, length: Int): String {
        val temp = ByteArray(length)
        System.arraycopy(src, dec, temp, 0, length)
        return bytes2HexStr(temp)
    }


    fun hexStr2decimal(hex: String): Long {
        return hex.toLong(16)
    }


    fun decimal2fitHex(num: Long): String {
        val hex = java.lang.Long.toHexString(num).toUpperCase()
        return if (hex.length % 2 != 0) {
            "0$hex"
        } else hex.toUpperCase()
    }


    fun decimal2fitHex(num: Long, strLength: Int): String {
        val hexStr = decimal2fitHex(num)
        val stringBuilder = StringBuilder(hexStr)
        while (stringBuilder.length < strLength) {
            stringBuilder.insert(0, '0')
        }
        return stringBuilder.toString()
    }


    fun fitDecimalStr(dicimal: Int, strLength: Int): String {
        val builder = StringBuilder(dicimal.toString())
        while (builder.length < strLength) {
            builder.insert(0, "0")
        }
        return builder.toString()
    }


    fun str2HexString(str: String): String {
        val chars = "0123456789ABCDEF".toCharArray()
        val sb = StringBuilder()
        var bs: ByteArray? = null
        try {
            bs = str.toByteArray(Charset.forName("utf8"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
        var bit: Int
        for (i in bs!!.indices) {
            bit = bs[i].toInt() and 0x0f0.toInt() shr 4
            sb.append(chars[bit])
            bit = bs[i].toInt() and 0x0f
            sb.append(chars[bit])
        }
        return sb.toString()
    }

    fun hexStr2bytes(hex: String): ByteArray {
        val len = hex.length / 2
        val result = ByteArray(len)
        val achar = hex.toUpperCase().toCharArray()
        for (i in 0 until len) {
            val pos = i * 2
            result[i] =
                (hexChar2byte(achar[pos]) shl 4 or hexChar2byte(achar[pos + 1])).toByte()
        }
        return result
    }

    fun long2bytes(ori: Long, arrayAmount: Int): ByteArray {
        val bytes = ByteArray(arrayAmount)
        for (i in 0 until arrayAmount) {
            bytes[i] = (ori ushr (arrayAmount - i - 1) * 8 and 0xff).toByte()
        }
        return bytes
    }


    fun long2bytes(
        ori: Long,
        targetBytes: ByteArray,
        offset: Int,
        arrayAmount: Int
    ): ByteArray {
        for (i in 0 until arrayAmount) {
            // 高位在前
            targetBytes[offset + i] =
                (ori ushr (arrayAmount - i - 1) * 8 and 0xff).toByte()
        }
        return targetBytes
    }

    @JvmOverloads
    fun bytes2long(ori: ByteArray, offset: Int = 0, len: Int = ori.size): Long {
        var result: Long = 0
        for (i in 0 until len) {
            result = result or (0xffL and ori[offset + i].toLong() shl (len - 1 - i) * 8)
        }
        return result
    }


    private fun hexChar2byte(c: Char): Int {
        return when (c) {
            '0' -> 0
            '1' -> 1
            '2' -> 2
            '3' -> 3
            '4' -> 4
            '5' -> 5
            '6' -> 6
            '7' -> 7
            '8' -> 8
            '9' -> 9
            'a', 'A' -> 10
            'b', 'B' -> 11
            'c', 'C' -> 12
            'd', 'D' -> 13
            'e', 'E' -> 14
            'f', 'F' -> 15
            else -> -1
        }
    }

    @JvmOverloads
    fun toBinString(
        value: Long,
        byteLen: Int,
        withDivider: Boolean = true
    ): String {
        var value = value
        val bitLen = byteLen * 8
        val chars = CharArray(bitLen)
        Arrays.fill(chars, '0')
        var charPos = bitLen
        do {
            --charPos
            if (value and 1 > 0) {
                chars[charPos] = '1'
            }
            value = value ushr 1
        } while (value != 0L && charPos > 0)
        if (withDivider && byteLen > 1) {
            val stringBuilder = StringBuilder()
            var alreadyAppend = false
            for (i in 0 until byteLen) {
                if (alreadyAppend) {
                    stringBuilder.append(' ')
                } else {
                    alreadyAppend = true
                }
                stringBuilder.append(chars, i * 8, 8)
            }
            return stringBuilder.toString()
        }
        return String(chars)
    }


    fun getXOR(bytes: ByteArray, offset: Int, len: Int): Byte {
        var toDiff: Byte = 0
        for (i in 0 until len) {
            toDiff = (toDiff.toInt() xor bytes[i + offset].toInt()) as Byte
        }
        return toDiff
    }

    fun getBitFromLeft(bytes: ByteArray, dataOffset: Int, bitPos: Int): Int {
        val byteIndex = (bitPos - 1) / 8
        val bitIndex = (bitPos - 1) % 8
        return if (bytes[dataOffset + byteIndex].toInt() and (1 shl bitIndex) != 0) {
            1
        } else 0
    }

    fun getCheckSum(bytes: ByteArray): Int {
        var xor = 0
        for (i in bytes.indices) xor = xor xor bytes[i].toInt()
        return xor
    }
}