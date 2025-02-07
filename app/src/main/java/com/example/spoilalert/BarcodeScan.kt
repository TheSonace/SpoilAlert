package com.example.spoilalert

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.SurfaceHolder
import android.widget.Toast
import android.widget.ViewFlipper
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.example.spoilalert.databinding.ActivityBarcodeScanBinding
import com.example.spoilalert.enginebuilder.OpenFoodFactsKtorClient
import com.google.android.gms.vision.CameraSource
import com.google.android.gms.vision.Detector
import com.google.android.gms.vision.barcode.Barcode
import com.google.android.gms.vision.barcode.BarcodeDetector
import kotlinx.coroutines.launch
import java.io.IOException
import java.lang.NullPointerException
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
    val itemQueries = database.itemQueries
    val productQueries = database.productQueries

    val myFormat = "yyyyMMdd"
    val sdf = SimpleDateFormat(myFormat, Locale.US)

    private lateinit var binding: ActivityBarcodeScanBinding
    private lateinit var barcodeDetector: BarcodeDetector
    private lateinit var cameraSource: CameraSource

    var scandatetime = ""
    var spoildate = ""
    var cal = Calendar.getInstance()

    private val dateSetListener = DatePickerDialog.OnDateSetListener { view, year, monthOfYear, dayOfMonth ->
        cal.set(Calendar.YEAR, year)
        cal.set(Calendar.MONTH, monthOfYear)
        cal.set(Calendar.DAY_OF_MONTH, dayOfMonth)
        spoildate = sdf.format(cal.time)
        addItemtoDB(spoildate)
    }

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d("TAG", productQueries.selectAll().executeAsList().toString())
        super.onCreate(savedInstanceState)
        binding = ActivityBarcodeScanBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.flipperMedia.btnAddItem.setOnClickListener {
            itemsRequired = Integer.parseInt(binding.flipperMedia.AddItems.text.toString())
            openDatePicker()
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
    }

    private fun iniBc(){
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
                Toast.makeText(applicationContext, "returning to main()", Toast.LENGTH_SHORT).show()
            }

        })
        barcodeDetector.setProcessor(object : Detector.Processor<Barcode>{
            override fun release() {
                Toast.makeText(applicationContext, "barcode scanner has been stopped", Toast.LENGTH_SHORT).show()
            }

            override fun receiveDetections(detections: Detector.Detections<Barcode>) {
                if (activeScanBoolean == 0) {
                    val barcodes = detections.detectedItems
                    if(barcodes.size()!=0){
                        val getbarcode = barcodes.valueAt(0).displayValue
                        if(latestbarcodescan != getbarcode){

                            Log.d("barcode read", getbarcode)
                            latestbarcodescan = getbarcode
                            val viewFlipper = binding.myViewFlipper
                            try {productQueries.localcheck(getbarcode).executeAsList()[0]}
                            catch (_: IndexOutOfBoundsException) {
                                lifecycleScope.launch {
                                    downloadProduct(getbarcode)}}
                            try {
                                val productpreviewlist =
                                    productQueries.getimg(latestbarcodescan).executeAsList()[0]
                                if (productpreviewlist != "null") {
                                    val img = loadImageFromWebOperations(productpreviewlist)
                                    latestbarcodescan = getbarcode
                                    binding.flipperMedia.imageView.setImageBitmap(img)
                                    runOnUiThread(Runnable { switchToPreview(viewFlipper) })}
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

    override fun onPause() {
        super.onPause()
        cameraSource.stop()
    }

    override fun onResume() {
        super.onResume()
        iniBc()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        val viewFlipper = binding.myViewFlipper
        if (viewFlipper.displayedChild == viewFlipper.indexOfChild(binding.flipperMedia.main2)){
            switchToScan()
            Log.d("TAG", "re-initiated iniBc?")
        }
        else {super.onBackPressed()}
        //super.onBackPressed();
    }

    fun switchToPreview(viewFlipper: ViewFlipper) {
        binding.flipperMedia.AddItems.setText("1")
        activeScanBoolean = 1
        itemtobeAdded = 1
        viewFlipper.displayedChild = viewFlipper.indexOfChild(binding.flipperMedia.main2)
    }

    fun switchToScan() {
        val viewFlipper = binding.myViewFlipper
        viewFlipper.displayedChild = viewFlipper.indexOfChild(binding.main)
        activeScanBoolean = 0
    }

    suspend fun downloadProduct(getbarcode: String) {
            val json = ktorclient.fetchProductByCode(getbarcode)
            val brand = json.product?.brands.toString()
            val product = json.product?.productName.toString()
            val productUrl = ktorclient.createProductUrl(getbarcode).toString()
            val image = json.product?.imageFrontUrl.toString()
            val status = json.status.toString()

            productQueries.insert(
                getbarcode,
                brand,
                product,
                status,
                productUrl,
                image,
                sdf.format(cal.time)
            )
    }

    private fun openDatePicker() {
        DatePickerDialog(this@BarcodeScan, R.style.CustomDatePickerDialogTheme, dateSetListener,
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun addItemtoDB(spoildate: String) {
        Log.d("loop_nr", itemtobeAdded.toString())
        Log.d("item_required", itemsRequired.toString())
        scandatetime = sdf.format(Calendar.getInstance().time).toString()
        Log.d("TAG", latestbarcodescan)
        Log.d("TAG", scandatetime)
        Log.d("TAG", spoildate)
        Log.d("TAG", "Added")
        itemQueries.insert(latestbarcodescan, spoildate, scandatetime)
        Toast.makeText(applicationContext, "Item " + itemtobeAdded + " of " + itemsRequired + " has been saved", Toast.LENGTH_SHORT).show()
        if (itemtobeAdded == itemsRequired){
            switchToScan()}
        else {
            itemtobeAdded += 1
            openDatePicker()
        }
    }

    private fun removeItemfromDB() {
        scandatetime = sdf.format(Calendar.getInstance().time).toString()
        Log.d("TAG", latestbarcodescan)
        Log.d("TAG", scandatetime)
        Log.d("TAG", "Removed")
        itemQueries.removedfromstock(scandatetime, latestbarcodescan, latestbarcodescan)
        Toast.makeText(applicationContext, "Item has been removed", Toast.LENGTH_SHORT).show()
        switchToScan()
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
}