package com.example.spoilalert


import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher

import com.example.spoilalert.databinding.BarcodePreviewBinding

class BarCodePreview(context: Context) : Dialog(context) {
    private lateinit var binding: BarcodePreviewBinding
    private var dialog = Dialog(context)

    fun showPopup() {
        binding = BarcodePreviewBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val dialogview = LayoutInflater.from(context).inflate(null, binding.root)
        dialog.setCancelable(true)
        dialog.setContentView(dialogview)
        dialog.show()
    }
    companion object{
        var dialog: Dialog? = null
        fun dismissPopup() = dialog?.let { dialog!!.dismiss() }
    }
}
