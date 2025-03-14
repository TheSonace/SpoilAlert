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
import com.example.spoilalert.enginebuilder.OpenFoodFactsKtorClient
import com.example.spoilalert.utils.DownloadAndSaveImageTask
import com.example.spoilalert.utils.getNutriScore
import com.example.spoilalert.utils.loadImageFromWebOperations
import java.io.File


fun populateProdPreview(
    context: Context,
    getBarcode: String,
    productQueries: ProductQueries,
    prodInfo: ActivityMainProductInfoBinding
) {
    val localProduct =
        productQueries.getlocal(getBarcode).executeAsList()[0]
    if (localProduct.status == "1") {
        Log.e("status prodinfoadapter", localProduct.status)
        val record = localProduct.RecordKey
        val file = File(
            File(context.filesDir, "Products"),
            "$record.jpg")
        if (file.exists()) {
            Log.e("prodinfoadapterfile", "file exists?")
            prodInfo.imageView.setImageBitmap(BitmapFactory.decodeFile(file.toString()))
        } else {
            loadImageFromWebOperations(localProduct.image_original, prodInfo)
            Log.e("prodinfoadapterfile", "img loaded from web")
        }

        prodInfo.tvProductName.text = localProduct.product
        prodInfo.tvProductBrand.text = localProduct.brand
        prodInfo.tvbarCode.text = getBarcode
        prodInfo.NutriScore.setImageResource(
        getNutriScore(localProduct.nutriscore)
        )

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

suspend fun forceRefreshProdPreview(context: Context, getBarcode: String, productQueries: ProductQueries) {
    productQueries.forceRefresh(getBarcode)
    Log.e("forced refresh", "query executed")
    val localProduct =
        productQueries.getlocal(getBarcode).executeAsList()[0]
    val record = localProduct.RecordKey
    var imgLoc = productQueries.getimg_original(record).executeAsList()[0]
    var imgmodified = productQueries.getimg_modified(record).executeAsList()[0]
    val file = File(
        File(context.filesDir, "Products"),
        "$record.jpg"
    )
    if (file.exists() && imgLoc != "null") {
        file.delete()
        if (!imgLoc.startsWith("https" )) {
            val json = OpenFoodFactsKtorClient().fetchProductByCode(getBarcode)
            imgLoc = json.product?.imageFrontUrl.toString()
            productQueries.reset_image(imgLoc, imgLoc, record)
        }
    Log.e("forced refresh", imgLoc)

    }

    if (!file.exists() && imgLoc != "null") {
        Log.e("forced refresh", "file deleted")
        DownloadAndSaveImageTask(
            context,
            record
        ).execute(imgLoc)
    }
}

