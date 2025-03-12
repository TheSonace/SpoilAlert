package com.example.spoilalert.utils

import com.example.spoilalert.R

fun getNutriScore(nutriscore: String): Int {
    var image: Int = R.raw.nutriscore_grey
    when (nutriscore) {
        "a" -> image = R.raw.nutriscore_a
        "b" -> image = R.raw.nutriscore_b
        "c" -> image = R.raw.nutriscore_c
        "d" -> image = R.raw.nutriscore_d
        "e" -> image = R.raw.nutriscore_e
    }
    return image
}