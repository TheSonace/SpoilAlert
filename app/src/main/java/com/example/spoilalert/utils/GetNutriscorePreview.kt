package com.example.spoilalert.utils

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import com.example.ProductQueries
import com.example.spoilalert.R
import com.example.spoilalert.databinding.ActivityMainProductInfoBinding

fun getNutriScore(nutriscore: String): Int {
    var image: Int = R.raw.nutriscore_grey
    when (nutriscore) {
        "a" -> image = R.raw.nutriscore_a
        "b" -> image = R.raw.nutriscore_b
        "c" -> image = R.raw.nutriscore_c
        "d" -> image = R.raw.nutriscore_d
        "e" -> image = R.raw.nutriscore_e
        "null" -> image = R.raw.nutriscore_grey
    }
    return image
}

fun openNutriscoreDialog(
    context: Context,
    layoutInflater: LayoutInflater,
    prodInfo: ActivityMainProductInfoBinding,
    productQueries: ProductQueries,
) {

    val customLayout: View = layoutInflater.inflate(R.layout.dialog_nutriscore_select, null)
    val builder: AlertDialog.Builder = AlertDialog.Builder(context)
    builder.setView(customLayout)

    val nutriA: ImageView = customLayout.findViewById(R.id.imageViewNutri_A)
    val nutriB: ImageView = customLayout.findViewById(R.id.imageViewNutri_B)
    val nutriC: ImageView = customLayout.findViewById(R.id.imageViewNutri_C)
    val nutriD: ImageView = customLayout.findViewById(R.id.imageViewNutri_D)
    val nutriE: ImageView = customLayout.findViewById(R.id.imageViewNutri_E)
    val nutriG: ImageView = customLayout.findViewById(R.id.imageViewNutri_G)
    val getBarcode = prodInfo.tvbarCode.text.toString()

    val dialog: AlertDialog = builder.create()
    dialog.show()

    nutriA.setOnClickListener {
        prodInfo.NutriScore.setImageResource(getNutriScore("a"))
        productQueries.update_nutriscore("a", getBarcode)
        dialog.dismiss()
    }

    nutriB.setOnClickListener {
        prodInfo.NutriScore.setImageResource(getNutriScore("b"))
        productQueries.update_nutriscore("b", getBarcode)
        dialog.dismiss()
    }

    nutriC.setOnClickListener {
        prodInfo.NutriScore.setImageResource(getNutriScore("c"))
        productQueries.update_nutriscore("c", getBarcode)
        dialog.dismiss()
    }

    nutriD.setOnClickListener {
        prodInfo.NutriScore.setImageResource(getNutriScore("d"))
        productQueries.update_nutriscore("d", getBarcode)
        dialog.dismiss()
    }

    nutriE.setOnClickListener {
        prodInfo.NutriScore.setImageResource(getNutriScore("e"))
        productQueries.update_nutriscore("e", getBarcode)
        dialog.dismiss()
    }

    nutriG.setOnClickListener {
        prodInfo.NutriScore.setImageResource(getNutriScore("null"))
        productQueries.update_nutriscore("null", getBarcode)
        dialog.dismiss()
    }
}