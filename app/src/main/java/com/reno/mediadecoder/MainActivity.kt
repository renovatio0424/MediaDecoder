package com.reno.mediadecoder

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.reno.mediadecoder.ParserType.Companion.EXTRA_PARSER
import kotlinx.android.synthetic.main.activity_main.*
import java.io.InputStream

class MainActivity: AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        butBmpParser.setOnClickListener {
            startParserActivity(ParserType.BMP)

        }

        butJpgParser.setOnClickListener {
            startParserActivity(ParserType.JPG)
        }
    }

    private fun startParserActivity(parserType: ParserType) {
        Intent(this, ParserActivity::class.java).let {
            it.putExtra(EXTRA_PARSER, parserType.value)
            startActivity(it)
        }
    }
}

enum class ParserType(val value:Int) {
    BMP(0),
    JPG(1);

    companion object {
        fun createParser(value: Int, context: Context): MediaFormatParser {
            return when(value) {
                BMP.value -> BmpParser(context.resources.openRawResource(R.raw.sample_bmp))
                JPG.value -> JpgParser(context.resources.openRawResource(R.raw.sample_jpg2))
                else -> throw Exception("invalid parser value")
            }
        }

        const val EXTRA_PARSER = "PARSER_TYPE"
    }
}