package com.reno.mediadecoder

import android.graphics.DiscretePathEffect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.lang.StringBuilder

class JpegParser(private val inputStream: InputStream) : MediaFormatParser {
    private lateinit var byteArray: ByteArray
    private var headerLength: Int = 0

    override suspend fun getHeader(): String = withContext(Dispatchers.Default) {
        val sb = StringBuilder()
        if (!::byteArray.isInitialized)
            byteArray = inputStreamToByteArray(inputStream)

        val lastIndex = byteArray.lastIndex

        sb.append("JPG Information\n\n")

        sb.append("size : ${byteArray.size}")
        sb.append("40 : ${byteArray.sliceArray(0..39).toHex()}\n")
        sb.append(parseSOI(byteArray.sliceArray(0..1)))
        sb.append(parseJFIF_APPO(byteArray.sliceArray(2..19)))
        sb.append(parserMarker(byteArray.sliceArray(20..(headerLength - 2))))
        sb.append(parseEOI(byteArray.sliceArray((byteArray.lastIndex - 1)..byteArray.lastIndex)))

        sb.toString()
    }

    private fun parseEOI(byteArray: ByteArray): String {
        return "EOI : ${byteArray.toHex()}\n"
    }

    private fun parserMarker(byteArray: ByteArray): String {
        val sb = StringBuilder()
        var markCount = 1

        for ((index, byte) in byteArray.withIndex()) {
            if (byte.toHex().contains("FF")) {
                sb.append("marker $markCount: ")
                val marker = "${byte.toHex()}${byteArray[index + 1].toHex()}".replace(" ", "")
                val markerName = when {
                    marker.contains("FFD8") -> "SOI (Start Of Image)"
                    marker.contains("FFC0") -> "SOFO (Start Of Frame)"
                    marker.contains("FFC2") -> "SOF2 (Start Of Frame)"
                    marker.contains("FFC4") -> "DHT (Define Huffman Tables)"
                    marker.contains("FFDB") -> "DQT (Define Quantization Tables)"
                    marker.contains("FFDD") -> "DRI (Define Restart Interval)"
                    marker.contains("FFDA") -> "SOS (Start of Scan)"
                    marker.contains("FFD0") ||
                            marker.contains("FFD1") ||
                            marker.contains("FFD2") ||
                            marker.contains("FFD3") ||
                            marker.contains("FFD4") ||
                            marker.contains("FFD5") ||
                            marker.contains("FFD6") ||
                            marker.contains("FFD7") -> "Restart"
                    marker.contains("FFE0") -> "Application-specific"
                    marker.contains("FFFE") -> "Comment"
                    marker.contains("FFD9") -> "End Of Image"
                    else -> "Unknown: $marker"
                }
                sb.append(markerName)
                sb.append("/ marker index : $index\n")
                markCount++
            }

        }

        return sb.toString()
    }

    private fun parseJFIF_APPO(byteArray: ByteArray): String {
        val appoMarker = byteArray.sliceArray(0..1).toHex()
        headerLength = byteArray.sliceArray(2..3).toInt()
        val identifier = byteArray.sliceArray(4..8).toHex()
        val jfifVersion =
            "${byteArray[9].toInt()}.${byteArray[10].toInt()}"//byteArray.sliceArray(9..10).convertInteger()
        val densityUnit = byteArray[11].toInt()
        val xDensity = byteArray.sliceArray(12..13).toInt()
        val yDensity = byteArray.sliceArray(14..15).toInt()
        val xThumbnail = byteArray[16].toInt()
        val yThumbnail = byteArray[17].toInt()

        val sb = StringBuilder()
        sb.append("JFIF APPO\n")
        sb.append("Hex : ${byteArray.toHex()}\n")
        sb.append("appoMarker : $appoMarker\n")
        sb.append("length : $headerLength\n")
        sb.append("identifier : $identifier\n")
        sb.append("jfif version : $jfifVersion\n")
        sb.append("density unit : $densityUnit\n")
        sb.append("x density : $xDensity\n")
        sb.append("y density : $yDensity\n")
        sb.append("x thumbnail : $xThumbnail\n")
        sb.append("y thumbnail : $yThumbnail\n")

        return sb.toString()
    }

    private fun parseSOI(byteArray: ByteArray): String {
        val sb = StringBuilder()
        sb.append("JPG SOI\n")
        sb.append("SOI : ${byteArray.toHex()}\n\n")
        return sb.toString()
    }

    override suspend fun getBody(): ByteArray = withContext(Dispatchers.Default) {
        if (!::byteArray.isInitialized)
            byteArray = inputStreamToByteArray(inputStream)
        byteArray
    }

}