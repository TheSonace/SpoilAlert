package com.example.spoilalert.models

import com.google.gson.annotations.SerializedName
import java.io.Serializable
import java.util.Date

data class ItemModel(
    @SerializedName("RecordKey")
    val RecordKey: Int,
    @SerializedName("Item")
    val Item: String,
    @SerializedName("spoildate")
    val spoildate: Date
) : Serializable