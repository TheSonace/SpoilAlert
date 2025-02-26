package com.example.spoilalert

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.text.format.Formatter
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.example.spoilalert.adapters.ProductAdapter
import com.example.spoilalert.databinding.ActivityMainBinding
import com.example.spoilalert.enginebuilder.OpenFoodFactsKtorClient
import com.example.spoilalert.models.ProductModel
import com.example.spoilalert.utils.JsonConverter
import com.example.spoilalert.utils.UpdateAndSaveImageTask
import java.io.File
import java.io.FileOutputStream


class MainActivity : ComponentActivity(){ //, OnTouchListener, GestureDetector.OnGestureListener {
    val ktorclient = OpenFoodFactsKtorClient()
    private var scanBarcode: ActivityResultLauncher<String>? = null
    private var cameraLauncher: ActivityResultLauncher<String>? = null
    private lateinit var binding: ActivityMainBinding
    val driver = AndroidSqliteDriver(Database.Schema, this, "launch.db")
    private val database = Database(driver)
    private val itemQueries = database.itemQueries
    private val dbinfoQueries = database.dBInfoQueries
    private var mRecyclerView: RecyclerView? = null
    private val productQueries = database.productQueries
    private lateinit var cameraProvider: ProcessCameraProvider

    @SuppressLint("QueryPermissionsNeeded")
    override fun onCreate(savedInstanceState: Bundle?) {
//        itemQueries.deleteAll()
//        productQueries.deleteAll()
        dbUpdateManager()

        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)

        binding.mainAddSlidingDrawer.animateOpen()
        binding.mainMenuSlidingDrawer.animateOpen()
//        gestureDetector = GestureDetector(this, this)

