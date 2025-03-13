package com.example.spoilalert.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.view.View
import com.example.ProductQueries
import com.example.spoilalert.R
import com.example.spoilalert.databinding.ActivityMainProductInfoBinding
import com.example.spoilalert.utils.getNutriScore
import java.io.File


fun populateProdPreview(
    context: Context,
    getBarcode: String,
    productQueries: ProductQueries,
    prodInfo: ActivityMainProductInfoBinding
) {
    var bitmap: Bitmap? = null
    val localProduct =
        productQueries.getlocal(getBarcode).executeAsList()[0]
    if (localProduct.status == "1") {
        Log.e("status prodinfoadapter", localProduct.status)
        val record = localProduct.RecordKey
        val file = File(
            File(context.filesDir, "Products"),
            "$record.jpg")
        Log.e("prodinfoadapterfile", file.toString())
        if (file.exists()) {
            bitmap = BitmapFactory.decodeFile(file.toString())
            prodInfo.refreshProductButton.visibility = View.VISIBLE
            prodInfo.imageView.setImageBitmap(bitmap)
            prodInfo.tvProductName.text = localProduct.product
            prodInfo.tvProductBrand.text = localProduct.brand
            prodInfo.tvbarCode.text = getBarcode
            prodInfo.NutriScore.setImageResource(
            getNutriScore(localProduct.nutriscore)
            )
        }
    }
}

@SuppressLint("ResourceType")
fun clearProdPreview(
    prodInfo: ActivityMainProductInfoBinding
) {
    prodInfo.imageView.setImageBitmap(null)
    prodInfo.tvProductName.text = ""
    prodInfo.tvProductBrand.text = ""
    prodInfo.tvbarCode.text = ""
    prodInfo.NutriScore.setImageResource(R.raw.nutriscore_grey)
}

