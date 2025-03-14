package com.example.spoilalert.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.example.spoilalert.databinding.ActivityMainProductInfoBinding
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

fun loadImageFromWebOperations(src: String, prodInfo: ActivityMainProductInfoBinding): Bitmap? {
    try {
        Log.e("src", src)
        val url = URL(src)
        val connection = url.openConnection() as HttpURLConnection
        connection.doInput = true
        connection.connect()
        val input = connection.inputStream
        val myBitmap = BitmapFactory.decodeStream(input)
        Log.e("Bitmap", "returned")
        prodInfo.imageView.setImageBitmap(myBitmap)
        return myBitmap
    } catch (e: IOException) {
        e.printStackTrace()
        Log.e("Exception", e.message!!)
        return null
    }
}