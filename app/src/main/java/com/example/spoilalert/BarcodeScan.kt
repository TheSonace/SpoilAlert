package com.example.spoilalert

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.SurfaceHolder
import android.view.View
import android.widget.DatePicker
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import android.widget.ViewFlipper
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.example.Product_data
import com.example.spoilalert.databinding.ActivityBarcodeScanBinding
import com.example.spoilalert.enginebuilder.OpenFoodFactsKtorClient
import com.example.spoilalert.utils.DownloadAndSaveImageTask
import com.example.spoilalert.utils.UpdateAndSaveImageTask
import com.google.android.gms.vision.CameraSource
import com.google.android.gms.vision.Detector
import com.google.android.gms.vision.barcode.Barcode
import com.google.android.gms.vision.barcode.BarcodeDetector
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale


var latestbarcodescan = ""
var activeScanBoolean = 0
var itemsRequired = 1
var itemtobeAdded = 1

class BarcodeScan : AppCompatActivity() {
    private val ktorclient = OpenFoodFactsKtorClient()
    val database = Database(AndroidSqliteDriver(Database.Schema, this, "launch.db"))
    private val itemQueries = database.itemQueries
    val productQueries = database.productQueries
    private val dbinfoQueries = database.dBInfoQueries

    private var cameraLauncher: ActivityResultLauncher<String>? = null
    private lateinit var cameraProvider: ProcessCameraProvider

    val myFormat = "yyyyMMdd"
    val sdf = SimpleDateFormat(myFormat, Locale.US)

    private lateinit var binding: ActivityBarcodeScanBinding
    private lateinit var barcodeDetector: BarcodeDetector
    private lateinit var cameraSource: CameraSource

    var scandatetime = ""
    var spoildate = ""
    var cal = Calendar.getInstance()

    var scans: Int = 0


    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d("TAG", productQueries.selectAll().executeAsList().toString())
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        binding = ActivityBarcodeScanBinding.inflate(layoutInflater)
        setContentView(binding.root)

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

        binding.flipperMedia.btnAddItem.setOnClickListener {
            itemsRequired = Integer.parseInt(binding.flipperMedia.AddItems.text.toString())
            var x = 0
            scans = dbinfoQueries.get_tokens().executeAsOne().toInt()
            if (scans >= itemsRequired) {x = 1}
            when (x) {
                0 -> Toast.makeText(
                    applicationContext,
                    "Not enough tokens remaining, please watch an add to receive more",
                    Toast.LENGTH_SHORT).show()
                1 -> openDatePicker()
            }
        }

        binding.flipperMedia.btnRemoveItem.setOnClickListener {
            removeItemfromDB()
        }

        binding.flipperMedia.addCount.setOnClickListener {
            val itemCount = Integer.parseInt(binding.flipperMedia.AddItems.text.toString())
            binding.flipperMedia.AddItems.setText((itemCount + 1).toString())
        }

        binding.flipperMedia.removeCount.setOnClickListener {
            val itemCount = Integer.parseInt(binding.flipperMedia.AddItems.text.toString())
            if (itemCount > 1) {binding.flipperMedia.AddItems.setText((itemCount - 1).toString())}
        }

        binding.button.setOnClickListener {
            Toast.makeText(applicationContext, "BarCode can't be detected in DataBase. " +
                    "Will need to manually add item.", Toast.LENGTH_SHORT).show()
        }

        binding.flipperMedia.prodInfo.editImageButton.setOnClickListener{
            cameraLauncher?.launch(android.Manifest.permission.CAMERA)
        }

        binding.flipperMediaCamera.AddImageSaveButton.setOnClickListener{
            val bitmap = binding.flipperMediaCamera.viewFinder.getBitmap()
            val fileName = productQueries.getRecordKey(binding.flipperMedia.prodInfo.tvbarCode.text.toString()).executeAsList()[0]
            val file = UpdateAndSaveImageTask(this, fileName, database, bitmap).saveImage()
            if (file != false) {
                binding.flipperMedia.prodInfo.imageView.setImageBitmap(BitmapFactory.decodeFile(file.toString()))
                binding.myViewFlipper.displayedChild = binding.myViewFlipper.indexOfChild(binding.flipperMedia.main2)
                Toast.makeText(this, getString(R.string.updateImageSucceed), Toast.LENGTH_SHORT).show()
            } else {Log.i("SpoilAlert", "Failed to save image.")}
        }

