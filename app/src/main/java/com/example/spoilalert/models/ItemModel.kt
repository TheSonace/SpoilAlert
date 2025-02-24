package com.example.spoilalert.models

import com.google.gson.annotations.SerializedName
import java.io.Serializable
import java.util.Date

data class ItemModel(
    @SerializedName("RecordKey")
    val RecordKey: Int,
    @SerializedName("barCode")
    var barCode: String,
    @SerializedName("Item")
    val Item: String,
    @SerializedName("brand")
    var brand: String,
    @SerializedName("product")
    var product: String,
    @SerializedName("spoildate")
    val spoildate: Date,
    @SerializedName("location")
    var location: String,
) : Serializable