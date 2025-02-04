package com.example.spoilalert.utils

import android.content.Context
import android.util.Log
import com.example.Selectjson
import com.example.spoilalert.models.ItemModel
import com.example.spoilalert.models.ProductModel
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import org.json.JSONArray


open class JsonConverter(private var context: Context, data: List<Selectjson>) {
    private var productList: MutableList<ProductModel>? = null
    private var sqlQuery: List<Selectjson>? = data

    open fun getItemData(): MutableList<ProductModel>? {
        if (productList == null)
            productList = ArrayList()

        try {
            val rawjson = Gson().toJson(sqlQuery)
            Log.d("test2", rawjson)
            // convert raw json to specified kotlin class.
            // Direct class to class conversion is more difficult than flat Json conversion
            val data = GsonBuilder().setDateFormat("yyyyMMdd").create().fromJson(
                rawjson, Array<ItemModel>::class.java
            ).toMutableList()

            Log.d("test3", data.toString())
            // map grouped class
            val groupedClass = data.groupBy { it.barCode }
                .map {
                    it.value.minByOrNull { Data2 -> Data2.spoildate }?.let { it1 ->
                        ProductModel(
                            it.key, it.value.toMutableList(), it.value.count(),
                            it1.spoildate
                        )
                    }
                }
            Log.d("test4", groupedClass.toString())

            val groupedClass2 = data.groupBy { it.barCode }



            Log.d("test4.1", groupedClass2.toString())
            // convert back to raw json Array
            val jsonArray = JSONArray(
                GsonBuilder()
                    .serializeNulls()
                    .create()
                    .toJson(groupedClass)
            )
            Log.d("test5", jsonArray.toString())
            val k = jsonArray.length()

            for (i in 0 until k) {
                val tempJsonObject = jsonArray.getJSONObject(i).toString()
                val gson = Gson()
                val tempitem = gson.fromJson<ProductModel>(tempJsonObject, ProductModel::class.java)
                productList?.add(tempitem)
            }
            return productList
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
}