package com.reno.mediadecoder

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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

        val bfh = parseBitmapFileHeader(fullByteArray.sliceArray(INDEX_RANGE_BITMAP_FILE_HEADER))
        val dib = parseBitmapInfoHeader(fullByteArray.sliceArray(14..53))
        val pixelStartIdx = getPixelStartIdx(fullByteArray, bfh.offset) //todo offset index 임 찾는게 아니구
        val pixelColor = parseToAscii(fullByteArray.sliceArray(pixelStartIdx..fullByteArray.lastIndex), dib.width)

        sb.append(bfh)
        sb.append(dib)
        sb.append(pixelColor)

        sb.toString()
    }

    private fun getPixelStartIdx(fullByteArray: ByteArray, offset: String): Int {
        return fullByteArray.toHex().indexOf(offset)
    }

    private fun parsePixelColor(pixelByteArray: ByteArray): String {
        val sb = StringBuilder()
        for (idx in pixelByteArray.indices step 3) {
            sb.append("R[${pixelByteArray[idx].toHex()}] ")
            sb.append("G[${pixelByteArray[idx + 1].toHex()}] ")
            sb.append("B[${pixelByteArray[idx + 2].toHex()}] |")
        }
        return sb.toString()
    }

    private fun parseToAscii(pixelByteArray: ByteArray, width: Int): String {
        val sb = StringBuilder()
        val asciis = charArrayOf('#', '#', '@', '%', '=', '*', '+', ':', '-', '.', '.')
        for (idx in pixelByteArray.indices step width * 3) {
            for (colorByteIdx in idx until (idx + width) step 3) {
                try {
                    val colorR = pixelByteArray[colorByteIdx].toPositiveInt()
                    val colorG = pixelByteArray[colorByteIdx + 1].toPositiveInt()
                    val colorB = pixelByteArray[colorByteIdx + 2].toPositiveInt()
                    val grey = (colorR + colorG + colorB) / 3

                    val ascii = asciis[grey * asciis.size / 256]
                    sb.append(ascii)
                } finally {
                    continue
                }
            }
            sb.append("\n")
        }
        return sb.toString()
    }

    override suspend fun getBody(): ByteArray = withContext(Dispatchers.Default) {
        if (!::fullByteArray.isInitialized) {
            fullByteArray = inputStreamToByteArray(inputStream)
        }

        fullByteArray
    }

    private fun parseBitmapInfoHeader(headerBytes: ByteArray): BitmapInfoHeader {
        val headerSize = headerBytes.sliceArray(0..3).convertInteger()
        val width = headerBytes.sliceArray(4..7).convertInteger()
        val height = headerBytes.sliceArray(8..11).convertInteger()
        val colorPlane = headerBytes.sliceArray(12..13).toInt()
        val bitPerPixel = headerBytes.sliceArray(14..15).toInt()
        val compressionMethod =
            BitmapCompressionMethod.fromValue(headerBytes.sliceArray(16..19).convertInteger())
        val imageSize = headerBytes.sliceArray(20..23).convertInteger()
        val horizontalResolution = headerBytes.sliceArray(24..27).convertInteger()
        val verticalResolution = headerBytes.sliceArray(28..31).convertInteger()
        val numberOfColor = headerBytes.sliceArray(32..35).convertInteger()
        val numberOfImportantColor = headerBytes.sliceArray(36..39).convertInteger()

        return BitmapInfoHeader(
            headerSize,
            width,
            height,
            colorPlane,
            bitPerPixel,
            compressionMethod,
            imageSize,
            horizontalResolution,
            verticalResolution,
            numberOfColor,
            numberOfImportantColor
        )
    }

    private fun parseBitmapFileHeader(headerBytes: ByteArray): BitmapFileHeader {
        val identify = headerBytes.sliceArray(INDEX_RANGE_IDENTIFY).toString(Charsets.UTF_8)
        val fileSize = headerBytes.sliceArray(INDEX_RANGE_FILE_SIZE).convertInteger()
        val reserved1 = headerBytes.sliceArray(INDEX_RANGE_RESERVED1).toInt()
        val reserved2 = headerBytes.sliceArray(INDEX_RANGE_RESERVED2).toInt()
        val offset = headerBytes.sliceArray(INDEX_RANGE_OFFSET).toHex()

        return BitmapFileHeader(identify, fileSize, reserved1, reserved2, offset)
    }
}

data class BitmapInfoHeader(
    private val headerSize: Int,
    val width: Int,
    private val height: Int,
    private val colorPlane: Int,
    private val bitPerPixel: Int,
    private val compressionMethod: BitmapCompressionMethod,
    private val imageSize: Int,
    private val horizontalResolution: Int,
    private val verticalResolution: Int,
    private val numberOfColor: Int,
    private val numberOfImportantColor: Int
) {
    override fun toString(): String {
        val sb = StringBuilder()
        sb.append("Bitmap Info Header\n\n")
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
}

data class BitmapFileHeader(
    private val identify: String,
    private val fileSize: Int,
    private val reserved1: Int,
    private val reserved2: Int,
    val offset: String
) {
    override fun toString(): String {
        val sb = StringBuilder()
        sb.append("BitmapFileHeader\n\n")
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
        fun fromValue(value: Int): BitmapCompressionMethod {
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
                else -> throw Exception("Invalid Bitmap Compression Method")
            }
        }
    }
}