        binding.flipperMedia.prodInfo.tvProductName.setOnClickListener{
            updateProductInfoDialog("ProductName",
                binding.flipperMedia.prodInfo.tvProductName.text.toString(),
                binding.flipperMedia.prodInfo.tvbarCode.text.toString(),
                "update")
        }

        binding.flipperMedia.prodInfo.tvProductBrand.setOnClickListener{
            updateProductInfoDialog(
                "ProductBrand",
                binding.flipperMedia.prodInfo.tvProductBrand.text.toString(),
                binding.flipperMedia.prodInfo.tvbarCode.text.toString(),
                "update"
            )
        }
    }

    private fun iniBc(){
        activeScanBoolean = 0
        barcodeDetector = BarcodeDetector.Builder(this)
            .setBarcodeFormats(Barcode.ALL_FORMATS)
            .build()
        cameraSource = CameraSource.Builder(this, barcodeDetector)
            .setRequestedPreviewSize(1920,1080)
            .setAutoFocusEnabled(true)
            //.setFacing(CameraSource.CAMERA_FACING_BACK)
            .build()
        binding.SurfaceView.holder.addCallback(object : SurfaceHolder.Callback{
            @SuppressLint("MissingPermission")
            override fun surfaceCreated(p0: SurfaceHolder) {
                try {
                    cameraSource.start(binding.SurfaceView.holder)
                }catch (e: IOException) {
                    e.printStackTrace()
                }
            }

            override fun surfaceChanged(
                holder: SurfaceHolder,
                format: Int,
                width: Int,
                height: Int
            ) {}

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                cameraSource.stop()
            }

        })
        barcodeDetector.setProcessor(object : Detector.Processor<Barcode>{
            override fun release() {
                Toast.makeText(applicationContext, getString(R.string.stopScanReturnHome), Toast.LENGTH_SHORT).show()
            }

            @SuppressLint("SetTextI18n")
            override fun receiveDetections(detections: Detector.Detections<Barcode>) {
                if (activeScanBoolean == 0) {
                    val barcodes = detections.detectedItems
                    if(barcodes.size()!=0){
                        val getBarcode = barcodes.valueAt(0).displayValue
                        if(latestbarcodescan != getBarcode){
                            latestbarcodescan = getBarcode
//                            Log.e("barcode nr", latestbarcodescan)
                            val viewFlipper = binding.myViewFlipper
                            Thread.sleep(200)
                            try {productQueries.localcheck(getBarcode).executeAsList()[0]}
                            catch (_: IndexOutOfBoundsException) {
//                                Log.e("Adding Data!!", latestbarcodescan)
                                lifecycleScope.launch {
                                    downloadProduct(getBarcode)}}
                            try {
                                val localProduct =
                                    productQueries.getlocal(latestbarcodescan).executeAsList()[0]
                                val record = localProduct.RecordKey
                                val imgLoc = productQueries.getimg(record).executeAsList()[0]
                                var myBitmap: Bitmap? = null
                                if (imgLoc != "null") {
                                    val file = File(File(this@BarcodeScan.filesDir, "Products"),
                                        "$record.jpg")
                                    if (!file.exists()) {
                                        DownloadAndSaveImageTask(this@BarcodeScan, record, database).execute(imgLoc)
                                        myBitmap = loadImageFromWebOperations(localProduct.image)
                                        if (file.exists()) {
                                            productQueries.update_image(file.toString(), record)
                                            productQueries.set_nullcheck(localProduct.barCode)
                                        }
                                    }
                                    if (file.exists()) {
                                        myBitmap = BitmapFactory.decodeFile(file.toString())
                                    }
                                }
                                binding.flipperMedia.prodInfo.imageView.setImageBitmap(myBitmap)
                                binding.flipperMedia.prodInfo.tvProductName.text = localProduct.product + ", "
                                binding.flipperMedia.prodInfo.tvProductBrand.text = localProduct.brand
                                binding.flipperMedia.prodInfo.tvbarCode.text = latestbarcodescan
                                runOnUiThread(Runnable { switchToPreview(viewFlipper) })
                            } catch(_: NullPointerException) {}
                        }
                        else {
                            latestbarcodescan = ""
                            Thread.sleep(1000)
                        }
                    }
                }
            }
        })
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

    private fun checkForNull(localProduct: Product_data, barCode: String) {
        if (localProduct.product == "null") {
            updateProductInfoDialog("ProductName", localProduct.product, barCode, "add")
        }
        else if (localProduct.brand == "null") {
            updateProductInfoDialog("ProductBrand", localProduct.brand, barCode, "add")
        }
        else if (localProduct.image == "null") {
            Toast.makeText(
                applicationContext,
                getString(R.string.addProductImage),
                Toast.LENGTH_SHORT
            ).show()
        }
        productQueries.set_nullcheck(barCode)
    }

    @SuppressLint("SetTextI18n")
    private fun updateProductInfoDialog(item: String, value: String, barCode: String, type: String) {
        val columnName = item.replace(", ", "")
        lateinit var newItem : String
        if (type == "add") {
            if (columnName == "ProductName") { newItem = getString(R.string.addProductName)}
            if (columnName == "ProductBrand") { newItem = getString(R.string.addProductBrand)}
        }
        else {
            if (columnName == "ProductName") { newItem = getString(R.string.updateProductName)}
            if (columnName == "ProductBrand") { newItem = getString(R.string.updateProductBrand)}
        }

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
        // add a button
        builder.setPositiveButton(
            "Update"
        ) { _, _ -> // do something with response
            val updatedValue = editText.text.toString()
            Log.e("Product Info new Update", updatedValue)
            if (columnName == "ProductName") {
                productQueries.update_product(updatedValue, newValue, barCode)
                binding.flipperMedia.prodInfo.tvProductName.text = "$updatedValue, "
            }
            if (columnName == "ProductBrand") {
                productQueries.update_brand(updatedValue, newValue, barCode)
                binding.flipperMedia.prodInfo.tvProductBrand.text = updatedValue
            }
        }
        // create and show the alert dialog
        val dialog: AlertDialog = builder.create()
        dialog.setOnDismissListener {
            productQueries.set_nullcheck(barCode)
            checkForNull(productQueries.getlocal(barCode).executeAsList()[0], barCode)
        }
        dialog.show()
    }

    override fun onPause() {
        super.onPause()
        cameraSource.stop()
    }

    override fun onResume() {
        Log.e("resumed?", "yes?")
        super.onResume()
        iniBc()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        val viewFlipper = binding.myViewFlipper
        if (viewFlipper.displayedChild == viewFlipper.indexOfChild(binding.flipperMedia.main2)){
            switchToScan()
        }
        else if (viewFlipper.displayedChild == viewFlipper.indexOfChild(binding.flipperMediaCamera.cameraImagePreviewLayout)){
            switchToScan()}
        else {super.onBackPressed()}
        //super.onBackPressed();
    }

    fun switchToPreview(viewFlipper: ViewFlipper) {
        binding.flipperMedia.AddItems.setText("1")
        activeScanBoolean = 1
        itemtobeAdded = 1
        viewFlipper.displayedChild = viewFlipper.indexOfChild(binding.flipperMedia.main2)
    }

    @SuppressLint("MissingPermission")
    fun switchToScan() {
        binding.myViewFlipper.displayedChild = binding.myViewFlipper.indexOfChild(binding.main)
        onResume()
        try {
            cameraSource.start(binding.SurfaceView.holder)
        }catch (e: IOException) {
            e.printStackTrace()
        }

    }

    operator fun get(index: Int) {}
    suspend fun downloadProduct(getbarcode: String) {
        try {
            val json = ktorclient.fetchProductByCode(getbarcode)
            val brand = json.product?.brands.toString()
            val product = json.product?.productName.toString()
            val status = json.status.toString()
            val productUrl = ktorclient.createProductUrl(getbarcode).toString()
//        val nutriments = json.product?.nutriments?.other
            val image = json.product?.imageFrontUrl.toString()

//        if (nutriments != null) {
//            Log.d("string of nutriments", nutriments.toString())
//            Log.d("string of nutriments count", nutriments.count().toString())
//        }
            var x = 2
            scans = dbinfoQueries.get_tokens().executeAsOne().toInt()
            if (status == "1" && scans == 0) {x = 0}
            else if (status == "1" && scans > 0) {x = 1}

            when (x) {
                0 -> Toast.makeText(
                    applicationContext,
                    "No more tokens remaining, please watch an add to receive more",
                    Toast.LENGTH_SHORT).show()
                1 -> {productQueries.insert_new(
                    getbarcode,
                    getbarcode,
                    brand,
                    brand,
                    product,
                    product,
                    status,
                    productUrl,
                    productUrl,
                    image,
                    image,
                    sdf.format(cal.time)
                    )
                    dbinfoQueries.update_tokens()
                    scans = dbinfoQueries.get_tokens().executeAsOne().toInt()
                    Toast.makeText(
                    applicationContext,
                        "$scans tokens remaining, ",
                    Toast.LENGTH_SHORT).show()}
            }
        } catch (_: NullPointerException) {
            Toast.makeText(
                applicationContext,
                "Failed to scan barcode. Please send number to developer.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    @SuppressLint("ResourceType")
    private fun openDatePicker() {
//        DatePickerDialog(this@BarcodeScan, R.style.CustomDatePickerDialogTheme, dateSetListener,
//            cal.get(Calendar.YEAR),
//            cal.get(Calendar.MONTH),
//            cal.get(Calendar.DAY_OF_MONTH)).show()
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        val customLayout: View = layoutInflater.inflate(R.layout.dialog_datepicker_custom, null)
        builder.setView(customLayout)
        val monthDate: DatePicker = customLayout.findViewById(R.id.CustomDatePicker)
        monthDate.init(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH), null)

        val freezer: ImageView = customLayout.findViewById(R.id.imageView2)
        val pantry: ImageView = customLayout.findViewById(R.id.imageView3)
        val fridge: ImageView = customLayout.findViewById(R.id.imageView4)

        val dialog: AlertDialog = builder.create()
        dialog.show()

        fridge.setOnClickListener {
            cal.set(Calendar.YEAR, monthDate.year)
            cal.set(Calendar.MONTH, monthDate.month)
            cal.set(Calendar.DAY_OF_MONTH, monthDate.dayOfMonth)
            spoildate = sdf.format(cal.time)
            addItemtoDB(spoildate, "Fridge")
            dialog.dismiss()
        }

        pantry.setOnClickListener {
            cal.set(Calendar.YEAR, monthDate.year)
            cal.set(Calendar.MONTH, monthDate.month)
            cal.set(Calendar.DAY_OF_MONTH, monthDate.dayOfMonth)
            spoildate = sdf.format(cal.time)
            addItemtoDB(spoildate, "Pantry")
            dialog.dismiss()
        }

        freezer.setOnClickListener {
            cal.set(Calendar.YEAR, monthDate.year)
            cal.set(Calendar.MONTH, monthDate.month)
            cal.set(Calendar.DAY_OF_MONTH, monthDate.dayOfMonth)
            spoildate = sdf.format(cal.time)
            addItemtoDB(spoildate, "Freezer")
            dialog.dismiss()
        }
    }

    private fun addItemtoDB(spoildate: String, location: String) {
        scandatetime = sdf.format(Calendar.getInstance().time).toString()
        itemQueries.insert(latestbarcodescan, spoildate, scandatetime, location)
        dbinfoQueries.update_tokens()
        scans = dbinfoQueries.get_tokens().executeAsOne().toInt()
        if (itemsRequired == 1) {
            Toast.makeText(applicationContext, "Item has been saved" +
                "\n$scans tokens remaining", Toast.LENGTH_SHORT).show()
        }
        else {
            Toast.makeText(applicationContext, "Item $itemtobeAdded of $itemsRequired has been saved" +
                "\n$scans tokens remaining", Toast.LENGTH_SHORT).show()
        }
        if (itemtobeAdded == itemsRequired){
            switchToScan()
        }
        else {
            itemtobeAdded += 1
            openDatePicker()
        }
    }

    private fun removeItemfromDB() {
        scandatetime = sdf.format(Calendar.getInstance().time).toString()
        itemQueries.removedfromstock(scandatetime, latestbarcodescan, latestbarcodescan)
        Toast.makeText(applicationContext, "Item has been removed", Toast.LENGTH_SHORT).show()
        switchToScan()
    }
}

fun loadImageFromWebOperations(src: String): Bitmap? {
    try {
        Log.e("src", src)
        val url = URL(src)
        val connection = url.openConnection() as HttpURLConnection
        connection.doInput = true
        connection.connect()
        val input = connection.inputStream
        val myBitmap = BitmapFactory.decodeStream(input)
        Log.e("Bitmap", "returned")
        return myBitmap
    } catch (e: IOException) {
        e.printStackTrace()
        Log.e("Exception", e.message!!)
        return null
    }
}