        scanBarcode = registerForActivityResult(
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

        cameraLauncher = registerForActivityResult(
            ActivityResultContracts
                .RequestPermission(),
        ) {
            if (it) {
                val viewFlipper = binding.myViewFlipper
                viewFlipper.displayedChild = viewFlipper.indexOfChild(binding.flipperMediaCamera.cameraImagePreviewLayout)
                startCamera()
            } else {
                Toast.makeText(this, "Permission Not Granted", Toast.LENGTH_SHORT).show()
            }
        }

        binding.mainMenuStartScanButton.setOnClickListener {
            scanBarcode?.launch(android.Manifest.permission.CAMERA)
        }

        binding.flipperMedia.editImageButton.setOnClickListener{
            cameraLauncher?.launch(android.Manifest.permission.CAMERA)
        }

        binding.flipperMediaCamera.AddImageSaveButton.setOnClickListener{
            val bitmap = binding.flipperMediaCamera.viewFinder.getBitmap()
            val fileName = productQueries.getRecordKey(binding.flipperMedia.tvbarCode.text.toString()).executeAsList()[0]
            val file = UpdateAndSaveImageTask(this, fileName, database, bitmap).saveImage()
            if (file != false) {
                binding.flipperMedia.imageView.setImageBitmap(BitmapFactory.decodeFile(file.toString()))
                binding.myViewFlipper.displayedChild = binding.myViewFlipper.indexOfChild(binding.flipperMedia.productView)
                mStopCamera()
                Toast.makeText(this, "Image Overwritten", Toast.LENGTH_SHORT).show()
            } else {Log.i("SpoilAlert", "Failed to save image.")}
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

        binding.mainWatchAdd.setOnClickListener {
            Toast.makeText(applicationContext, "Placeholder for watch add button", Toast.LENGTH_SHORT).show()
        }

        binding.mainMenuSettingsButton.setOnClickListener {
            Toast.makeText(applicationContext, "Placeholder for settings button", Toast.LENGTH_SHORT).show()
        }

        binding.mainMenuInfoButton.setOnClickListener {
            Toast.makeText(applicationContext, "Placeholder for info button", Toast.LENGTH_SHORT).show()
        }
    }

//    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
//        event.let { gestureDetector.onTouchEvent(it) }
//        return super.dispatchTouchEvent(event)
//    }

    private fun iniBc(){
        val allitems = itemQueries.selectjson().executeAsList()
//        Log.d("All Items query", itemQueries.selectAll().executeAsList().toString())
//        Log.d("All Items json query", allitems.toString())
        Log.d("All Items json", JsonConverter(this, allitems).getItemData().toString())
        mRecyclerView = binding.recyclerView
        val adapter = ProductAdapter(this, JsonConverter(this, allitems).getItemData(), binding)
        mRecyclerView!!.adapter = adapter
        mRecyclerView!!.layoutManager = LinearLayoutManager(this)
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            // Used to bind the lifecycle of cameras to the lifecycle owner
            cameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(binding.flipperMediaCamera.viewFinder.surfaceProvider)
                }

            // Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview)

            } catch(exc: Exception) {
                Log.e("Camera", "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun mStopCamera() {
        cameraProvider.unbindAll()
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
                versionNr = "4"
                Log.d("Software version", "Software updated to Version: $versionNr")
            } catch (_: RuntimeException) {
                Log.e("Software version", "Software failed to update to Version: $versionNr")
            }
        }
        if (versionNr == "4") {
            try {
                Database.Schema.migrate(driver, oldVersion = 4, newVersion = 5)
                versionNr = "5"
                Log.d("Software version", "Software updated to Version: $versionNr")
            } catch (e: RuntimeException) {
                if (e.toString() == "android.database.sqlite.SQLiteException: duplicate column name: barCode_original (code 1 SQLITE_ERROR): , while compiling: ALTER TABLE product_data ADD COLUMN barCode_original TEXT NOT NULL DEFAULT 0") {
                    dbinfoQueries.update(5)
                    Log.d("Software version", "Software updated to Version: 5")}
                else {
                    Log.e("Software version", "Software failed to update to Version: $versionNr")
                }
            }
            // DO NOT FORGET TO SET INITIAL TABLE GENERATION DB VERSION in DBInfo IF UPDATING. CURRENTLY SET TO VERSION 5
            // DO NOT FORGET TO UPDATE SOFTWARE VERSION IN MIGRATION FILE
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
            returnToMain()}
        else if (viewFlipper.displayedChild == viewFlipper.indexOfChild(binding.flipperMediaCamera.cameraImagePreviewLayout)){
            mStopCamera()
            viewFlipper.displayedChild = viewFlipper.indexOfChild(binding.flipperMedia.productView)}
        else if (binding.mainAddSlidingDrawer.isOpened) {binding.mainAddSlidingDrawer.animateClose()}
        else if (binding.mainMenuSlidingDrawer.isOpened) {binding.mainMenuSlidingDrawer.animateClose()}
        else {super.onBackPressed()}
        //super.onBackPressed();
    }

    private fun returnToMain() {
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





//    override fun onDown(p0: MotionEvent): Boolean = false
//    override fun onShowPress(p0: MotionEvent) {}
//    override fun onSingleTapUp(p0: MotionEvent): Boolean = false
//    override fun onScroll(p0: MotionEvent?, p1: MotionEvent, p2: Float, p3: Float): Boolean = false
//    override fun onLongPress(p0: MotionEvent) {}
//    override fun onFling(p0: MotionEvent?, p1: MotionEvent, p2: Float, p3: Float): Boolean {
//        val addsMenu = binding.myViewFlipper.findViewById<SlidingDrawer>(R.id.mainAddSlidingDrawer)
//        val mainMenu = binding.myViewFlipper.findViewById<SlidingDrawer>(R.id.mainMenuSlidingDrawer)
//        // Handle the fling gesture
//        if (p2 > 500) {
//            // Right fling
//            if (mainMenu.isOpened) {mainMenu.animateClose()}
//            if (!mainMenu.isOpened) {addsMenu.animateOpen()}
//        } else if (p3 < -500) {
//            // Left fling
//            if (addsMenu.isOpened) {addsMenu.animateClose()}
//            if (!addsMenu.isOpened) {mainMenu.animateOpen()}
//        }
//        return true
//    }
//    @SuppressLint("ClickableViewAccessibility")
//    override fun onTouch(p0: View?, p1: MotionEvent?): Boolean = false

}
