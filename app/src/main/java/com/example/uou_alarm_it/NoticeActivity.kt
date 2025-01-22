package com.example.uou_alarm_it

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.uou_alarm_it.databinding.ActivityNoticeBinding

class NoticeActivity : AppCompatActivity() {
    lateinit var binding : ActivityNoticeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNoticeBinding.inflate(layoutInflater)

        setContentView(binding.root)
    }
}