package com.example.spoilalert

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.example.DBInfoQueries
import com.example.spoilalert.adapters.ProductAdapter
import com.example.spoilalert.databinding.ActivityMainBinding
import com.example.spoilalert.enginebuilder.OpenFoodFactsKtorClient
import com.example.spoilalert.models.ProductModel
import com.example.spoilalert.utils.JsonConverter

class MainActivity : ComponentActivity() {
    val ktorclient = OpenFoodFactsKtorClient()
    private var requestCamera: ActivityResultLauncher<String>? = null
    private lateinit var binding: ActivityMainBinding
    val driver = AndroidSqliteDriver(Database.Schema, this, "launch.db")
    val database = Database(driver)
    val itemQueries = database.itemQueries
    private val dbinfoQueries = database.dBInfoQueries
    var mRecyclerView: RecyclerView? = null
    private val productQueries = database.productQueries

    override fun onCreate(savedInstanceState: Bundle?) {
//        itemQueries.deleteAll()
        dbUpdateManager(dbinfoQueries, driver)

        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)

        requestCamera = registerForActivityResult(
            ActivityResultContracts
                .RequestPermission(),
        ) {
            if (it) {
                val intent = Intent(this, BarcodeScan::class.java)
                startActivity(intent)
            } else {
                Toast.makeText(this, "Permission Not Granted", Toast.LENGTH_SHORT).show()
            }
        }
        binding.startscanbutton.setOnClickListener() { requestCamera?.launch(android.Manifest.permission.CAMERA) }
    }

    private fun iniBc(){
        val allitems = itemQueries.selectjson().executeAsList()
        mRecyclerView = binding.recyclerView
        val adapter = ProductAdapter(this, JsonConverter(this, allitems).getItemData(), binding)
        mRecyclerView!!.adapter = adapter
        mRecyclerView!!.layoutManager = LinearLayoutManager(this)
    }


    private fun dbUpdateManager(dbinfoQueries: DBInfoQueries, driver: AndroidSqliteDriver) {
        var versionNr = '1'.toString()
        try {
            versionNr = dbinfoQueries.get_latest().executeAsOne().toString()
        } catch (_: Exception) {
        }
        Log.d("Software version:", "Current Software Version: " + versionNr)
        if (versionNr == "1") {
            try {
                Database.Schema.migrate(driver, oldVersion = 1, newVersion = 2)
            } catch (_: RuntimeException) {
            }
            versionNr = "2"
            Log.d("Software version", "Software updated to Version: " + versionNr)
        }
        if (versionNr == "2") {
            Database.Schema.migrate(driver, oldVersion = 2, newVersion = 3)
            dbinfoQueries.insert(3)
            versionNr = "3"
            Log.d("Software version", "Software updated to Version: " + versionNr)
        }
//    Log.d("test", dbinfoQueries.get_all().executeAsList().toString())
    }

    override fun onResume() {
        super.onResume()
        iniBc()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        val viewFlipper = binding.myViewFlipper
        if (viewFlipper.displayedChild == viewFlipper.indexOfChild(binding.flipperMedia.productView)){
            returnToMain()
        }
        else {super.onBackPressed()}
        //super.onBackPressed();
    }

    fun returnToMain() {
        val viewFlipper = binding.myViewFlipper
        viewFlipper.displayedChild = viewFlipper.indexOfChild(binding.main)
    }

    companion object {
        fun openPreview(productpreviewlist: String, binding: ActivityMainBinding) {
            val viewFlipper = binding.myViewFlipper
            val img = loadImageFromWebOperations(productpreviewlist)
            binding.flipperMedia.imageView.setImageBitmap(img)
            viewFlipper.displayedChild = viewFlipper.indexOfChild(binding.flipperMedia.productView)
        }
    }
}
