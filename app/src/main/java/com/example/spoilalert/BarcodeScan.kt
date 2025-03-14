package com.example.spoilalert

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.os.Bundle
import android.util.Log
import android.view.SurfaceHolder
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
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
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.example.Product_data
import com.example.spoilalert.adapters.clearProdPreview
import com.example.spoilalert.adapters.forceRefreshProdPreview
import com.example.spoilalert.adapters.populateProdPreview
import com.example.spoilalert.databinding.ActivityBarcodeScanBinding
import com.example.spoilalert.enginebuilder.OpenFoodFactsKtorClient
import com.example.spoilalert.utils.DownloadAndSaveImageTask
import com.example.spoilalert.utils.UpdateAndSaveImageTask
import com.example.spoilalert.utils.openNutriscoreDialog
import com.example.spoilalert.utils.rotateImageProxy
import com.google.android.gms.vision.CameraSource
import com.google.android.gms.vision.Detector
import com.google.android.gms.vision.barcode.Barcode
import com.google.android.gms.vision.barcode.BarcodeDetector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale


var latestbarcodescan = ""
var activeScanBoolean = 0
var itemsRequired = 1
var itemtobeAdded = 1
var test = 0

class BarcodeScan : AppCompatActivity() {
    private val ktorclient = OpenFoodFactsKtorClient()
    private val database = Database(AndroidSqliteDriver(Database.Schema, this, "launch.db"))
    private val itemQueries = database.itemQueries
    private val productQueries = database.productQueries
    private val dbinfoQueries = database.dBInfoQueries

    private var cameraLauncher: ActivityResultLauncher<String>? = null
    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var imageCapture: ImageCapture

    private val myFormat = "yyyyMMdd"
    private val sdf = SimpleDateFormat(myFormat, Locale.US)

    private lateinit var binding: ActivityBarcodeScanBinding
    private lateinit var viewFlipper: ViewFlipper
    private lateinit var barcodeDetector: BarcodeDetector
    private lateinit var cameraSource: CameraSource

    private var scandatetime = ""
    var spoildate = ""
    private var cal: Calendar = Calendar.getInstance()

