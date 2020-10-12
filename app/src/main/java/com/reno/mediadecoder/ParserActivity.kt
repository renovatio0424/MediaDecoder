package com.reno.mediadecoder

import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.reno.mediadecoder.ParserType.Companion.EXTRA_PARSER
import kotlinx.android.synthetic.main.activity_parser.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

class ParserActivity : AppCompatActivity(), CoroutineScope {
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_parser)
        intent.extras

        val parser: MediaFormatParser = ParserType.createParser(intent.getIntExtra(EXTRA_PARSER, -1), baseContext)

        pbLoading.visibility = View.VISIBLE

        launch {
            val result = parser.getHeader()
            pbLoading.visibility = View.INVISIBLE
            tvParser.text = result
            val bmByte = parser.getBody()
            Log.d("bm", "bm: ${bmByte.size}")
//            val bm = Bitmap.createBitmap(bmByte, 300, 300, Bitmap.Config.ARGB_8888);
            val bm = BitmapFactory.decodeByteArray(bmByte, 0, bmByte.size)
            Log.d("bm", "bm: $bm")
            ivResult.setImageBitmap(bm)
        }
    }
}