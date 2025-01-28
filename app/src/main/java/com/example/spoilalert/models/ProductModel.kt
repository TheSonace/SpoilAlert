package com.example.spoilalert.models

import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName
import java.io.Serializable
import java.util.Date

data class ProductModel(
    @SerializedName("name")
    val name: String,
    @SerializedName("item_data")
    val item_data: List<ItemModel>,
    @SerializedName("item_count")
    val item_count: Int,
    @SerializedName("min_spoildate")
    val min_spoildate: Date,
    @SerializedName("is_expanded")
    var isExpanded: Boolean? = false) : Serializable {
    override
    fun toString(): String {
        return GsonBuilder().serializeNulls().create().toJson(this)
    }
}