    private var scans: Int = 0
    private var autoScan: Int = 1
    private var addingCustom: Int = 0
    var bitmap: Bitmap? = null


    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d("TAG", productQueries.selectAll().executeAsList().toString())
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        binding = ActivityBarcodeScanBinding.inflate(layoutInflater)
        viewFlipper = binding.myViewFlipper
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
            bitmap = null
            autoScan = 0
        }

        binding.flipperMedia.prodInfo.refreshProductButton.setOnClickListener{
            val builder = AlertDialog.Builder(this@BarcodeScan)
            builder.setMessage("Are you sure you want to Refresh all data?")
                .setCancelable(false)
                .setPositiveButton("Yes") { dialog, _ ->
                    val getBarcode = binding.flipperMedia.prodInfo.tvbarCode.text.toString()
                    clearProdPreview(binding.flipperMedia.prodInfo)
                    lifecycleScope.launch {
                        forceRefreshProdPreview(this@BarcodeScan, getBarcode, productQueries)
                    }
                    Thread.sleep(1000)
                    populateProdPreview(this@BarcodeScan, getBarcode, productQueries, binding.flipperMedia.prodInfo)
                }
                .setNegativeButton("No") { dialog, _ ->
                    dialog.dismiss()
                }
            val alert = builder.create()
            alert.show()
        }

        binding.flipperMedia.prodInfo.editImageButton.setOnClickListener{
            cameraLauncher?.launch(android.Manifest.permission.CAMERA)
        }

        binding.flipperMediaCamera.AddImageSaveButton.setOnClickListener{
            val fileName = productQueries.getRecordKey(binding.flipperMedia.prodInfo.tvbarCode.text.toString()).executeAsList()[0]
            imageCapture.takePicture(
                ContextCompat.getMainExecutor(this),
                object : ImageCapture.OnImageCapturedCallback() {
                    override fun onCaptureSuccess(image: ImageProxy) {
                        val rotatedImage = rotateImageProxy(image)
                        UpdateAndSaveImageTask(this@BarcodeScan, fileName, database, rotatedImage).saveImage()

                        binding.flipperMedia.prodInfo.imageView.setImageBitmap(rotatedImage)
                        binding.myViewFlipper.displayedChild = binding.myViewFlipper.indexOfChild(binding.flipperMedia.main2)
                        Toast.makeText(this@BarcodeScan, getString(R.string.updateImageSucceed), Toast.LENGTH_SHORT).show()
                        binding.flipperMedia.prodInfo.editImageButton.setBackgroundResource(R.drawable.circle_background)
                        productQueries.set_nullcheck(binding.flipperMedia.prodInfo.tvbarCode.text.toString())
                    }
                    override fun onError(exception: ImageCaptureException) {
                        Log.e("SpoilAlert", "Error capturing image: ${exception.message}")
                    }
                }
            )
        }

        binding.flipperMedia.prodInfo.NutriScore.setOnClickListener{
            openNutriscoreDialog(this, layoutInflater, binding.flipperMedia.prodInfo, productQueries)
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
                if (autoScan == 1) {getAutoScan(detections)}
                else if (autoScan == 0) {
                    getManualScan(detections)
                }

            }
        })
        clearProdPreview(binding.flipperMedia.prodInfo)
    }

    private fun getManualScan(detections: Detector.Detections<Barcode>) {
        autoScan = 1
        val barcodes = detections.detectedItems
        if (barcodes.size()!=0) {
            val getBarcode = barcodes.valueAt(0).displayValue
            Log.e("barcode detected", getBarcode)
            val customLayout: View = layoutInflater.inflate(R.layout.dialog_update_product_info, null)
            runOnUiThread(Runnable {
                addCustomBarcode(customLayout, getBarcode)
            })
        }
        else if (barcodes.size()==0) {
            var ba: ByteArray?
            cameraSource.takePicture(null) { data ->
                ba = data
                val exifInterface = ExifInterface(ByteArrayInputStream(ba))
                val orientation =
                    exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, 1)
                val matrix = Matrix()
                when (orientation) {
                    2 -> matrix.setScale(-1f, 1f)
                    3 -> matrix.setRotate(180f)
                    4 -> {
                        matrix.setRotate(180f)
                        matrix.postScale(-1f, 1f)
                    }

                    5 -> {
                        matrix.setRotate(90f)
                        matrix.postScale(-1f, 1f)
                    }

                    6 -> matrix.setRotate(90f)
                    7 -> {
                        matrix.setRotate(-90f)
                        matrix.postScale(-1f, 1f)
                    }

                    8 -> matrix.setRotate(-90f)
                }

                val tempBitmap = BitmapFactory.decodeByteArray(ba, 0, ba!!.size)
                bitmap = Bitmap.createBitmap(
                    tempBitmap,
                    0,
                    0,
                    tempBitmap.width,
                    tempBitmap.height,
                    matrix,
                    true
                )
            }
            // Get all current custom items in DB.
            var customlist = productQueries.get_all_custom_products().executeAsList()
            if (customlist.isEmpty()) {
                customlist = listOf("")
            }
            Log.e("barcode detected, custom list:", customlist.toString())
            val adapter: ArrayAdapter<String> = ArrayAdapter(this, android.R.layout.simple_list_item_1, customlist)
            val customLayout: View = layoutInflater.inflate(R.layout.dialog_add_custom_product, null)
            val autoCompleteTextView: AutoCompleteTextView = customLayout.findViewById(R.id.autoTextView)
            runOnUiThread(Runnable {
                autoCompleteTextView.setAdapter(adapter)
                addCustomProduct(customLayout, autoCompleteTextView)
            })

        }
    }

    private fun addCustomBarcode(customLayout: View, getBarcode: String) {
        // create an alert builder
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setTitle(getString(R.string.customBarcodeDetected))
        val editText: TextView = customLayout.findViewById<EditText>(R.id.editText)
        editText.text = getBarcode
        // set the custom layout
        builder.setView(customLayout)
        // add a button
        builder.setPositiveButton(
            "Confirm"
        ) { _, _ -> // do something with response
            sendCustomBarcodeToActivity(editText.text.toString())
        }
        // create and show the alert dialog
        val dialog: AlertDialog = builder.create()
        dialog.show()
    }

    private fun sendCustomBarcodeToActivity(getBarcode: String) {
        try {
            val localProduct =
                productQueries.getlocal(getBarcode).executeAsList()[0]
            productQueries.update_status(localProduct.RecordKey)
            populateProdPreview(this@BarcodeScan, getBarcode, productQueries, binding.flipperMedia.prodInfo)
            runOnUiThread(Runnable { switchToPreview(viewFlipper, getBarcode) })
        }
        catch (_: IndexOutOfBoundsException) {
            addingCustom = 1
            lifecycleScope.launch {
                downloadProduct(getBarcode, "addBarcode")}
            Thread.sleep(1000)
        }
    }

    private fun addCustomProduct(customLayout: View, autoCompleteTextView: AutoCompleteTextView) {
        // create an alert builder
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setTitle(getString(R.string.customProductDetected))
        // set the custom layout
        builder.setView(customLayout)
        // add a button
        builder.setPositiveButton(
            "Add Product"
        ) { _, _ -> // do something with response
            sendCustomProductToActivity(autoCompleteTextView.text.toString());
        }
        // create and show the alert dialog
        val dialog: AlertDialog = builder.create()
        dialog.show()
    }

    private fun sendCustomProductToActivity(addedItem: String) {
        Log.e("detected custom item", addedItem)
        lateinit var newProductRecordkey: String
        try {
            val getBarcode = productQueries.get_custom_product_barcode(addedItem).executeAsList()[0]
            populateProdPreview(this@BarcodeScan, getBarcode, productQueries, binding.flipperMedia.prodInfo)
            runOnUiThread(Runnable { switchToPreview(binding.myViewFlipper, getBarcode) })
        }
        catch (_: IndexOutOfBoundsException) {
            newProductRecordkey = try {
                "custom_${productQueries.get_max_recordkey().executeAsOne().toInt() + 1}"
            } catch (_: NullPointerException) {
                "999999999"
            }
            addingCustom = 1
            lifecycleScope.launch {
                downloadProduct(newProductRecordkey, addedItem)}
            Thread.sleep(1000)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun getAutoScan(detections: Detector.Detections<Barcode>) {
        if (activeScanBoolean == 0) {
            val barcodes = detections.detectedItems
            if(barcodes.size()!=0){
                val getBarcode = barcodes.valueAt(0).displayValue
                Log.e("previous barcode nr", latestbarcodescan)
                if(latestbarcodescan != getBarcode){
                    latestbarcodescan = getBarcode
                    getAutoScanHandler(getBarcode)
                }
                else {
                    latestbarcodescan = ""
                    Thread.sleep(1000)
                }
            }
        }
        else {
            Thread.sleep(1000)
        }
    }

    private fun getAutoScanHandler(getBarcode: String) {

        Log.e("barcode autoscanhandler", getBarcode)
        try {
            val localProduct =
                productQueries.getlocal(getBarcode).executeAsList()[0]
            if (localProduct.status == "1") {
                runOnUiThread {populateProdPreview(this@BarcodeScan, getBarcode, productQueries, binding.flipperMedia.prodInfo)}
                Log.e("gotopreview autoscnhandler", "pre")
                runOnUiThread { switchToPreview(viewFlipper, getBarcode) }
            }
        } catch (_: NullPointerException) {}
        catch (_: IndexOutOfBoundsException) {
            if (activeScanBoolean == 0) {
                activeScanBoolean = 1
                Log.e("Adding Data!!", getBarcode)
                lifecycleScope.launch {
                    downloadProduct(getBarcode, "")
                }
            }
        }
        return
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

            imageCapture = ImageCapture.Builder()
                .build()

            // Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, imageCapture, preview)

            } catch(exc: Exception) {
                Log.e("Camera", "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun checkForNull(localProduct: Product_data) {
        var isnull = true
        if (localProduct.product == "null" || localProduct.product == "" || localProduct.product == " ") {
            updateProductInfoDialog("ProductName", localProduct.product, localProduct.barCode, "add")
            isnull = false
        }
        else if (localProduct.brand == "null" || localProduct.brand == "" || localProduct.brand == " ") {
            updateProductInfoDialog("ProductBrand", localProduct.brand, localProduct.barCode, "add")
            isnull = false
        }
        else if (localProduct.image == "null" || localProduct.image == "" || localProduct.image == " ") {
            binding.flipperMedia.prodInfo.editImageButton.setBackgroundResource(R.drawable.circle_border_red)
            Toast.makeText(
                applicationContext,
                getString(R.string.addProductImage),
                Toast.LENGTH_SHORT
            ).show()
            isnull = false
        }
        if (localProduct.image != "null") {
            binding.flipperMedia.prodInfo.editImageButton.setBackgroundResource(R.drawable.circle_background)
        }
        if (addingCustom == 1) {
            dbinfoQueries.update_tokens()
            scans = dbinfoQueries.get_tokens().executeAsOne().toInt()
            Log.e("Token used", "Added_custom")
            Toast.makeText(
                applicationContext,"Product has been saved " +
                        "\n$scans tokens remaining, ",
                Toast.LENGTH_SHORT).show()
            addingCustom = 0
        }
        if (isnull) {
            productQueries.set_nullcheck(localProduct.barCode)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateProductInfoDialog(item: String, value: String, barCode: String, type: String) {
        // Please make sure to also update updateProductInfoDialog in Mainactivity
        lateinit var newItem : String
        var customlist = listOf("")
        if (item == "ProductName") {
            customlist = productQueries.get_all_products().executeAsList()
            newItem = if (type == "add") {
                getString(R.string.addProductName)
            } else {
                getString(R.string.addProductBrand)
            }
        }
        if (item == "ProductBrand") {
            customlist = productQueries.get_all_brands().executeAsList()
            newItem = if (type == "add") {
                getString(R.string.updateProductName)
            } else {
                getString(R.string.updateProductBrand)
            }
        }

        if (customlist.isEmpty()) {
            customlist = listOf("")
        }

        // create an alert builder
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setTitle(newItem)
        // set the custom layout
        val adapter: ArrayAdapter<String> = ArrayAdapter(this, android.R.layout.simple_list_item_1, customlist)
        val customLayout: View = layoutInflater.inflate(R.layout.dialog_update_product_info, null)
        val autoCompleteTextView: AutoCompleteTextView = customLayout.findViewById(R.id.editText)
        autoCompleteTextView.setAdapter(adapter)
        builder.setView(customLayout)
        // add a button
        builder.setPositiveButton(
            "Update"
        ) { _, _ -> // do something with response
            val updatedValue = autoCompleteTextView.text.toString()
            if (item == "ProductName") {
                productQueries.update_product(updatedValue, value, barCode)
                binding.flipperMedia.prodInfo.tvProductName.text = "$updatedValue, "
            }
            if (item == "ProductBrand") {
                productQueries.update_brand(updatedValue, value, barCode)
                binding.flipperMedia.prodInfo.tvProductBrand.text = updatedValue
            }
        }
        // create and show the alert dialog
        val dialog: AlertDialog = builder.create()
        dialog.setOnDismissListener {
            productQueries.set_nullcheck(barCode)
            checkForNull(productQueries.getlocal(barCode).executeAsList()[0])
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

    fun switchToPreview(viewFlipper: ViewFlipper, barCode: String) {
        binding.flipperMedia.AddItems.setText("1")
        activeScanBoolean = 1
        itemtobeAdded = 1
        viewFlipper.displayedChild = viewFlipper.indexOfChild(binding.flipperMedia.main2)
        checkForNull(productQueries.getlocal(barCode).executeAsList()[0])
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

    private suspend fun downloadProduct(getbarcode: String, customProductName: String) {
        var y = 0
        val json = ktorclient.fetchProductByCode(getbarcode)
        var brand = json.product?.brands.toString()
        var product = json.product?.productName.toString()
        var status = json.status.toString()
        val productUrl = ktorclient.createProductUrl(getbarcode).toString()
//        val nutriments = json.product?.nutriments?.other
        var image = json.product?.imageFrontUrl.toString()
        var nutriscore = json.product?.nutriscoreGrade.toString()
        if (customProductName != "") {
            if (customProductName != "addBarcode") {
                y = 1
                brand = "custom"
                product = customProductName
                status = "0"
                image = "null"
                nutriscore = "null"
                val file = UpdateAndSaveImageTask(this, getbarcode
                    .replace("custom_", "").toLong(), database, bitmap).saveImage()
                if (file != false) {
                    image = file.toString()
                }
                else {Log.i("SpoilAlert", "Failed to save image.")}
        }}
        if (customProductName == "addBarcode") {
            status = "1"
        }

//        if (nutriments != null) {
//            Log.d("string of nutriments", nutriments.toString())
//            Log.d("string of nutriments count", nutriments.count().toString())
//        }
        var x = 1
        scans = dbinfoQueries.get_tokens().executeAsOne().toInt()
        if (status == "1" && scans == 0) {x = 0}
        else if (status == "1" && scans > 0) {y = 2}

        Log.e("print x", x.toString())

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
                nutriscore,
                nutriscore,
                sdf.format(cal.time)
                )
                if (y == 1){
                    sendCustomProductToActivity(product)
                }
                else if (y == 2 && activeScanBoolean != 2){
                    dbinfoQueries.update_tokens()
                    Log.e("Token used", "Added_standard")
                    scans = dbinfoQueries.get_tokens().executeAsOne().toInt()
                    Toast.makeText(
                    applicationContext,"Product has been saved " +
                                "\n$scans tokens remaining, ",
                    Toast.LENGTH_SHORT).show()
                    }
                }
        }

        Log.e("customproductname", customProductName)

        // Download Image
        if (customProductName == "") {
            try{
                val localProduct =
                    productQueries.getlocal(getbarcode).executeAsList()[0]
                if (localProduct.status == "1") {
                    Log.e("status", localProduct.status)
                    val record = localProduct.RecordKey
                    val imgLoc = productQueries.getimg(record).executeAsList()[0]
                    bitmap = null
                    if (imgLoc != "null") {
                        Log.e("imgLoc", imgLoc)
                        val file = File(
                            File(this@BarcodeScan.filesDir, "Products"),
                            "$record.jpg"
                        )
                        Log.e("imgLoc_fiiiile_check?", file.toString())
                        DownloadAndSaveImageTask(
                            this@BarcodeScan,
                            record
                        ).execute(imgLoc)

                    }
                }
            } catch (e: IndexOutOfBoundsException) {}
        }
        Log.e("scanboolean?", activeScanBoolean.toString())
        if (activeScanBoolean == 2) {
            withContext(Dispatchers.IO) {
                Thread.sleep(1000)
            }
            getAutoScanHandler(getbarcode)
        }
        // reset to start scanning again after download / update completed
        activeScanBoolean = 0
    }

    @SuppressLint("ResourceType")
    private fun openDatePicker() {
        addingCustom = 0
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
        val getBarcode = binding.flipperMedia.prodInfo.tvbarCode.text.toString()

        val dialog: AlertDialog = builder.create()
        dialog.show()

        fridge.setOnClickListener {
            cal.set(Calendar.YEAR, monthDate.year)
            cal.set(Calendar.MONTH, monthDate.month)
            cal.set(Calendar.DAY_OF_MONTH, monthDate.dayOfMonth)
            spoildate = sdf.format(cal.time)
            addItemtoDB(spoildate, "Fridge", getBarcode)
            dialog.dismiss()
        }

        pantry.setOnClickListener {
            cal.set(Calendar.YEAR, monthDate.year)
            cal.set(Calendar.MONTH, monthDate.month)
            cal.set(Calendar.DAY_OF_MONTH, monthDate.dayOfMonth)
            spoildate = sdf.format(cal.time)
            addItemtoDB(spoildate, "Pantry", getBarcode)
            dialog.dismiss()
        }

        freezer.setOnClickListener {
            cal.set(Calendar.YEAR, monthDate.year)
            cal.set(Calendar.MONTH, monthDate.month)
            cal.set(Calendar.DAY_OF_MONTH, monthDate.dayOfMonth)
            spoildate = sdf.format(cal.time)
            addItemtoDB(spoildate, "Freezer", getBarcode)
            dialog.dismiss()
        }
    }

    private fun addItemtoDB(spoildate: String, location: String, getBarcode:String) {
        scandatetime = sdf.format(Calendar.getInstance().time).toString()
        itemQueries.insert(getBarcode, spoildate, scandatetime, location)
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
        val getBarcode = binding.flipperMedia.prodInfo.tvbarCode.text.toString()
        scandatetime = sdf.format(Calendar.getInstance().time).toString()
        itemQueries.removedfromstock(scandatetime, getBarcode, getBarcode)
        Toast.makeText(applicationContext, "Item has been removed", Toast.LENGTH_SHORT).show()
        switchToScan()
    }
}