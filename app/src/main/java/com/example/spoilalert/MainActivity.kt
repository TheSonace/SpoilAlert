package com.example.spoilalert

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.lifecycleScope
import com.example.spoilalert.ui.theme.SpoilAlertTheme
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.example.spoilalert.databinding.ActivityMainBinding
import com.example.spoilalert.enginebuilder.OpenFoodFactsKtorClient
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val ktorclient = OpenFoodFactsKtorClient()
    private val testbarcode = "8718166011199"
    private var requestCamera: ActivityResultLauncher<String>? = null
    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)
        requestCamera = registerForActivityResult(ActivityResultContracts
            .RequestPermission(),){
            if(it){
                val intent = Intent(this, BarcodeScan :: class.java)
                startActivity(intent)
            }else{
                Toast.makeText(this, "Permission Not Granted", Toast.LENGTH_SHORT).show()
            }
        }

        binding.startscanbutton.setOnClickListener() {requestCamera?.launch(android.Manifest.permission.CAMERA)}

        fun doDatabaseThings(driver: SqlDriver){
            val database = Database(driver)
            val itemQueries = database.itemQueries
//            itemQueries.deleteAll()
//            Log.d("TAG", itemQueries.selectAll().executeAsList().toString())
//            itemQueries.insert(
//                "0001",
//                "2000-01-01",
//                "1900-01-01",
//                "2025-01-01")
            Log.d("TAG", itemQueries.selectAll().executeAsList().toString())
        }
        doDatabaseThings(AndroidSqliteDriver(Database.Schema, this, "launch.db"))

        lifecycleScope.launch {
            val json = ktorclient.fetchProductByCode(testbarcode)
            Log.d("Barcode", testbarcode)
            Log.d("Brand", json.product?.brands.toString())
            Log.d("Product", json.product?.productName.toString())
            Log.d("product URL", ktorclient.createProductUrl(testbarcode).toString())
            val image = json.product?.imageFrontUrl.toString()
            Log.d("image", image)
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

