package com.reno.mediadecoder

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream

class JpegParser(private val inputStream: InputStream) : MediaFormatParser {
    private lateinit var byteArray: ByteArray
    private var headerLength: Int = 0

    override suspend fun getHeader(): String = withContext(Dispatchers.Default) {
        val sb = StringBuilder()
        if (!::byteArray.isInitialized)
            byteArray = inputStreamToByteArray(inputStream)

        val lastIndex = byteArray.lastIndex

        sb.append("JPG Information\n\n")
        sb.append(parserMarker(byteArray))

        sb.toString()
    }

    private fun parseEOI(byteArray: ByteArray): String {
        return "EOI : ${byteArray.toHex()}\n"
    }

    private fun parserMarker(byteArray: ByteArray): String {
        val sb = StringBuilder()
        val markerList = arrayListOf<Triple<String, Int, Int>>()
        var prevMarkerName: String? = null
        var prevStartIdx = -1

        for (index in 0 until byteArray.lastIndex step 2) {
            val marker = byteArray[index + 1].toHex().replace(" ", "")
            if (byteArray[index].toHex().replace(" ", "") == "FF") {
                val markerName = when (marker) {
                    "D8" -> "SOI (Start Of Image)"
                    "C0" -> "SOFO (Start Of Frame)"
                    "C2" -> "SOF2 (Start Of Frame)"
                    "C4" -> "DHT (Define Huffman Tables)"
                    "DB" -> "DQT (Define Quantization Tables)"
                    "DD" -> "DRI (Define Restart Interval)"
                    "DA" -> "SOS (Start of Scan)"
                    "D0", "D1", "D2", "D3", "D4", "D5", "D6", "D7" -> "RSTn (Restart)"
                    "E0", "E1", "E2", "E3", "E4", "E5", "E6", "E7", "E8", "E9" -> "APPn (Application-specific)"
                    "FE" -> "COM (Comment)"
                    "D9" -> "EOI (End Of Image)"
                    else -> continue
                }

                if (prevMarkerName == null || prevStartIdx == -1) {
                    prevMarkerName = markerName
                    prevStartIdx = index
                    continue
                }

                markerList.add(Triple(prevMarkerName, prevStartIdx, index - 1))
                prevMarkerName = markerName
                prevStartIdx = index
            }
        }

        for ((idx, marker) in markerList.withIndex()) {
            sb.append("marker$idx : ${marker.first}\n")
            val parseString = when {
                marker.first.contains("SOI") -> parseSOI(byteArray.sliceArray(marker.second..marker.third))
                marker.first.contains("APPn") -> parseJFIF_APPO(byteArray.sliceArray(marker.second..marker.third))
                marker.first.contains("EOI") -> parseEOI(byteArray.sliceArray(marker.second..marker.third))
                else -> ""
            }
            sb.append(parseString)
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
        sb.append("SOI : ${byteArray.toHex()}\n")
        return sb.toString()
    }

    override suspend fun getBody(): ByteArray = withContext(Dispatchers.Default) {
        if (!::byteArray.isInitialized)
            byteArray = inputStreamToByteArray(inputStream)
        byteArray
    }

}