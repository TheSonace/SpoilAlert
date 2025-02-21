package com.example.spoilalert.utils

import android.content.Context
import android.graphics.Bitmap
import android.os.AsyncTask
import android.util.Log
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.DownsampleStrategy
import com.bumptech.glide.request.RequestOptions
import java.io.File
import java.io.FileOutputStream
import java.lang.ref.WeakReference

class DownloadAndSaveImageTask(context: Context, filename: String) : AsyncTask<String, Unit, Unit>() {
    private var mContext: WeakReference<Context> = WeakReference(context)
    private var fileName: String = filename

    override fun doInBackground(vararg params: String?) {
        val url = params[0]
        val requestOptions = RequestOptions()
            .downsample(DownsampleStrategy.CENTER_INSIDE)
            .skipMemoryCache(true)
            .diskCacheStrategy(DiskCacheStrategy.NONE)

        mContext.get()?.let {
            val bitmap = Glide.with(it)
                .asBitmap()
                .load(url)
                .apply(requestOptions)
                .submit()
                .get()

            try {
                var file = File(it.filesDir, "Products")
                if (!file.exists()) {
                    file.mkdir()
                }
                file = File(file, "$fileName.jpg")
                if (!file.exists()) {
                    val out = FileOutputStream(file)
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
                    out.flush()
                    out.close()
                    Log.i("SpoilAlert", "Image saved.")
                } else {}
            } catch (e: Exception) {
                Log.i("SpoilAlert", "Failed to save image.")
            }
        }
    }
}