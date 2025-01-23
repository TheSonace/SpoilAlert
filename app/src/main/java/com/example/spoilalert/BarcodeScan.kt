package com.example.spoilalert

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.SurfaceHolder
import android.widget.Toast
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
import java.io.InputStream
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class BarcodeScan : AppCompatActivity() {
    private val ktorclient = OpenFoodFactsKtorClient()
    private val database = Database(AndroidSqliteDriver(Database.Schema, this, "launch.db"))
    private val itemQueries = database.itemQueries
    private val productQueries = database.productQueries

    val myFormat = "dd.MM.yyyy"
    val sdf = SimpleDateFormat(myFormat, Locale.US)

    private val executorService: ExecutorService = Executors.newSingleThreadExecutor()
    private lateinit var binding: ActivityBarcodeScanBinding
    private lateinit var barcodeDetector: BarcodeDetector
    private lateinit var cameraSource: CameraSource

    var latestbarcodescan = ""
    var scandatetime = ""
    var spoildate = ""
    var cal = Calendar.getInstance()
    val dateSetListener = DatePickerDialog.OnDateSetListener { view, year, monthOfYear, dayOfMonth ->
        cal.set(Calendar.YEAR, year)
        cal.set(Calendar.MONTH, monthOfYear)
        cal.set(Calendar.DAY_OF_MONTH, dayOfMonth)
        spoildate = sdf.format(cal.time)
        addItemtoDB(spoildate)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBarcodeScanBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnAction.setOnClickListener {
            if (binding.addremoveswitch.isChecked) {
                DatePickerDialog(this@BarcodeScan, dateSetListener,
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH)).show()
            }
            else
                removeItemfromDB()
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
        binding.SurfaceView!!.holder.addCallback(object : SurfaceHolder.Callback{
            @SuppressLint("MissingPermission")
            override fun surfaceCreated(p0: SurfaceHolder) {
                try {
                    cameraSource.start(binding.SurfaceView!!.holder)
                }catch (e: IOException) {
                    e.printStackTrace()
                }
            }

            override fun surfaceChanged(
                holder: SurfaceHolder,
                format: Int,
                width: Int,
                height: Int
            ) {

            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                cameraSource.stop()
            }

        })
        barcodeDetector.setProcessor(object : Detector.Processor<Barcode>{
            override fun release() {
                Toast.makeText(applicationContext, "barcode scanner has been stopped", Toast.LENGTH_SHORT).show()
            }

            override fun receiveDetections(detections: Detector.Detections<Barcode>) {
                val barcodes = detections.detectedItems
                if(barcodes.size()!=0){
                    val getbarcode = barcodes.valueAt(0).displayValue
                    if(latestbarcodescan != getbarcode){
                        if(productQueries.localcheck(getbarcode).executeAsList().isEmpty()){
                            lifecycleScope.launch {
                                uploadNewProduct(getbarcode)
                            }
                        }
                        binding.btnAction.isClickable = true
                        binding.btnAction.text = "Scan Item"
                        latestbarcodescan = barcodes.valueAt(0).displayValue
                        binding.Preview.text = latestbarcodescan

                    }
                }
//                else {
//                    binding.btnAction.isClickable = false
//                    binding.btnAction.text = "No Barcode detected"
//                    binding.Preview.text = ""
//                    latestbarcodescan = ""
//
//                }
            }
        })
    }

    override fun onPause() {
        super.onPause()
        cameraSource!!.release()
    }

    override fun onResume() {
        super.onResume()
        iniBc()
    }

    suspend fun uploadNewProduct(getbarcode: String) {
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

        Log.d("TAG", productQueries.selectAll().executeAsList().toString())
    }

    private fun addItemtoDB(spoildate: String) {
        scandatetime = Calendar.getInstance().time.toString()
        Log.d("TAG", latestbarcodescan)
        Log.d("TAG", scandatetime)
        Log.d("TAG", spoildate)
        Log.d("TAG", "Added")
        Toast.makeText(applicationContext, "Item has been saved", Toast.LENGTH_SHORT).show()
    }

    private fun removeItemfromDB() {
        scandatetime = Calendar.getInstance().time.toString()
        Log.d("TAG", latestbarcodescan)
        Log.d("TAG", scandatetime)
        Log.d("TAG", "Removed")
        Toast.makeText(applicationContext, "Item has been removed", Toast.LENGTH_SHORT).show()
    }

    fun LoadImageFromWebOperations(url: String?): Drawable? {
        try {
            val `is` = URL(url).content as InputStream
            val d = Drawable.createFromStream(`is`, "src name")
            return d
        } catch (e: Exception) {
            return null
        }
    }
}