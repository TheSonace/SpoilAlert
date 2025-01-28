package com.example.spoilalert.models

import com.google.gson.annotations.SerializedName
import java.io.Serializable
import java.util.Date

class ItemModel(
    @SerializedName("barCode")
    val barCode: String,
    @SerializedName("spoildate")
    val spoildate: Date
) : Serializable