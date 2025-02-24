package com.example.spoilalert

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.example.spoilalert.adapters.ProductAdapter
import com.example.spoilalert.databinding.ActivityMainBinding
import com.example.spoilalert.enginebuilder.OpenFoodFactsKtorClient
import com.example.spoilalert.models.ProductModel
import com.example.spoilalert.utils.JsonConverter


class MainActivity : ComponentActivity(), OnTouchListener {
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
//        productQueries.deleteAll()
        dbUpdateManager()

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
        binding.mainMenuStartScanButton.setOnClickListener() {
            requestCamera?.launch(android.Manifest.permission.CAMERA)
        }

        binding.flipperMedia.tvProductName.setOnClickListener{
            updateProductInfoDialog("ProductName",
                binding.flipperMedia.tvProductName.text.toString(),
                binding.flipperMedia.tvbarCode.text.toString())
        }

        binding.flipperMedia.tvProductBrand.setOnClickListener{
            updateProductInfoDialog("ProductBrand",
                binding.flipperMedia.tvProductBrand.text.toString(),
                binding.flipperMedia.tvbarCode.text.toString())
        }

        binding.mainWatchAdd.setOnClickListener() {
            Toast.makeText(applicationContext, "Placeholder for watch add button", Toast.LENGTH_SHORT).show()
        }

        binding.mainMenuSettingsButton.setOnClickListener() {
            Toast.makeText(applicationContext, "Placeholder for settings button", Toast.LENGTH_SHORT).show()
        }

        binding.mainMenuInfoButton.setOnClickListener() {
            Toast.makeText(applicationContext, "Placeholder for info button", Toast.LENGTH_SHORT).show()
        }
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {

        return when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                // when we touch or tap on the screen
                Log.d("TAG", "Action was DOWN")
                true
            }
            MotionEvent.ACTION_MOVE -> {
                // while pressing on the screen,
                // we move our finger
                Log.d("TAG", "Action was MOVE")

                true
            }
            MotionEvent.ACTION_UP -> {
                // Lifting up the finger after
                // pressing on the screen
                Log.d("TAG", "Action was UP")

                true
            }
            MotionEvent.ACTION_CANCEL -> {
                Log.d("TAG", "Action was CANCEL")

                true
            }
            MotionEvent.ACTION_OUTSIDE -> {
                Log.d("TAG", "Movement occurred outside of screen element")

                true
            }
            else -> super.onTouchEvent(event)
        }
    }

    fun iniBc(){
        val allitems = itemQueries.selectjson().executeAsList()
//        Log.d("All Items query", itemQueries.selectAll().executeAsList().toString())
//        Log.d("All Items json query", allitems.toString())
        Log.d("All Items json", JsonConverter(this, allitems).getItemData().toString())
        mRecyclerView = binding.recyclerView
        val adapter = ProductAdapter(this, JsonConverter(this, allitems).getItemData(), binding)
        mRecyclerView!!.adapter = adapter
        mRecyclerView!!.layoutManager = LinearLayoutManager(this)
    }

    @SuppressLint("SetTextI18n")
    private fun updateProductInfoDialog(item: String, value: String, barCode: String) {
        val columnName = item.replace(", ", "")
        var newItem : String = ""
        if (columnName == "ProductName") { newItem = "Product Name"}
        if (columnName == "ProductBrand") { newItem = "Product Brand"}

        val newValue = value.replace(", ", "")
        Log.e("Product Info Update", barCode)
        Log.e("Product Info Update", newItem)
        Log.e("Product Info Update", newValue)

        // create an alert builder
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setTitle(newItem)
        // set the custom layout
        val customLayout: View = layoutInflater.inflate(R.layout.dialog_update_product_info, null)
        builder.setView(customLayout)
        val editText: TextView = customLayout.findViewById<EditText>(R.id.editText)
        editText.text = newValue
        // add a button
        builder.setPositiveButton(
            "Update"
        ) { _, _ -> // do something with response
            val updatedValue = editText.text.toString()
            Log.e("Product Info new Update", updatedValue)
            if (columnName == "ProductName") {
                productQueries.update_product(updatedValue, newValue, barCode)
                binding.flipperMedia.tvProductName.text = "$updatedValue, "
                iniBc()
            }
            if (columnName == "ProductBrand") {
                productQueries.update_brand(updatedValue, newValue, barCode)
                binding.flipperMedia.tvProductBrand.text = updatedValue
                iniBc()
            }
        }
        // create and show the alert dialog
        val dialog: AlertDialog = builder.create()
        dialog.show()
    }


    private fun dbUpdateManager() {
        var versionNr = try {
            dbinfoQueries.get_latest().executeAsOne().toString()
        } catch (_: Exception) {"1"}
        Log.d("Software version:", "Current Software Version: $versionNr")
        if (versionNr == "1") {
            try {
                Database.Schema.migrate(driver, oldVersion = 1, newVersion = 2)
                Log.d("Software version", "Software updated to Version: $versionNr")
            } catch (_: RuntimeException) {}
            versionNr = "2"
        }
        if (versionNr == "2") {
            try {
                Database.Schema.migrate(driver, oldVersion = 2, newVersion = 3)
                Log.d("Software version", "Software updated to Version: $versionNr")
            } catch (_: RuntimeException) {
                Log.e("Software version", "Software failed to update to Version: $versionNr")
            }
            versionNr = "3"
        }
        if (versionNr == "3") {
            try {
                Database.Schema.migrate(driver, oldVersion = 3, newVersion = 4)
                Log.d("Software version", "Software updated to Version: $versionNr")
            } catch (_: RuntimeException) {
                Log.e("Software version", "Software failed to update to Version: $versionNr")
            }
            versionNr = "4"
        }
        if (versionNr == "4") {
            try {
                Database.Schema.migrate(driver, oldVersion = 4, newVersion = 5)
                Log.d("Software version", "Software updated to Version: $versionNr")
            } catch (_: RuntimeException) {
                Log.e("Software version", "Software failed to update to Version: $versionNr")
            }
            versionNr = "5"
            // DO NOT FORGET TO SET INITIAL TABLE GENERATION DB VERSION in DBInfo IF UPDATING. CURRENTLY SET TO VERSION 5
        }
        Log.d("GetAllProducts", productQueries.selectAll().executeAsList().toString())
        Log.d("GetAllDBInfo", dbinfoQueries.selectAll().executeAsList().toString())
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
        @SuppressLint("SetTextI18n")
        fun openPreview(productpreviewlist: String, item: ProductModel, binding: ActivityMainBinding, bitmapimg: Bitmap?) {
            val viewFlipper = binding.myViewFlipper
            var img = bitmapimg
            if (img == null) {
                img = loadImageFromWebOperations(productpreviewlist)
            }
            binding.flipperMedia.imageView.setImageBitmap(img)
            binding.flipperMedia.tvProductName.text = item.product + ", "
            binding.flipperMedia.tvProductBrand.text = item.brand
            binding.flipperMedia.tvbarCode.text = item.barCode
            viewFlipper.displayedChild = viewFlipper.indexOfChild(binding.flipperMedia.productView)
        }
    }

    override fun onTouch(p0: View?, p1: MotionEvent?): Boolean {
        TODO("Not yet implemented")
    }
}
