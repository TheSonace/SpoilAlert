package com.example.spoilalert

import UIContent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.lifecycleScope
import com.example.spoilalert.ui.theme.SpoilAlertTheme
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.example.spoilalert.model.Product
import com.example.spoilalert.model.ProductResponse
import com.google.gson.Gson
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val ktorclient = OpenFoodFactsKtorClient()
    private val testbarcode = "8718166011199"
    override fun onCreate(savedInstanceState: Bundle?) {
        fun doDatabaseThings(driver: SqlDriver){
            val database = Database(driver)
            val itemQueries = database.itemQueries
            itemQueries.deleteAll()
            Log.d("TAG", itemQueries.selectAll().executeAsList().toString())
            itemQueries.insert(
                "0001",
                "2000-01-01",
                "1900-01-01",
                "2025-01-01")
            Log.d("TAG", itemQueries.selectAll().executeAsList().toString())
        }
        doDatabaseThings(AndroidSqliteDriver(Database.Schema, this, "launch.db"))
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            val json = ktorclient.fetchProductByCode(testbarcode)
            Log.d("product URL", ktorclient.createProductUrl(testbarcode).toString())
            val image = json.product?.imageFrontUrl.toString()
            Log.d("json", json.product?.keywords.toString())
            Log.d("image", image)
        }
//        val data = Gson().fromJson(test, UIContent::class.java)


//        val previewClient = object : OpenFoodFactsClient {
//            override suspend fun fetchProductByCode(code: String): ProductResponse {
//                return ProductResponse(
//                    product = Product(code = "8718166011199")
//                )
//            }
//        }
//        Log.d("TAG", previewClient.toString())

        enableEdgeToEdge()
        setContent {
            SpoilAlertTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        name = "Android", //data.sample.sampleString,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}


@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    SpoilAlertTheme {
        Greeting("Android")
    }
}

