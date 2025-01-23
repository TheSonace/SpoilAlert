package com.example.spoilalert

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.example.DBInfoQueries
import com.example.spoilalert.databinding.ActivityMainBinding
import com.example.spoilalert.enginebuilder.OpenFoodFactsKtorClient

class MainActivity : ComponentActivity() {
    val ktorclient = OpenFoodFactsKtorClient()
    private val testbarcode = "8718166011199"
    private var requestCamera: ActivityResultLauncher<String>? = null
    private lateinit var binding: ActivityMainBinding
    val driver = AndroidSqliteDriver(Database.Schema, this, "launch.db")
    val database = Database(driver)
    val itemQueries = database.itemQueries
    private val productQueries = database.productQueries
    private val dbinfoQueries = database.dBInfoQueries

    override fun onCreate(savedInstanceState: Bundle?) {
//        dbinfoQueries.delete_all()
        dbUpdateManager(dbinfoQueries, driver)

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

//        fun doDatabaseThings(){
//            itemQueries.deleteAll()
//            Log.d("TAG", itemQueries.selectAll().executeAsList().toString())
//            itemQueries.insert(
//                "0001",
//                "2000-01-01",
//                "1900-01-01",
//                "2025-01-01")
//            Log.d("TAG1", itemQueries.selectAll().executeAsList().toString())
//            Log.d("TAG2", productQueries.selectAll().executeAsList()[0].product_url)
//        }
//        doDatabaseThings()


    }
}

fun dbUpdateManager(dbinfoQueries: DBInfoQueries, driver: AndroidSqliteDriver){
    var versionNr = '1'.toString()
    try{
        versionNr = dbinfoQueries.get_latest().executeAsOne().toString()
    }
    catch  (_: Exception) { }
    Log.d("Software version:", "Current Software Version: " + versionNr)
    if(versionNr == "1")
    {
        try {
            Database.Schema.migrate(driver, oldVersion = 1, newVersion = 2)
        } catch (_: RuntimeException) { }
        versionNr = "2"
        Log.d("Software version", "Software updated to Version: " + versionNr)
    }
    if(versionNr == "2")
    {
        Database.Schema.migrate(driver, oldVersion = 2, newVersion = 3)
        dbinfoQueries.insert(3)
        versionNr = "3"
        Log.d("Software version","Software updated to Version: " + versionNr)
    }
//    Log.d("test", dbinfoQueries.get_all().executeAsList().toString())
}
