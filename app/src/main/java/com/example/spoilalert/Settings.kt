package com.example.spoilalert

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.spoilalert.databinding.ActivitySettingsBinding

class Settings : AppCompatActivity() {
    private lateinit var binding: ActivitySettingsBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        supportActionBar?.hide()
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

//    override fun onBackPressed() {
//        super.onBackPressed()
//    }
}