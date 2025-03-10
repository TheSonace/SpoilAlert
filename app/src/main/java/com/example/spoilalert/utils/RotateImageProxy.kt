package com.example.spoilalert.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.camera.core.ImageProxy
import java.nio.ByteBuffer


fun rotateImageProxy(image: ImageProxy): Bitmap? {
    val rotation = image.imageInfo.rotationDegrees
    val buffer: ByteBuffer = image.planes[0].buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)
    val bitmimage = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    val matrix = Matrix()
    matrix.setRotate(rotation.toFloat())
    val rotatedImage = bitmimage?.let { it1 ->
        Bitmap.createBitmap(
            it1,
            0,
            0,
            bitmimage.width,
            bitmimage.height,
            matrix,
            true
        )
    }
    return rotatedImage
}