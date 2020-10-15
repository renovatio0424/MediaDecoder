package com.reno.mediadecoder

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.InputStream

val INDEX_RANGE_BITMAP_FILE_HEADER = 0..13

val INDEX_RANGE_IDENTIFY = 0..1
val INDEX_RANGE_FILE_SIZE = 2..5
val INDEX_RANGE_RESERVED1 = 6..7
val INDEX_RANGE_RESERVED2 = 8..9
val INDEX_RANGE_OFFSET = 10..13

interface MediaFormatParser {
    suspend fun getHeader(): String
    suspend fun getBody(): ByteArray
}

class BmpParser(
    private val inputStream: InputStream
) : MediaFormatParser {
    private lateinit var fullByteArray: ByteArray

    override suspend fun getHeader(): String = withContext(Dispatchers.Default) {
        val sb = StringBuilder()
        if (!::fullByteArray.isInitialized)
            fullByteArray = inputStreamToByteArray(inputStream)
        Log.d("body", "body byte arr size1 : ${fullByteArray.size}")

        val bfHeaderBytes = fullByteArray.sliceArray(INDEX_RANGE_BITMAP_FILE_HEADER)
        val bfh = parseBitmapFileHeader(bfHeaderBytes)
        val bih = fullByteArray.sliceArray(14..53)
        val dib = parseBitmapInfoHeader(bih)

        sb.append(bfh)
        sb.append(dib)
        sb.toString()
    }

    override suspend fun getBody(): ByteArray = withContext(Dispatchers.Default){
        if (!::fullByteArray.isInitialized) {
            fullByteArray = inputStreamToByteArray(inputStream)
        }

        fullByteArray
    }

    private fun parseBitmapInfoHeader(headerBytes: ByteArray): String {
        val headerSize = headerBytes.sliceArray(0..3).convertInteger()
        val width = headerBytes.sliceArray(4..7).convertInteger()
        val height = headerBytes.sliceArray(8..11).convertInteger()
        val colorPlane = headerBytes.sliceArray(12..13).toInt()
        val bitPerPixel = headerBytes.sliceArray(14..15).toInt()
        val compressionMethod =
            BitmapCompressionMethod.fromValue(headerBytes.sliceArray(16..19).convertInteger())?.toString()
        val imageSize = headerBytes.sliceArray(20..23).convertInteger()
        val horizontalResolution = headerBytes.sliceArray(24..27).convertInteger()
        val verticalResolution = headerBytes.sliceArray(28..31).convertInteger()
        val numberOfColor = headerBytes.sliceArray(32..35).convertInteger()
        val numberOfImportantColor = headerBytes.sliceArray(36..39).convertInteger()

        val sb = StringBuilder()
        sb.append("\nBitmap Info Header\n\n")
        sb.append("total hex : ${headerBytes.toHex()}\n")
        sb.append("header size: $headerSize\n")
        sb.append("width: $width\n")
        sb.append("height: $height\n")
        sb.append("color plane: $colorPlane\n")
        sb.append("bit per pixel: $bitPerPixel\n")
        sb.append("compression method: $compressionMethod\n")
        sb.append("image size: $imageSize\n")
        sb.append("horizontal resolution: $horizontalResolution\n")
        sb.append("vertical resolution: $verticalResolution\n")
        sb.append("number of color: $numberOfColor\n")
        sb.append("number of important color: $numberOfImportantColor\n")

        return sb.toString()
    }

    private fun parseBitmapFileHeader(headerBytes: ByteArray): String {
        val identify = headerBytes.sliceArray(INDEX_RANGE_IDENTIFY).toString(Charsets.UTF_8)
        val fileSize = headerBytes.sliceArray(INDEX_RANGE_FILE_SIZE).convertInteger()
        val reserved1 = headerBytes.sliceArray(INDEX_RANGE_RESERVED1).toInt()
        val reserved2 = headerBytes.sliceArray(INDEX_RANGE_RESERVED2).toInt()
        val offset = headerBytes.sliceArray(INDEX_RANGE_OFFSET).toHex()

        val sb = StringBuilder()
        sb.append("BitmapFileHeader\n\n")
        sb.append("total hex : ${headerBytes.toHex()}\n")
        sb.append("identify: $identify\n")
        sb.append("fileSize: $fileSize\n")
        sb.append("reserved1: $reserved1\n")
        sb.append("reserved2: $reserved2\n")
        sb.append("offset: $offset\n")

        return sb.toString()
    }

}

enum class BitmapCompressionMethod(
    val value: Int,
    private val methodName: String
) {
    BI_RGB(0, "none"),
    BI_RLE8(1, "RLE 8-bit/pixel"),
    BI_RLE4(2, "RLE 4-bit/pixel"),
    BI_BITFIELDS(3, "OS22XBITMAPHEADER: Huffman 1D"),
    BI_JPEG(4, "OS22XBITMAPHEADER: RLE-24"),
    BI_PNG(5, ""),
    BI_ALPHABITFIELDS(6, "RGBA bit field masks"),
    BI_CMYK(11, "none"),
    BI_CMYKRLE8(12, "RLE-8"),
    BI_CMYKRLE4(13, "RLE-4");

    override fun toString(): String {
        return "$name / $methodName"
    }

    companion object {
        fun fromValue(value: Int): BitmapCompressionMethod? {
            return when (value) {
                0 -> BI_RGB
                1 -> BI_RLE8
                2 -> BI_RLE4
                3 -> BI_BITFIELDS
                4 -> BI_JPEG
                5 -> BI_PNG
                6 -> BI_ALPHABITFIELDS
                11 -> BI_CMYK
                12 -> BI_CMYKRLE8
                13 -> BI_CMYKRLE4
                else -> null
            }
        }
    }
}
