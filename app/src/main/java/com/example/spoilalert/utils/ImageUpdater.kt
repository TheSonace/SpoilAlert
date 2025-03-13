package com.example.spoilalert.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.text.format.Formatter
import android.util.Log
import com.example.spoilalert.Database
import java.io.File
import java.io.FileOutputStream
import java.lang.String.valueOf
import kotlin.math.floor

class UpdateAndSaveImageTask(context: Context, filename: Long, database: Database, bitmap: Bitmap?) {
    private var mContext: Context = context
    private var fileName: Long = filename
    private var bitMap: Bitmap? = bitmap
    private val productQueries = database.productQueries
    private lateinit var file: File

    fun saveImage(): Any {
        try {
            file = File(mContext.filesDir, "Products")
            if (!file.exists()) {
                file.mkdir()
            }
            file = File(file, "$fileName.jpg")
            val out = FileOutputStream(file)
            val newBitmap = Bitmap.createBitmap(
                bitMap!!.width,
                bitMap!!.height,
                bitMap!!.config
            )
            val canvas = Canvas(newBitmap)
            canvas.drawColor(Color.WHITE)
            Thread.sleep(200)
            canvas.drawBitmap(bitMap!!, Matrix(), null)
            // Quality 95 results in images of around 200Kb, comparable to OpenFoods.
            // Direct export to PNG is dependent on camera (Emulator is 1500Kb
            newBitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
            out.flush()
            out.close()

            Log.e("File size?", floor(file.length() / 1000.0 + 0.5).toInt().toString())
            Log.e("image updated", file.toString())
            productQueries.update_image(file.toString(), fileName)
            return file
        }
         catch (e: Exception) {
            return false
        }
    }
}