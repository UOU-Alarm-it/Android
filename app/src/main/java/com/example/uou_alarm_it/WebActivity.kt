package com.example.uou_alarm_it

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.uou_alarm_it.databinding.ActivityWebBinding

class WebActivity : AppCompatActivity(){
    lateinit var binding : ActivityWebBinding
    var url : String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWebBinding.inflate(layoutInflater)
        val intent = getIntent()

        if (intent.hasExtra("url")) {
            url = intent.getStringExtra("url")!!
        }

        Log.d("getExtra URL", url)

        binding.webWebviewWv.loadUrl(url)

        binding.webBackBtnIv.setOnClickListener {
            finish()
        }

        binding.webLinkBtnIv.setOnClickListener {
            val browserIntent  = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(browserIntent)
        }

        setContentView(binding.root)
    }
}