package com.reno.mediadecoder

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder


fun ByteArray.toInt(): Int {
    var result = 0
    var shift = 0
    for (byte in this) {
        result = result or (byte.toInt() shl shift)
        shift += 8
    }
    return result
}

fun Byte.toPositiveInt() = toInt() and 0xFF

fun ByteArray.convertInteger(): Int = ByteBuffer.wrap(this).order(ByteOrder.LITTLE_ENDIAN).int

fun ByteArray.toHex(): String {
    val sb = StringBuilder()
    for (byte in this) {
        sb.append(byte.toHex())
    }
    return sb.toString()
}

fun Byte.toHex(): String {
    return String.format("%02X ", this)
}

@Suppress("BlockingMethodInNonBlockingContext")
suspend fun inputStreamToByteArray(`is`: InputStream): ByteArray = withContext(Dispatchers.IO) {
    lateinit var resBytes: ByteArray
    val bos = ByteArrayOutputStream()

    val buffer = ByteArray(1024)
    var read = `is`.read(buffer)

    while (read != -1) {
        bos.write(buffer, 0, read)
        read = `is`.read(buffer)
    }

    resBytes = bos.toByteArray()
    bos.close()

    resBytes
}