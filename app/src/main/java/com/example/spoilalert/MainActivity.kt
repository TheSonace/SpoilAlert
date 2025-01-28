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
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.example.DBInfoQueries
import com.example.spoilalert.adapters.NewspaperAdapter
import com.example.spoilalert.databinding.ActivityMainBinding
import com.example.spoilalert.enginebuilder.OpenFoodFactsKtorClient
import com.example.spoilalert.utils.JsonHelper
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken


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

        val recyclerView = binding.recyclerView
        val adapter = NewspaperAdapter(this, JsonHelper(this).getNewspaperData())
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        requestCamera = registerForActivityResult(
            ActivityResultContracts
                .RequestPermission(),
        ){
            if(it){
                val intent = Intent(this, BarcodeScan :: class.java)
                startActivity(intent)
            }else{
                Toast.makeText(this, "Permission Not Granted", Toast.LENGTH_SHORT).show()
            }
        }

        binding.startscanbutton.setOnClickListener() {requestCamera?.launch(android.Manifest.permission.CAMERA)}


        // This data class needs to be moved into NewspaperModel
        data class Data2(
            @SerializedName("RecordKey")
        val RecordKey: Long,
            @SerializedName("barCode")
        val barCode: String,
        @SerializedName("scandatetime")
        val scandatetime: String,
            @SerializedName("spoildate")
        val spoildate: String)


        // this data class needs to be moved into HeadlinesModel
        data class Itemgrouptemp(
            @SerializedName("name")
            val name: String,
            @SerializedName("headlines")
            val data: List<Data2>,
            @SerializedName("is_expanded")
            var isExpanded: Boolean? = false)


        val allitems = itemQueries.selectAll().executeAsList()
        // convert sql to raw json
        val rawjson = Gson().toJson(allitems)
        // convert raw json to specified kotlin class.
        // Direct class to class conversion is more difficult than flat Json conversion
        val data = GsonBuilder().create().fromJson(
            rawjson , Array<Data2>::class.java).toList()
        // convert back to raw json with mapped groupby values, column names
        val testjson = GsonBuilder()
            .serializeNulls()
            .create()
            .toJson(data.groupBy { it.barCode }
                .map { Itemgrouptemp(it.key, it.value) })
        // This is the same output as jsonhelper. can hook into newspaperadapter with this
        Log.d("this is the one????", testjson.toString())
    }

    private fun getJSONFromAssets(fileName: String): String? {
        val json: String
        try {
            val inputStream = this.assets.open(fileName)
            val size = inputStream.available()
            val buffer = ByteArray(size)
            inputStream.read(buffer)
            inputStream.close()
            json = String(buffer)
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
        return json
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
