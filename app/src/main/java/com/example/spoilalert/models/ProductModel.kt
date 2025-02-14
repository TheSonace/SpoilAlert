package com.example.spoilalert.models

import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName
import java.io.Serializable
import java.util.Date

data class ProductModel(
    @SerializedName("name")
    var name: String,
    @SerializedName("barCode")
    var barCode: String,
    @SerializedName("item_data")
    var item_data: MutableList<ItemModel>,
    @SerializedName("item_count")
    var item_count: Int,
    @SerializedName("min_spoildate")
    var min_spoildate: Date,
    @SerializedName("is_expanded")
    var isExpanded: Boolean? = false) : Serializable {
    override
    fun toString(): String {
        return GsonBuilder().serializeNulls().create().toJson(this)
    }
}